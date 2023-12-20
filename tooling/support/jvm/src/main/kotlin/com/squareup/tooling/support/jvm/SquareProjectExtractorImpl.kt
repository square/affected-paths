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

import com.squareup.tooling.models.SquareProject
import com.squareup.tooling.support.core.extractors.SquareProjectExtractor
import org.gradle.api.Project

// Jvm SquareProject extractor
internal class SquareProjectExtractorImpl : SquareProjectExtractor {

  override fun extractSquareProject(project: Project, gitRoot: String?): SquareProject? {
    return when {
      // Java plugin logic
      project.plugins.hasPlugin("org.gradle.java-base") -> project.extractJavaModuleProject(gitRoot)

      else -> null
    }
  }
}
