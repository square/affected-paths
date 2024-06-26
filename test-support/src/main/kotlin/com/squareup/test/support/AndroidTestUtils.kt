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

package com.squareup.test.support

import java.io.File

// Helper function for generating a bare-bone Android app project
public fun generateApplicationBuild(temporaryFolder: File): File {
  val buildDirectory = temporaryFolder.canonicalFile.apply { mkdirs() }
  File(buildDirectory, "build.gradle").apply {
    writeText("""
        apply plugin: 'com.android.application'
        apply plugin: 'kotlin-android'

        android {
            namespace 'com.squareup.test.app'
            compileSdk 33
            defaultConfig {
                applicationId 'com.squareup.test.app'
                targetSdk 17
                minSdk 17
                versionCode 1
                versionName "1.0"
            }
        }
      """.trimIndent())
  }
  return buildDirectory
}

// Helper function for generating a bare-bone Android library project
public fun generateLibraryBuild(temporaryFolder: File): File {
  val buildDirectory = temporaryFolder.canonicalFile.apply { mkdirs() }
  File(buildDirectory, "build.gradle").apply {
    writeText("""
        apply plugin: 'com.android.library'
        apply plugin: 'kotlin-android'

        android {
            namespace 'com.squareup.test.library'
            compileSdk 33
            defaultConfig {
                targetSdk 17
                minSdk 17
            }
        }
      """.trimIndent())
  }
  return buildDirectory
}

// Helper function for generating a bare-bone Android test project
public fun generateTestBuild(temporaryFolder: File): File {
  val buildDirectory = temporaryFolder.canonicalFile.apply { mkdirs() }
  File(buildDirectory, "build.gradle").apply {
    writeText("""
        apply plugin: 'com.android.test'
        apply plugin: 'kotlin-android'

        android {
            targetProjectPath = ":app"
            namespace 'com.squareup.test.library'
            compileSdk 33
            defaultConfig {
                targetSdk 17
                minSdk 17
            }
        }
      """.trimIndent())
  }
  return buildDirectory
}
