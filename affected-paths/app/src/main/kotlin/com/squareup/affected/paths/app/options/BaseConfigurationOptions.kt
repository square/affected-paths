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

package com.squareup.affected.paths.app.options

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.squareup.affected.paths.app.utils.LogBackLogLevelTypeConverter
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory
import picocli.CommandLine.Option
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isDirectory

/**
 * Base configuration flags
 */
@Suppress("unused")
internal class BaseConfigurationOptions {

  @Option(
    names = ["--logging"],
    description = ["Logging level (OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL)"],
    converter = [LogBackLogLevelTypeConverter::class],
    defaultValue = "info"
  )
  private fun setLogLevel(level: Level) {
    val logger = LoggerFactory.getLogger(ROOT_LOGGER_NAME) as Logger
    logger.level = level
  }

  @Option(
    names = ["--log-gradle"],
    description = ["Output Gradle logs"],
    fallbackValue = "true"
  )
  var logGradle: Boolean = false
    internal set

  @Option(
    names = ["--debug-gradle"],
    description = ["Makes the Gradle daemon used by TAPI debuggable"],
    fallbackValue = "true"
  )
  var debugGradle: Boolean = false
    internal set

  @set:Option(
    names = ["--dir"],
    description = ["Project directory"],
    defaultValue = "."
  )
  var directory: Path = Path(".").toRealPath()
    internal set(value) {
      require(value.isDirectory()) { "Param --dir must point to a valid directory" }
      field = value.toRealPath()
    }

  @set:Option(
    names = ["--comparison-commit"],
    description = ["The commit hash to compare against for git diff"],
    defaultValue = ""
  )
  lateinit var comparisonCommit: String
    internal set

  @Option(
    names = ["--allow-gradle-parallel"],
    description = ["Allows parallel configuration of Gradle builds"],
  )
  var allowGradleParallel: Boolean = false
    internal set

  @Option(
    names = ["--gradle-initial-memory"],
    description = [
      "Sets the initial JVM memory size for Gradle daemon (in MB)",
      "Equivalent to using '-Xms' for the Gradle JVM args"
    ]
  )
  var initialGradleMemory: Int? = null
    internal set

  @Option(
    names = ["--gradle-max-memory"],
    description = [
      "Sets the max JVM memory size for Gradle daemon (in MB)",
      "Equivalent to using '-Xmx' for the Gradle JVM args"
    ]
  )
  var maxGradleMemory: Int? = null
    internal set

  @Option(
    names = ["--inject-plugin"],
    description = ["Injects the \"com.squareup.tooling\" plugin to all projects in the build"]
  )
  var autoInject: Boolean = false
    internal set

  @Option(
    names = ["--changed-files"],
    description = ["List of changed files to use instead of the Git diff"],
    split = " "
  )
  var changedFiles: List<String> = emptyList()
    internal set

  @Option(
    names = ["--gradle-installation-path"],
    description = ["Use a custom Gradle installation"]
  )
  var gradleInstallationPath: Path? = null
    internal set
}
