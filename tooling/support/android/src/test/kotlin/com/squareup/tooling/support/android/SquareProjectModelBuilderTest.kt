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

import com.squareup.tooling.support.core.models.SquareProject
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SquareProjectModelBuilderTest {

  @Test
  fun `Ensure android app constructs SquareProject`() {
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

    appProject.plugins.apply("com.android.application")

    val result = projectExtractor.extractSquareProject(appProject)
    val expected = SquareProject(
      name = "app",
      namespace = "com.squareup.test",
      pathToProject = "test/app",
      pluginUsed = "android-app",
      variants = emptyMap()
    )
    assertEquals(expected, result)
  }

  @Test
  fun `Ensure android library constructs SquareProject`() {
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

    val libraryProject = ProjectBuilder
      .builder()
      .withName("lib")
      .withParent(testProject)
      .build()

    libraryProject.plugins.apply("com.android.library")

    val result = projectExtractor.extractSquareProject(libraryProject)
    val expected = SquareProject(
      name = "lib",
      namespace = "com.squareup.test",
      pathToProject = "test/lib",
      pluginUsed = "android-library",
      variants = emptyMap()
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
