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

package com.squareup.affected.paths.core

import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isDirectory

/**
 * Common configuration flags used by the [CoreAnalyzer]
 * and applied to all commands.
 */
@Suppress("unused")
public data class CoreOptions @JvmOverloads constructor(
  /** Log Gradle build output */
  val logGradle: Boolean = false,

  /** Directory of the Gradle build to use */
  val directory: Path = Path(".").toRealPath(),

  /** The commit to diff against `HEAD` */
  val comparisonCommit: String = "",

  /**
   * Enables Gradle debugging. Passes `-Dorg.gradle.debug=true` to the daemon. See also the documentation on
   * [Gradle Debugging](https://docs.gradle.org/current/userguide/command_line_interface.html#sec:command_line_debugging).
   */
  val debugGradle: Boolean = false,

  /**
   * Allow Gradle to query for models in parallel.
   * @see org.gradle.tooling.BuildController.getCanQueryProjectModelInParallel
   */
  val allowGradleParallel: Boolean = false,

  /** Sets the `-Xms` flag with this value, if not `null`. Value is in MB */
  val initialGradleMemory: Int? = null,

  /** Sets the `-Xmx` flag with this value, if not `null`. Value is in MB */
  val maxGradleMemory: Int? = null,

  /** Java VM arguments to use for Gradle Tooling API */
  val customJvmFlags: List<String> = emptyList(),

  /** Arguments to pass to Gradle command line */
  val customGradleFlags: List<String> = emptyList(),

  /** List of changed files to evaluate. If empty, internal Git tool is used to determine changed files */
  val changedFiles: List<String> = emptyList(),

  /** Auto-injects the "com.squareup.tooling" plugin to all projects in the build */
  val autoInjectPlugin: Boolean = true,

  /** Include any "includeBuild" builds from the current build */
  val useIncludeBuild: Boolean = true,

  /** Gradle distribution file to use */
  val gradleDistributionPath: Path? = null,

  /**
   * Pass in a custom Gradle installation, instead of using the build distribution
   *
   * **NOTE**: This will override `gradleDistributionPath` if used.
   */
  val gradleInstallationPath: Path? = null,

  /**
   * Gradle version to use for the current build
   *
   * **NOTE**: This will override `gradleInstallationPath` and `gradleDistributionPath` if used.
   */
  val gradleVersion: String? = null,

  /** Gradle user home directory to use */
  val gradleUserHome: Path? = null,

  /**
   * Add the build scan flag to the tooling.
   *
   * **Note**: This will cause the default tasks of a build to run.
   */

  val useBuildScan: Boolean = false,
) {

  init {
    require(directory.isDirectory()) { "Must point to a valid directory" }
  }

  internal val jvmArgs: List<String> = buildList {
    addAll(customJvmFlags)
    if (initialGradleMemory != null) {
      add("-Xms${initialGradleMemory}M")
    }

    if (maxGradleMemory != null) {
      add("-Xmx${maxGradleMemory}M")
    }

    if (debugGradle) {
      add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005")
    }
  }

  internal val gradleArgs: List<String> = buildList {
    addAll(customGradleFlags)
    add("-PuseIncludeBuild=$useIncludeBuild")
    if (autoInjectPlugin) {
      add("-I")
      add(
        File.createTempFile("tempScript", ".gradle").apply {
          writeText(
            """
              beforeProject { project ->
                project.buildscript {
                  repositories {
                    mavenLocal()
                    mavenCentral()
                  }
                  dependencies {
                    classpath "com.squareup.affected.paths:tooling-support:0.1.3"
                  }
                }
              }
              afterProject { project ->
                if (!project.plugins.hasPlugin("com.squareup.tooling")) {
                  project.apply plugin: "com.squareup.tooling"
                }
              }
            """.trimIndent()
          )
          deleteOnExit()
        }.absolutePath
      )
    }
    if (useBuildScan) {
      add("--scan")
    }
  }
}
