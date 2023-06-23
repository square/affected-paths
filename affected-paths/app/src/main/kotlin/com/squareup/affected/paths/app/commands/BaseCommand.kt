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

package com.squareup.affected.paths.app.commands

import com.squareup.affected.paths.app.options.BaseConfigurationOptions
import com.squareup.affected.paths.app.utils.VersionProvider
import com.squareup.affected.paths.app.utils.toCoreOptions
import com.squareup.affected.paths.core.CoreAnalyzer
import com.squareup.affected.paths.core.utils.AffectedPathsMessage
import com.squareup.affected.paths.core.utils.AffectedPathsMessage.Companion.AFFECTED_PATHS_ERROR_CODE
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import picocli.CommandLine.Command
import picocli.CommandLine.Mixin
import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.ScopeType.INHERIT
import picocli.CommandLine.Spec
import java.util.concurrent.Callable

private val LOGGER = LoggerFactory.getLogger(BaseCommand::class.java)

@Command(
  name = "base",
  mixinStandardHelpOptions = true,
  versionProvider = VersionProvider::class,
  description = ["Performs the necessary avoidance task"],
  scope = INHERIT
)
internal class BaseCommand : Callable<Int> {

  @Spec
  private lateinit var spec: CommandSpec

  @Mixin
  private val options = BaseConfigurationOptions()

  private val coreAnalyzer by lazy { CoreAnalyzer(options.toCoreOptions()) }

  override fun call(): Int {
    try {
      runBlocking {
        val analysisResult = coreAnalyzer.analyze()
        val fileToProjectsMap = sortedMapOf<String, MutableSet<String>>()

        analysisResult.affectedResults.forEach {
          val set = fileToProjectsMap.getOrPut(it.file) { sortedSetOf() }
          set.addAll(it.affectedProjectPaths)
        }

        if (fileToProjectsMap.isEmpty()) {
          println("No projects affected")
        } else {
          println("Affected projects found.")
          println()
          fileToProjectsMap.forEach { (file, affectedProjects) ->
            println("Changed file: $file")
            println("Projects affected by this changed file:")
            affectedProjects.forEach {
              println("    - :${it.replace('/', ':')}")
            }
            println()
          }
        }
      }
      return 0
    } catch (e: Throwable) {
      if (e is AffectedPathsMessage) {
        LOGGER.error(e.message)
        return AFFECTED_PATHS_ERROR_CODE
      } else {
        LOGGER.error("Unable to complete analysis", e)
      }
      return spec.exitCodeOnExecutionException()
    }
  }
}
