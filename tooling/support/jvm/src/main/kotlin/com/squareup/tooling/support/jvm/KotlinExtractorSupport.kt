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
import com.squareup.tooling.models.SquareTestConfiguration
import com.squareup.tooling.support.core.extractors.extractSquareDependencies
import com.squareup.tooling.support.core.models.SquareTestConfiguration
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer

// Extracts test configurations from kotlin source sets
internal fun KotlinSourceSet.extractSquareTestConfiguration(
  project: Project
): SquareTestConfiguration {
  return SquareTestConfiguration(
    srcs = kotlin.sourceDirectories.map { it.toRelativeString(project.projectDir) }.toSet(),
    deps = project.configurations.extractSquareDependencies(
      project,
      implementationMetadataConfigurationName,
      compileOnlyMetadataConfigurationName
    ).toSet()
  )
}

// Extracts sources and dependencies from the given kotlin source set.
internal fun KotlinSourceSet.extractSquareVariantConfigurationParams(
  project: Project,
  alias: String
): Pair<Set<String>, Set<SquareDependency>> {
  val srcs = linkedSetOf<String>()
  val deps = linkedSetOf<SquareDependency>()

  kotlin.sourceDirectories.mapTo(srcs) { it.toRelativeString(project.projectDir) }

  if (project.plugins.hasPlugin("com.squareup.sqldelight")) {
    val aliasFile = project.projectDir
      .resolve("src")
      .resolve(alias)
      .resolve("sqldelight")
    if (aliasFile.exists()) {
      srcs.add(aliasFile.path)
    }
    if (alias != "main") {
      val mainFile = project.projectDir
        .resolve("src")
        .resolve("main")
        .resolve("sqldelight")
      if (mainFile.exists()) {
        srcs.add(mainFile.path)
      }
    }
  }

  val configNames = buildList<String> {
    add(implementationMetadataConfigurationName)
    add(compileOnlyMetadataConfigurationName)
    addAll(
      project.configurations.getByName(implementationMetadataConfigurationName).extendsFrom
        .orEmpty().map { it.name }
    )
  }.toTypedArray()

  val result = project.configurations.extractSquareDependencies(project,*configNames).toList()
  deps.addAll(result)

  return srcs to deps
}

// Extracts all source sets for Kotlin projects
internal fun Project.extractKotlinSourceSets(
  tests: LinkedHashMap<String, MutableList<SquareTestConfiguration>>,
  variants: LinkedHashMap<String, Pair<Set<String>, Set<SquareDependency>>>,
  baseAndroidVariants: List<String>
) {
  // Capture sources from the "kotlin" source directory
  extensions.findByType(KotlinSourceSetContainer::class.java)
    ?.sourceSets
    ?.forEach { kss ->
      baseAndroidVariants.forEach { alias ->
        if (kss.name == SourceSet.TEST_SOURCE_SET_NAME) {
          val list = tests.getOrPut(alias) { arrayListOf() }
          list.add(kss.extractSquareTestConfiguration(this))
        } else {
          val oldResults = kss.extractSquareVariantConfigurationParams(this, alias)
          variants.compute(alias) { _, results ->
            return@compute if (results == null) {
              oldResults
            } else {
              (results.first + oldResults.first) to (results.second + oldResults.second)
            }
          }
        }
      }
    }
}
