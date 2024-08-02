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
import com.squareup.test.support.forceEvaluate
import com.squareup.test.support.generateApplicationBuild
import com.squareup.tooling.support.core.models.SquareDependency
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VariantExtractorsTest {

  @TempDir
  lateinit var temporaryFolder: File

  @Test
  fun `Test BaseVariant_extractSquareVariantConfigurationParams() extension`() {
    val appProject = ProjectBuilder
      .builder()
      .withProjectDir(generateApplicationBuild(temporaryFolder))
      .build()

    appProject.forceEvaluate()

    val variant = appProject.extensions.getByType(AppExtension::class.java).applicationVariants.first()

    val (srcs, deps) = variant.extractSquareVariantConfigurationParams(appProject, emptyMap())

    // Main and debug sources are present
    assertTrue(
      srcs.all { it.startsWith("src/") },
      "All source paths must be relative to the project dir"
    )
    assertEquals(24, srcs.size, "Sources were missing")
    // Filter out the kotlin-stdlib-jdk8 dependency
    val filteredDeps = deps.filterNot {
      it.target.contains("kotlin-stdlib")
    }
    assertTrue("No dependencies should be listed") { filteredDeps.isEmpty() }
  }

  @Test
  fun `Test BaseVariant_extractSquareVariantConfigurationParams() extension with plugin`() {
    val appProject = ProjectBuilder
      .builder()
      .withProjectDir(generateApplicationBuild(temporaryFolder))
      .build()

    appProject.plugins.apply("com.squareup.sqldelight")

    // Make directories for sqldelight for testing.
    appProject.projectDir.resolve("src").resolve("main").resolve("sqldelight").mkdirs()

    appProject.forceEvaluate()

    val variant = appProject.extensions.getByType(AppExtension::class.java).applicationVariants.first()

    val (srcs, deps) = variant.extractSquareVariantConfigurationParams(appProject, emptyMap())

    assertTrue(
      srcs.all { it.startsWith("src/") },
      "All source paths must be relative to the project dir"
    )
    // Main and debug sources are present
    assertEquals(25, srcs.size, "Sources were missing")
    // Ensure sqldelight sources are included
    assertContains(srcs, "src/main/sqldelight")
    // Dependency added by SqlDelight
    assertContains(deps, SquareDependency("@maven://com.squareup.sqldelight:runtime-jvm"))
  }

  @Test
  fun `Test BaseVariant_extractSquareVariantConfigurationParams() extension with dependencies`() {
    val appProject = ProjectBuilder
      .builder()
      .withProjectDir(generateApplicationBuild(temporaryFolder))
      .build()

    appProject.forceEvaluate()

    appProject.dependencies.add("implementation", "com.squareup:foo")

    val variant = appProject.extensions.getByType(AppExtension::class.java).applicationVariants.first()

    val (srcs, deps) = variant.extractSquareVariantConfigurationParams(appProject, emptyMap())

    assertTrue(
      srcs.all { it.startsWith("src/") },
      "All source paths must be relative to the project dir"
    )
    // Main and debug sources are present
    assertEquals(24, srcs.size, "Sources were missing")
    // Dependency added by SqlDelight
    assertContains(deps, SquareDependency("@maven://com.squareup:foo"))
  }
}
