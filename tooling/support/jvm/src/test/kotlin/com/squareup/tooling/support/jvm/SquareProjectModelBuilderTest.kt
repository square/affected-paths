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

import com.squareup.tooling.support.core.models.SquareProject
import com.squareup.tooling.support.core.models.SquareTestConfiguration
import com.squareup.tooling.support.core.models.SquareVariantConfiguration
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SquareProjectModelBuilderTest {

  @Test
  fun `Ensure java library constructs SquareProject`() {
    val projectExtractor = SquareProjectExtractorImpl()

    val rootProject = ProjectBuilder
      .builder()
      .withName("com.squareup")
      .build()

    val testProject = ProjectBuilder
      .builder()
      .withName("test")
      .withParent(rootProject)
      .build()

    val appProject = ProjectBuilder
      .builder()
      .withName("app")
      .withParent(testProject)
      .build()

    appProject.plugins.apply("java")

    val srcs = setOf("src/main/resources", "src/main/java")
    val testSrcs = setOf("src/test/resources", "src/test/java")

    val result = projectExtractor.extractSquareProject(appProject)
    val expected = SquareProject(
      name = "app",
      namespace = "com.squareup.test",
      pathToProject = "test/app",
      pluginUsed = "jvm",
      variants = mapOf(
        "debug" to SquareVariantConfiguration(
          srcs,
          emptySet(),
          mapOf(
            "debugUnitTest" to SquareTestConfiguration(
              testSrcs,
              emptySet()
            )
          )
        ),
        "release" to SquareVariantConfiguration(
          srcs,
          emptySet(),
          mapOf(
            "releaseUnitTest" to SquareTestConfiguration(
              testSrcs,
              emptySet()
            )
          )
        )
      )
    )
    assertEquals(expected, result)
  }

  @Test
  fun `Ensure no plugins returns null`() {
    val projectExtractor = SquareProjectExtractorImpl()

    val rootProject = ProjectBuilder
      .builder()
      .build()

    assertNull(projectExtractor.extractSquareProject(rootProject))
  }
}
