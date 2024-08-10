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

package com.squareup.tooling.support.builder

import com.squareup.test.support.forceEvaluate
import com.squareup.test.support.generateApplicationBuild
import com.squareup.test.support.generateLibraryBuild
import com.squareup.test.support.generateTestBuild
import com.squareup.tooling.models.SquareProject
import com.squareup.tooling.support.core.models.SquareDependency
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val ANDROID_SRC_DIRECTORY_PATHS =
  listOf(
    "aidl",
    "assets",
    "java",
    "jni",
    "jniLibs",
    "res",
    "resources",
    "rs",
    "kotlin",
    "ml",
    "shaders",
    "AndroidManifest.xml"
  )

private val ANDROID_TEST_SRC_DIRECTORY_PATHS =
  listOf("assets", "jni", "java", "jniLibs", "rs", "res", "resources", "shaders", "AndroidManifest.xml")

class SquareProjectModelBuilderTest {

  @TempDir
  lateinit var temporaryFolder: File

  @Test
  fun `Ensure canBuild() handles correct models`() {
    val projectModelBuilder = SquareProjectModelBuilder()

    assertTrue("SquareProject should be buildable") {
      projectModelBuilder.canBuild(SquareProject::class.java.name)
    }
    assertFalse("Non-SquareProject models should not be buildable") {
      projectModelBuilder.canBuild("test")
    }
  }

  @Test
  fun `Ensure android app constructs SquareProject`() {
    val projectModelBuilder = SquareProjectModelBuilder()

    val rootProject = ProjectBuilder
      .builder()
      .withProjectDir(temporaryFolder)
      .withName("com.squareup.test")
      .build()

    val appProject = ProjectBuilder
      .builder()
      .withName("app")
      .withProjectDir(generateApplicationBuild(File(rootProject.projectDir, "app")))
      .withParent(rootProject)
      .build()

    val expectedTestDependency = SquareDependency("/app", setOf("transitive"))

    appProject.forceEvaluate()

    val result = projectModelBuilder.buildAll(SquareProject::class.java.name, appProject) as SquareProject

    // Check SquareProject properties
    assertEquals("app", result.name)
    assertEquals("com.squareup.test", result.namespace)
    assertEquals("app", result.pathToProject)
    assertEquals("android-app", result.pluginUsed)

    // Check variant properties
    assertTrue(result.variants.keys.containsAll(listOf("debug", "release")))
    val debugVariant = requireNotNull(result.variants["debug"])
    assertTrue {
      debugVariant.srcs.containsAll(
        ANDROID_SRC_DIRECTORY_PATHS.map { "src/debug/$it" } +
            ANDROID_SRC_DIRECTORY_PATHS.map { "src/main/$it" }
      )
    }
    assertTrue(debugVariant.deps.filterNot { it.target.contains("kotlin-stdlib") }.isEmpty())

    val releaseVariant = requireNotNull(result.variants["release"])
    assertTrue {
      releaseVariant.srcs.containsAll(
        ANDROID_SRC_DIRECTORY_PATHS.map { "src/release/$it" } +
            ANDROID_SRC_DIRECTORY_PATHS.map { "src/main/$it" }
      )
    }
    assertTrue(releaseVariant.deps.filterNot { it.target.contains("kotlin-stdlib") }.isEmpty())

    // Check test variant properties
    val debugTestVariants = debugVariant.tests
    assertTrue(debugTestVariants.keys.containsAll(listOf("debugAndroidTest", "debugUnitTest")))
    val debugAndroidTest = requireNotNull(debugTestVariants["debugAndroidTest"])
    assertTrue {
      debugAndroidTest.srcs.containsAll(
        ANDROID_TEST_SRC_DIRECTORY_PATHS.map { "src/androidTest/$it" } +
            ANDROID_TEST_SRC_DIRECTORY_PATHS.map { "src/androidTestDebug/$it" }
      )
    }
    assertContains(debugAndroidTest.deps, expectedTestDependency)

    val debugUnitTest = requireNotNull(debugTestVariants["debugUnitTest"])
    assertTrue {
      debugUnitTest.srcs.containsAll(ANDROID_TEST_SRC_DIRECTORY_PATHS.map { "src/test/$it" } +
          ANDROID_TEST_SRC_DIRECTORY_PATHS.map { "src/testDebug/$it" })
    }
    assertContains(debugUnitTest.deps, expectedTestDependency)

    val releaseTestVariants = releaseVariant.tests
    assertTrue(releaseTestVariants.keys.containsAll(listOf("releaseUnitTest")))

    val releaseUnitTest = requireNotNull(releaseTestVariants["releaseUnitTest"])
    assertTrue {
      releaseUnitTest.srcs.containsAll(ANDROID_TEST_SRC_DIRECTORY_PATHS.map { "src/test/$it" } +
          ANDROID_TEST_SRC_DIRECTORY_PATHS.map { "src/testRelease/$it" })
    }
    assertContains(releaseUnitTest.deps, expectedTestDependency)
  }

  @Test
  fun `Ensure android library constructs SquareProject`() {
    val projectModelBuilder = SquareProjectModelBuilder()

    val rootProject = ProjectBuilder
      .builder()
      .withProjectDir(temporaryFolder)
      .withName("com.squareup.test")
      .build()

    val libProject = ProjectBuilder
      .builder()
      .withName("lib")
      .withProjectDir(generateLibraryBuild(File(rootProject.projectDir, "lib")))
      .withParent(rootProject)
      .build()

    val expectedTestDependency = SquareDependency("/lib", setOf("transitive"))

    libProject.forceEvaluate()

    val result = projectModelBuilder.buildAll(SquareProject::class.java.name, libProject) as SquareProject

    // Check SquareProject properties
    assertEquals("lib", result.name)
    assertEquals("com.squareup.test", result.namespace)
    assertEquals("lib", result.pathToProject)
    assertEquals("android-library", result.pluginUsed)

    // Check variant properties
    assertTrue(result.variants.keys.containsAll(listOf("debug", "release")))
    val debugVariant = requireNotNull(result.variants["debug"])
    assertTrue {
      debugVariant.srcs.containsAll(
        ANDROID_SRC_DIRECTORY_PATHS.map { "src/debug/$it" } +
            ANDROID_SRC_DIRECTORY_PATHS.map { "src/main/$it" }
      )
    }
    assertTrue(debugVariant.deps.filterNot { it.target.contains("kotlin-stdlib") }.isEmpty())

    val releaseVariant = requireNotNull(result.variants["release"])
    assertTrue {
      releaseVariant.srcs.containsAll(
        ANDROID_SRC_DIRECTORY_PATHS.map { "src/release/$it" } +
            ANDROID_SRC_DIRECTORY_PATHS.map { "src/main/$it" }
      )
    }
    assertTrue(releaseVariant.deps.filterNot { it.target.contains("kotlin-stdlib") }.isEmpty())

    // Check test variant properties
    val debugTestVariants = debugVariant.tests
    assertTrue(debugTestVariants.keys.containsAll(listOf("debugAndroidTest", "debugUnitTest")))
    val debugAndroidTest = requireNotNull(debugTestVariants["debugAndroidTest"])
    assertTrue {
      debugAndroidTest.srcs.containsAll(
        ANDROID_TEST_SRC_DIRECTORY_PATHS.map { "src/androidTest/$it" } +
            ANDROID_TEST_SRC_DIRECTORY_PATHS.map { "src/androidTestDebug/$it" }
      )
    }
    assertContains(debugAndroidTest.deps, expectedTestDependency)

    val debugUnitTest = requireNotNull(debugTestVariants["debugUnitTest"])
    assertTrue {
      debugUnitTest.srcs.containsAll(ANDROID_TEST_SRC_DIRECTORY_PATHS.map { "src/test/$it" } +
          ANDROID_TEST_SRC_DIRECTORY_PATHS.map { "src/testDebug/$it" })
    }
    assertContains(debugUnitTest.deps, expectedTestDependency)

    val releaseTestVariants = releaseVariant.tests
    assertTrue(releaseTestVariants.keys.containsAll(listOf("releaseUnitTest")))

    val releaseUnitTest = requireNotNull(releaseTestVariants["releaseUnitTest"])
    assertTrue {
      releaseUnitTest.srcs.containsAll(ANDROID_TEST_SRC_DIRECTORY_PATHS.map { "src/test/$it" } +
          ANDROID_TEST_SRC_DIRECTORY_PATHS.map { "src/testRelease/$it" })
    }
    assertContains(releaseUnitTest.deps, expectedTestDependency)
  }

  @Test
  fun `Ensure java library constructs SquareProject`() {
    val projectModelBuilder = SquareProjectModelBuilder()

    val rootProject = ProjectBuilder
      .builder()
      .withProjectDir(temporaryFolder)
      .withName("com.squareup.test")
      .build()

    val jvmProject = ProjectBuilder
      .builder()
      .withName("square-jvm")
      .withParent(rootProject)
      .build()

    jvmProject.plugins.apply("java")

    jvmProject.forceEvaluate()

    val result = projectModelBuilder.buildAll(SquareProject::class.java.name, jvmProject) as SquareProject

    // Check SquareProject properties
    assertEquals("square-jvm", result.name)
    assertEquals("com.squareup.test", result.namespace)
    assertEquals("square-jvm", result.pathToProject)
    assertEquals("jvm", result.pluginUsed)

    // Check variant properties
    assertTrue(result.variants.keys.containsAll(listOf("debug", "release")))
    val debugVariant = requireNotNull(result.variants["debug"])
    assertTrue {
      debugVariant.srcs.containsAll(listOf("src/main/resources", "src/main/java"))
    }
    assertTrue(debugVariant.deps.isEmpty())

    val releaseVariant = requireNotNull(result.variants["release"])
    assertTrue {
      releaseVariant.srcs.containsAll(listOf("src/main/resources", "src/main/java"))
    }
    assertTrue(releaseVariant.deps.isEmpty())

    // Check test variant properties
    val debugTestVariants = debugVariant.tests
    assertTrue(debugTestVariants.keys.containsAll(listOf("debugUnitTest")))
    val debugUnitTest = requireNotNull(debugTestVariants["debugUnitTest"])
    assertTrue {
      debugUnitTest.srcs.containsAll(listOf("src/test/resources", "src/test/java"))
    }
    assertTrue(debugUnitTest.deps.isEmpty())

    val releaseTestVariants = releaseVariant.tests
    assertTrue(releaseTestVariants.keys.containsAll(listOf("releaseUnitTest")))

    val releaseUnitTest = requireNotNull(releaseTestVariants["releaseUnitTest"])
    assertTrue {
      releaseUnitTest.srcs.containsAll(listOf("src/test/resources", "src/test/java"))
    }
    assertTrue(releaseUnitTest.deps.isEmpty())
  }

  @Test
  fun `Ensure android test constructs SquareProject`() {
    val projectModelBuilder = SquareProjectModelBuilder()

    val rootProject = ProjectBuilder
      .builder()
      .withProjectDir(temporaryFolder)
      .withName("com.squareup.test")
      .build()

    val libProject = ProjectBuilder
      .builder()
      .withName("test-lib")
      .withProjectDir(generateTestBuild(File(rootProject.projectDir, "lib")))
      .withParent(rootProject)
      .build()

    // Add in the ":app" project
    ProjectBuilder
      .builder()
      .withName("app")
      .withProjectDir(generateApplicationBuild(File(rootProject.projectDir, "app")))
      .withParent(rootProject)
      .build()

    val expectedTestDependency = SquareDependency("/app", setOf("transitive"))

    libProject.forceEvaluate()

    val result = projectModelBuilder.buildAll(SquareProject::class.java.name, libProject) as SquareProject

    // Check SquareProject properties
    assertEquals("test-lib", result.name)
    assertEquals("com.squareup.test", result.namespace)
    assertEquals("lib", result.pathToProject)
    assertEquals("android-test", result.pluginUsed)

    // Check variant properties
    assertTrue(result.variants.keys.containsAll(listOf("debug")))
    val debugVariant = requireNotNull(result.variants["debug"])
    assertTrue {
      debugVariant.srcs.containsAll(
        ANDROID_SRC_DIRECTORY_PATHS.map { "src/debug/$it" } +
                ANDROID_SRC_DIRECTORY_PATHS.map { "src/main/$it" }
      )
    }
    assertTrue(debugVariant.deps.contains(expectedTestDependency))

    // Check test variant properties
    val debugTestVariants = debugVariant.tests
    assertTrue(debugTestVariants.keys.isEmpty())
  }

  @Test
  fun `Ensure dependency substitution rules are accounted for during model building`() {
    val projectModelBuilder = SquareProjectModelBuilder()

    val rootProject = ProjectBuilder
      .builder()
      .withProjectDir(temporaryFolder)
      .withName("com.squareup.test")
      .build()

    ProjectBuilder
      .builder()
      .withName("test-lib")
      .withProjectDir(generateTestBuild(File(rootProject.projectDir, "test-lib")))
      .withParent(rootProject)
      .build()

    // Add in the ":app" project
    val app = ProjectBuilder
      .builder()
      .withName("app")
      .withProjectDir(generateApplicationBuild(File(rootProject.projectDir, "app")))
      .withParent(rootProject)
      .build()

    app.buildFile.appendText("""
      
      dependencies {
        implementation 'org.blah:blah'
      }
    """.trimIndent())

    app.buildscript.configurations.all { config ->
      config.resolutionStrategy.dependencySubstitution { substitution ->
        substitution.substitute(substitution.module("org.blah:blah"))
          .using(substitution.project(":test-lib"))
      }
    }

    app.forceEvaluate()

    val result = projectModelBuilder.buildAll(SquareProject::class.java.name, app) as SquareProject

    // Check SquareProject properties
    assertEquals("app", result.name)
    assertEquals("com.squareup.test", result.namespace)
    assertEquals("app", result.pathToProject)
    assertEquals("android-app", result.pluginUsed)

    assertTrue("Dependencies were not substituted") {
      result.variants.values.all { configuration ->
        configuration.deps.any { dep ->
          dep.target == "/test-lib"
        } && configuration.deps.none { dep ->
          dep.target == "@maven://org.blah:blah"
        }
      }
    }
  }

  @Test
  fun `Do not throw exception if a non-Java or non-Android plugin is used`() {
    val projectModelBuilder = SquareProjectModelBuilder()

    val project = ProjectBuilder.builder().build()

    assertDoesNotThrow {
      projectModelBuilder.buildAll(SquareProject::class.java.name, project)
    }
  }
}
