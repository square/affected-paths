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
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestExtractorsTest {

  @Test
  fun `Test SourceSet_extractSquareTestConfiguration() extension`() {
    val appProject = ProjectBuilder
      .builder()
      .build()

    appProject.plugins.apply("java")

    appProject.forceEvaluate()

    val ssc = requireNotNull(appProject.extensions.findByType(SourceSetContainer::class.java)?.findByName("test"))

    val squareTestConfiguration = ssc.extractSquareTestConfiguration(appProject)

    assertTrue(squareTestConfiguration.srcs.containsAll(listOf("src/test/resources", "src/test/java")))
    assertEquals(0, squareTestConfiguration.deps.size)
  }

  @Test
  fun `Test KotlinSourceSet_extractSquareTestConfiguration() extension`() {
    val appProject = ProjectBuilder
      .builder()
      .build()

    appProject.plugins.apply("java")
    appProject.plugins.apply("kotlin")

    appProject.forceEvaluate()

    val kotlinSourceSet = requireNotNull(
      appProject.extensions.findByType(KotlinSourceSetContainer::class.java)?.sourceSets?.findByName("test")
    )

    val squareTestConfiguration = kotlinSourceSet.extractSquareTestConfiguration(appProject)

    assertTrue(squareTestConfiguration.srcs.containsAll(listOf("src/test/kotlin", "src/test/java")))
    assertEquals(0, squareTestConfiguration.deps.size)
  }
}
