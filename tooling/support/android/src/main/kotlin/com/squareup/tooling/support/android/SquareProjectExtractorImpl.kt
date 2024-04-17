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

import com.squareup.tooling.models.SquareProject
import com.squareup.tooling.support.core.extractors.SquareProjectExtractor
import org.gradle.api.Project

// Android SquareProject extractor
internal class SquareProjectExtractorImpl : SquareProjectExtractor {

  override fun extractSquareProject(project: Project): SquareProject? {
    return when {
      // Android app plugin logic
      project.plugins.hasPlugin("com.android.application") -> project.extractAppModuleProject()

      // Android library plugin logic
      project.plugins.hasPlugin("com.android.library") -> project.extractLibraryModuleProject()

      // Android test plugin logic
      project.plugins.hasPlugin("com.android.test") -> project.extractTestModuleProject()

      else -> null
    }
  }
}
