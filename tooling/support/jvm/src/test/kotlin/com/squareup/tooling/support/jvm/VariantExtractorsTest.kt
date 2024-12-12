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

import com.squareup.test.support.forceEvaluate
import com.squareup.tooling.support.core.models.SquareDependency
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class VariantExtractorsTest {

  @Test
  fun `Test SourceSet_extractSquareVariantConfigurationParams() extension`() {
    val appProject = ProjectBuilder
      .builder()
      .build()

    appProject.plugins.apply("java")

    appProject.forceEvaluate()

    val ssc = requireNotNull(appProject.extensions.findByType(SourceSetContainer::class.java)?.findByName("main"))

    val (srcs, deps) = ssc.extractSquareVariantConfigurationParams(appProject, "main")
    assertTrue(srcs.containsAll(listOf("src/main/resources", "src/main/java")))
    assertTrue("No dependencies should be listed") { deps.isEmpty() }
  }

  @Test
  fun `Test SourceSet_extractSquareVariantConfigurationParams() extension with dependencies`() {
    val appProject = ProjectBuilder
      .builder()
      .build()

    appProject.plugins.apply("java")

    appProject.forceEvaluate()

    appProject.dependencies.add("implementation", "com.squareup:foo")

    val ssc = requireNotNull(appProject.extensions.findByType(SourceSetContainer::class.java)?.findByName("main"))

    val (srcs, deps) = ssc.extractSquareVariantConfigurationParams(appProject, "main")
    assertTrue(srcs.containsAll(listOf("src/main/resources", "src/main/java")))
    assertContains(deps, SquareDependency("@maven://com.squareup:foo"))
  }

  @Test
  fun `Test SourceSet_extractSquareVariantConfigurationParams() extension with plugins`() {
    val appProject = ProjectBuilder
      .builder()
      .build()

    appProject.plugins.apply("java")
    appProject.plugins.apply("kotlin") // Needed for SQLDelight
    appProject.plugins.apply("com.squareup.sqldelight")

    // Make directories for sqldelight for testing.
    appProject.projectDir.resolve("src").resolve("main").resolve("sqldelight").mkdirs()

    appProject.forceEvaluate()

    val ssc = requireNotNull(appProject.extensions.findByType(SourceSetContainer::class.java)?.findByName("main"))

    val (srcs, deps) = ssc.extractSquareVariantConfigurationParams(appProject, "main")
    assertTrue(
      srcs.containsAll(listOf("src/main/resources", "src/main/java", "src/main/kotlin", "src/main/sqldelight"))
    )
    // Dependency added by SqlDelight
    assertContains(deps, SquareDependency("@maven://com.squareup.sqldelight:runtime-jvm"))
  }

  @Test
  fun `Test KotlinSourceSet_extractSquareVariantConfigurationParams() extension`() {
    val appProject = ProjectBuilder
      .builder()
      .build()

    appProject.plugins.apply("java")
    appProject.plugins.apply("kotlin")

    appProject.forceEvaluate()

    val kotlinSourceSet = requireNotNull(
      appProject.extensions.findByType(KotlinSourceSetContainer::class.java)?.sourceSets?.findByName("main")
    )

    val (srcs, deps) = kotlinSourceSet.extractSquareVariantConfigurationParams(appProject, "main")
    assertTrue(srcs.containsAll(listOf("src/main/java", "src/main/kotlin")))
    // Filter out the kotlin-stdlib-jdk8 dependency
    val filteredDeps = deps.filterNot {
      it.target.contains("kotlin-stdlib")
    }
    assertTrue("No dependencies should be listed") { filteredDeps.isEmpty() }
  }

  @Test
  fun `Test KotlinSourceSet_extractSquareVariantConfigurationParams() with dependencies`() {
    val appProject = ProjectBuilder
      .builder()
      .build()

    appProject.plugins.apply("java")
    appProject.plugins.apply("kotlin")

    appProject.forceEvaluate()

    appProject.dependencies.add("implementation", "com.squareup:foo")

    val kotlinSourceSet = requireNotNull(
      appProject.extensions.findByType(KotlinSourceSetContainer::class.java)?.sourceSets?.findByName("main")
    )

    val (srcs, deps) = kotlinSourceSet.extractSquareVariantConfigurationParams(appProject, "main")
    assertTrue(srcs.containsAll(listOf("src/main/java", "src/main/kotlin")))
    assertContains(deps, SquareDependency("@maven://com.squareup:foo"))
  }
}
