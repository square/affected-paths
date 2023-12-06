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

package com.squareup.affected.paths.app.utils

import com.squareup.affected.paths.app.options.BaseConfigurationOptions
import com.squareup.affected.paths.core.CoreOptions

internal fun BaseConfigurationOptions.toCoreOptions(): CoreOptions {
  return CoreOptions(
    logGradle = logGradle,
    directory = directory,
    comparisonCommit = comparisonCommit,
    debugGradle = debugGradle,
    allowGradleParallel = allowGradleParallel,
    initialGradleMemory = initialGradleMemory,
    maxGradleMemory = maxGradleMemory,
    customJvmFlags = listOf("-XX:-MaxFDLimit"),
    customGradleFlags = listOf("--stacktrace", "-Dorg.gradle.unsafe.isolated-projects=true", "--configuration-cache-problems=warn", "-Dorg.gradle.unsafe.configuration-cache.max-problems=2000"),
    autoInjectPlugin = autoInject,
    changedFiles = changedFiles,
    gradleInstallationPath = gradleInstallationPath,
  )
}
