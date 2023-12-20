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

@file:Suppress("UnstableApiUsage")

package com.squareup.affected.paths.core.utils

import com.squareup.tooling.models.SquareProject
import com.squareup.tooling.models.SquareProjectParameters
import org.gradle.tooling.BuildAction
import org.gradle.tooling.BuildController
import org.gradle.tooling.model.Model

// Build action to grab the SquareProject on a per-project basis
private class ProjectBuildAction(
  private val project: Model,
  private val rootDir: String
) : BuildAction<SquareProject?> {
  override fun execute(controller: BuildController): SquareProject? {
    return controller.findModel(project, SquareProject::class.java, SquareProjectParameters::class.java) {
      it.gitRoot = rootDir
    }
  }
}

/**
 * Base build action to gather all [SquareProject] from the current build.
 */
internal class SquareBuildAction(
  private val allowParallelConfiguration: Boolean,
  private val useIncludeBuild: Boolean,
  private val rootDir: String
) : BuildAction<List<SquareProject>> {
  override fun execute(controller: BuildController): List<SquareProject> {
    // Run the ProjectBuildAction in parallel, if we can
    val canRunParallel = controller.getCanQueryProjectModelInParallel(SquareProject::class.java)

    val actions = buildList {
      // Include any builds along with the root build
      if (useIncludeBuild) {
        controller.buildModel.includedBuilds.forEach { build ->
          addAll(
            build.projects // All projects included in the "settings.gradle" file of all builds
              .asSequence()
              .map { project ->
                return@map ProjectBuildAction(project, rootDir)
              }
          )
        }
      }

      // The "BuildModel" is the Gradle build after evaluating the "settings.gradle" file
      addAll(
        controller.buildModel
          .projects // All projects included in the "settings.gradle" file
          .asSequence()
          .map { project ->
            return@map ProjectBuildAction(project, rootDir)
          }.toList()
      )
    }

    if (actions.isEmpty()) return emptyList()
    return if (allowParallelConfiguration && canRunParallel) {
      controller.run(actions).filterNotNull()
    } else {
      actions.mapNotNull { it.execute(controller) }
    }
  }
}
