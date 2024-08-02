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

package com.squareup.tooling.support.jvm

import com.squareup.tooling.models.SquareDependency
import com.squareup.tooling.support.core.extractors.extractSquareDependencies
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet

// Extracts sources and dependencies from the given source set. Used by java projects.
internal fun SourceSet.extractSquareVariantConfigurationParams(
  project: Project,
  alias: String
): Pair<Set<String>, Set<SquareDependency>> {
  val srcs = linkedSetOf<String>()
  val deps = linkedSetOf<SquareDependency>()

  allSource.sourceDirectories.mapTo(srcs) { it.toRelativeString(project.projectDir) }

  // SqlDelight doesn't add src/*/sqldelight to the source sets, but changes to those files can affect other projects
  if (project.plugins.hasPlugin("com.squareup.sqldelight")) {
    // Handles differing builds (release/debug)
    val aliasFile = project.projectDir
      .resolve("src")
      .resolve(alias)
      .resolve("sqldelight")
    if (aliasFile.exists()) {
      srcs.add(aliasFile.toRelativeString(project.projectDir))
    }
    // Ensures that main is added
    if (alias != "main") {
      val mainFile = project.projectDir
        .resolve("src")
        .resolve("main")
        .resolve("sqldelight")
      if (mainFile.exists()) {
        srcs.add(mainFile.toRelativeString(project.projectDir))
      }
    }
  }

  val configNames = buildList<String> {
    add(compileClasspathConfigurationName)
    addAll(
      project.configurations.getByName(compileClasspathConfigurationName).extendsFrom
        .orEmpty().map { it.name }
    )
    addAll(
      project.configurations.getByName(runtimeClasspathConfigurationName).extendsFrom
        .orEmpty().map { it.name }
    )
  }.toTypedArray()

  deps.addAll(project.configurations.extractSquareDependencies(project, *configNames))

  return srcs to deps
}
