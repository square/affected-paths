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

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension
import com.squareup.tooling.models.SquareProject
import com.squareup.tooling.models.SquareTestConfiguration
import com.squareup.tooling.support.core.extractors.relativePathToRootBuild
import com.squareup.tooling.support.core.extractors.relativePathToRootProject
import com.squareup.tooling.support.core.models.SquareProject
import com.squareup.tooling.support.core.models.SquareVariantConfiguration
import org.gradle.api.Project

/**
 * Extracts a [SquareProject] using the Android [AppExtension].
 */
// Using a separate function due to minor differences between the app and library extensions
// that can't be reconciled with a generic function
internal fun Project.extractAppModuleProject(): SquareProject {
  val appExtension = requireNotNull(extensions.findByType(AppExtension::class.java))
  // Gets the sources defined in the extension
  val sourceIndex = appExtension.sourceIndexExtractor()

  return SquareProject(
    name = name,
    pluginUsed = "android-app",
    namespace = rootProject.name,
    pathToProject = relativePathToRootBuild() ?: relativePathToRootProject(),
    // Variants and configurations are different things. Should really be split.
    variants = appExtension.applicationVariants.associate { variant ->
      val (srcs, deps) = variant.extractSquareVariantConfigurationParams(this, sourceIndex)
      val tests = buildMap<String, SquareTestConfiguration> {
        variant.testVariant?.let {
          put(it.name, it.extractSquareTestConfiguration(this@extractAppModuleProject))
        }
        variant.unitTestVariant?.let {
          put(it.name, it.extractSquareTestConfiguration(this@extractAppModuleProject))
        }
      }
      variant.name to SquareVariantConfiguration(
        srcs = srcs,
        deps = deps,
        tests = tests
      )
    }
  )
}

/**
 * Extracts a [SquareProject] using the Android [LibraryExtension].
 */
internal fun Project.extractLibraryModuleProject(): SquareProject {
  val libraryExtension = requireNotNull(extensions.findByType(LibraryExtension::class.java))
  // Gets the sources defined in the extension
  val sourceIndex = libraryExtension.sourceIndexExtractor()

  return SquareProject(
    name = name,
    pluginUsed = "android-library",
    namespace = rootProject.name,
    pathToProject = relativePathToRootBuild() ?: relativePathToRootProject(),
    variants = libraryExtension.libraryVariants.associate { variant ->
      val (srcs, deps) = variant.extractSquareVariantConfigurationParams(this, sourceIndex)
      val tests = buildMap<String, SquareTestConfiguration> {
        variant.testVariant?.let {
          put(it.name, it.extractSquareTestConfiguration(this@extractLibraryModuleProject))
        }
        variant.unitTestVariant?.let {
          put(it.name, it.extractSquareTestConfiguration(this@extractLibraryModuleProject))
        }
      }
      variant.name to SquareVariantConfiguration(
        srcs = srcs,
        deps = deps,
        tests = tests
      )
    }
  )
}

/**
 * Extracts a [SquareProject] using the Android [TestExtension].
 */
internal fun Project.extractTestModuleProject(): SquareProject {
  val testExtension = requireNotNull(extensions.findByType(TestExtension::class.java))

  // Gets the sources defined in the extension
  val sourceIndex = testExtension.sourceIndexExtractor()

  return SquareProject(
    name = name,
    pluginUsed = "android-test",
    namespace = rootProject.name,
    pathToProject = relativePathToRootBuild() ?: relativePathToRootProject(),
    variants = testExtension.applicationVariants.associate { variant ->
      val (srcs, deps) = variant.extractSquareVariantConfigurationParams(this, sourceIndex)
      val tests = buildMap<String, SquareTestConfiguration> {
        variant.testVariant?.let {
          put(it.name, it.extractSquareTestConfiguration(this@extractTestModuleProject))
        }
        variant.unitTestVariant?.let {
          put(it.name, it.extractSquareTestConfiguration(this@extractTestModuleProject))
        }
      }
      variant.name to SquareVariantConfiguration(
        srcs = srcs,
        deps = deps,
        tests = tests
      )
    }
  )
}
