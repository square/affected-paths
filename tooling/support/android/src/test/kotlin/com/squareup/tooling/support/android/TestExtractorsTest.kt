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
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestExtractorsTest {

  @TempDir
  lateinit var temporaryFolder: File

  @Test
  fun `Test variant extractSquareTestConfiguration() extension`() {
    val rootProject = ProjectBuilder
      .builder()
      .withProjectDir(temporaryFolder)
      .build()

    val appProject = ProjectBuilder
      .builder()
      .withParent(rootProject)
      .withProjectDir(generateApplicationBuild(File(rootProject.projectDir, "app")))
      .withName("app")
      .build()

    appProject.forceEvaluate()

    // Test the UnitTestVariant
    val unitTestVariant = appProject.extensions.getByType(AppExtension::class.java).unitTestVariants.first()

    val squareUnitTestConfiguration = unitTestVariant
      .extractSquareTestConfiguration(appProject)

    assertEquals(1, squareUnitTestConfiguration.deps.size)
    assertTrue { squareUnitTestConfiguration.deps.all { it.target == "/app" } }

    // Test the TestVariant
    val testVariant = appProject.extensions.getByType(AppExtension::class.java).testVariants.first()

    val squareTestConfiguration = testVariant.extractSquareTestConfiguration(appProject)

    assertTrue(
      squareTestConfiguration.srcs.all { it.startsWith("src/") },
      "All source paths must be relative to the project dir"
    )
    assertEquals(1, squareTestConfiguration.deps.size)
    assertTrue { squareTestConfiguration.deps.all { it.target == "/app" } }
  }
}
