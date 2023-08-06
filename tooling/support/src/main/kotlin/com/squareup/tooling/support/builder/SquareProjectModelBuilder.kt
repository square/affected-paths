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

import com.squareup.tooling.models.SquareProject
import com.squareup.tooling.support.core.extractors.SquareProjectExtractor
import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder

/**
 * Responsible for building tooling models ([SquareProject]) to be used by the tooling API.
 *
 * @see [ToolingModelBuilder]
 */
public interface SquareProjectModelBuilder : ToolingModelBuilder

// Hide the implementation and expose only a helper function
@JvmSynthetic
internal fun SquareProjectModelBuilder(): SquareProjectModelBuilder = SquareProjectModelBuilderImpl()

private class SquareProjectModelBuilderImpl : SquareProjectModelBuilder {

  private val extractors = SquareProjectExtractor.squareProjectExtractors

  override fun canBuild(modelName: String): Boolean {
    // Currently, this builder only supports one model type
    return modelName == SquareProject::class.java.name
  }

  override fun buildAll(modelName: String, project: Project): Any? {
    if (modelName == SquareProject::class.java.name) {
      return extractors.firstNotNullOfOrNull { it.extractSquareProject(project) }
    }

    // If this is used for any other project types, or for some other model type,
    // furiously throw an error and not a desk
    throw IllegalArgumentException(
      "Unknown model $modelName found, expected ${SquareProject::class.java.name}"
    )
  }
}
