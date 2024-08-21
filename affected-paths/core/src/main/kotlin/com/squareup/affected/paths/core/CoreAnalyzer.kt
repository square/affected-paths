/*
 * Copyright (c) 2023 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.squareup.affected.paths.core

import com.squareup.affected.paths.core.di.configurationsModule
import com.squareup.affected.paths.core.di.gitModule
import com.squareup.affected.paths.core.di.gradleToolingModule
import com.squareup.affected.paths.core.git.SquareGit
import com.squareup.affected.paths.core.models.AnalysisResult
import com.squareup.affected.paths.core.utils.SquareBuildAction
import com.squareup.affected.paths.core.utils.findAffectedPaths
import com.squareup.tooling.models.SquareProject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import org.gradle.tooling.GradleConnector
import org.koin.core.KoinApplication
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import java.nio.file.Path

/**
 * This performs an analysis with the given [SquareProject] to determine affected paths
 */
public class CoreAnalyzer @JvmOverloads constructor(private val coreOptions: CoreOptions = CoreOptions()) {

  private val affectedPathsApplication by lazy {
    val app = GlobalContext.getKoinApplicationOrNull() ?: startKoin(KoinApplication.init())
    return@lazy app.modules(
      configurationsModule(coreOptions),
      gitModule(),
      gradleToolingModule()
    )
  }

  /**
   * Performs analysis on the [SquareProject] list provided, or on the list gathered via Gradle Tooling API
   */
  public suspend fun analyze(projects: List<SquareProject>? = null): AnalysisResult {
    // Cancellation token to cancel the Gradle configuration immediately
    val cancellationTokenSource = GradleConnector.newCancellationTokenSource()

    // No need for a new coroutine scope. Block the current thread until all coroutines are completed.
    return try {
      coroutineScope {
        val changedFilesDeferred = async {
          ensureActive() // In case this is cancelled before start
          return@async coreOptions.changedFiles.ifEmpty {
            val git = affectedPathsApplication.koin.get<SquareGit>()
            return@ifEmpty git.findChangedFiles()
          }
        }

        val rootDir = affectedPathsApplication.koin.get<Path>()
        /*
         * Since the Gradle Tooling API is run from a different process, run all TAPIs on the IO
         * dispatcher.
         */
        val projectsDeferred = projects?.let { CompletableDeferred(it) } ?: async(Dispatchers.IO) {
          ensureActive() // In case this is cancelled before start

          val projectConnection = with(affectedPathsApplication.koin.get<GradleConnector>()) {
            forProjectDirectory(rootDir.toFile())
            when {
              coreOptions.gradleDistributionPath != null -> {
                useDistribution(coreOptions.gradleDistributionPath.toUri())
              }
              coreOptions.gradleInstallationPath != null -> {
                useInstallation(coreOptions.gradleInstallationPath.toFile())
              }
              coreOptions.gradleVersion != null -> {
                useGradleVersion(coreOptions.gradleVersion)
              }
              else -> useBuildDistribution()
            }

            if (coreOptions.gradleUserHome != null) {
              useGradleUserHomeDir(coreOptions.gradleUserHome.toFile())
            }
            return@with connect()
          }

          ensureActive()

          val actionExecutor = projectConnection.action(
            SquareBuildAction(coreOptions.allowGradleParallel)
          )
          actionExecutor.withCancellationToken(cancellationTokenSource.token())
          if (coreOptions.useBuildScan) {
            actionExecutor.forTasks(emptyList())
          }
          actionExecutor.addArguments(coreOptions.gradleArgs)
          actionExecutor.addJvmArguments(coreOptions.jvmArgs)

          // Output Gradle logs to console
          if (coreOptions.logGradle) {
            actionExecutor
              .setStandardOutput(System.out)
              .setStandardInput(System.`in`)
              .setStandardError(System.err)
          }

          return@async actionExecutor.run()
        }

        val affectedResultsDeferred = async(Dispatchers.Default) {
          findAffectedPaths(projectsDeferred.await(), changedFilesDeferred.await())
        }

        // Cancel the Gradle build if the coroutine was cancelled
        projectsDeferred.invokeOnCompletion {
          if (it is CancellationException) {
            cancellationTokenSource.cancel()
          }
        }

        val projectMapDeferred = async(Dispatchers.Default) {
          projectsDeferred.await().associateBy { it.pathToProject }
        }

        return@coroutineScope AnalysisResult(
          changedFilesDeferred.await(),
          projectMapDeferred.await(),
          affectedResultsDeferred.await()
        )
      }
    } catch (e: Throwable) {
      // Make sure the Gradle configuration is cancelled for errors!
      cancellationTokenSource.cancel()
      throw e
    }
  }
}
