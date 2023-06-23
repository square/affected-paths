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

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.AndroidSourceDirectorySet
import java.io.File

// Extracts source directories from the android extension.
internal fun BaseExtension.sourceIndexExtractor(): Map<String, Sequence<File>> {
  return sourceSets.associate { ss ->
    ss.name to sequenceOf(
      ss.aidl,
      ss.assets,
      ss.java,
      ss.jni,
      ss.jniLibs,
      ss.res,
      ss.resources,
      ss.renderscript,
      ss.kotlin,
      ss.mlModels,
      ss.shaders
      // Filtering in order to case, since "kotlin" is returning a different "AndroidSourceDirectorySet",
      // Even though the underlying object implements the correct "AndroidSourceDirectorySet"
    ).filterIsInstance<AndroidSourceDirectorySet>()
      .map { it.srcDirs }
      .flatten()
      .plus(ss.manifest.srcFile)
  }
}
