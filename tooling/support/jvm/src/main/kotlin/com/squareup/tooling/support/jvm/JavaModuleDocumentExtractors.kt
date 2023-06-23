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
import com.squareup.tooling.models.SquareProject
import com.squareup.tooling.models.SquareTestConfiguration
import com.squareup.tooling.support.core.models.SquareProject
import com.squareup.tooling.support.core.models.SquareVariantConfiguration
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

// List of variants to be used by Java only projects
private val baseAndroidVariants = listOf("debug", "release")

/**
 * Extracts a [SquareProject] from the [org.gradle.api.plugins.JavaBasePlugin]
 */
internal fun Project.extractJavaModuleProject(): SquareProject {
  val ssc = extensions.findByType(SourceSetContainer::class.java).orEmpty()
  val tests = linkedMapOf<String, MutableList<SquareTestConfiguration>>()
  val variants = linkedMapOf<String, Pair<Set<String>, Set<SquareDependency>>>()

  // Due to the current logic expecting variants, we fake it here for the time being
  // This only checks the "java" source directory
  ssc.forEach { ss ->
    baseAndroidVariants.forEach { alias ->
      if (ss.name == SourceSet.TEST_SOURCE_SET_NAME) {
        val list = tests.getOrPut(alias) { arrayListOf() }
        list.add(ss.extractSquareTestConfiguration(this))
      } else {
        val result = ss.extractSquareVariantConfigurationParams(this, alias)
        variants[alias] = result
      }
    }
  }

  if (project.plugins.hasPlugin("kotlin")) {
    project.extractKotlinSourceSets(tests, variants, baseAndroidVariants)
  }

  return SquareProject(
    name = name,
    pluginUsed = "jvm",
    namespace = group.toString(),
    pathToProject = projectDir.toRelativeString(rootDir),
    variants = variants.mapValues { (key, pair) ->
      val (srcs, deps) = pair
      SquareVariantConfiguration(
        srcs = srcs,
        deps = deps,
        tests = tests[key].orEmpty().associateBy { "${key}UnitTest" }
      )
    }
  )
}
