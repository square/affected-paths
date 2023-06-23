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

package com.squareup.tooling.support

import com.squareup.tooling.support.builder.SquareModelsBuildService
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import javax.inject.Inject

/**
 * Convenience plugin for registering [com.squareup.tooling.support.builder.SquareProjectModelBuilder] to the
 * [ToolingModelBuilderRegistry].
 */
public class SquareToolingPlugin @Inject constructor(
  private val registry: ToolingModelBuilderRegistry
) : Plugin<Project> {

  override fun apply(target: Project) {
    // Attempting to register the ToolBuilder by injection
    // causes this bug: https://github.com/gradle/gradle/issues/17559
    // For now, registering it as a singleton
    registry.register(SquareModelsBuildService.squareProjectModelBuilder)
  }
}
