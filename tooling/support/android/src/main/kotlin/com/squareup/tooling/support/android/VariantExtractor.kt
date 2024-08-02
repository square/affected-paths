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

package com.squareup.tooling.support.android

import com.android.build.gradle.api.BaseVariant
import com.squareup.tooling.models.SquareDependency
import com.squareup.tooling.support.core.extractors.extractSquareDependencies
import org.gradle.api.Project
import java.io.File

// Extracts sources and dependencies from the given variant.
internal fun BaseVariant.extractSquareVariantConfigurationParams(
  project: Project,
  sourceIndex: Map<String, Sequence<File>>
): Pair<Set<String>, Set<SquareDependency>> {
  val srcs = linkedSetOf<String>()

  // Add all the sources for the given variant name
  srcs.addAll(sourceIndex[name].orEmpty().map { it.toRelativeString(project.projectDir) })
  srcs.addAll(
    sourceSets.asSequence().flatMap { sp ->
      sequenceOf(
        sp.assetsDirectories,
        sp.cDirectories,
        sp.cppDirectories,
        sp.javaDirectories,
        sp.jniLibsDirectories,
        sp.renderscriptDirectories,
        sp.resDirectories,
        sp.resourcesDirectories,
        sp.shadersDirectories,
        sp.kotlinDirectories,
        sp.aidlDirectories,
        sp.mlModelsDirectories
      ).flatten() + sp.manifestFile
    }.map { it.toRelativeString(project.projectDir) }
  )

  // SqlDelight doesn't add src/*/sqldelight to the source sets, but changes to those files can affect other projects
  if (project.plugins.hasPlugin("com.squareup.sqldelight")) {
    val aliasFile = project.projectDir
      .resolve("src")
      .resolve(name)
      .resolve("sqldelight")
    if (aliasFile.exists()) {
      srcs.add(aliasFile.toRelativeString(project.projectDir))
    }
    if (name != "main") {
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
    add(compileConfiguration.name)
    addAll(compileConfiguration.extendsFrom.map { it.name })
  }.toTypedArray()

  val deps = project.configurations.extractSquareDependencies(project, *configNames)

  return srcs to deps.toSet()
}
