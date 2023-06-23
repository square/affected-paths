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

package com.squareup.tooling.support.core.models

import com.squareup.tooling.models.SquareProject
import com.squareup.tooling.models.SquareVariantConfiguration

/**
 * Helper function for creating [SquareProject] objects
 */
public fun SquareProject(
  name: String,
  namespace: String,
  pathToProject: String,
  pluginUsed: String,
  variants: Map<String, SquareVariantConfiguration>,
): SquareProject = SquareProjectImpl(
  name = name,
  namespace = namespace,
  pathToProject = pathToProject,
  pluginUsed = pluginUsed,
  variants = variants
)

// Internal implementation, hidden from the world
private data class SquareProjectImpl(
  override val name: String,
  override val namespace: String,
  override val pathToProject: String,
  override val pluginUsed: String,
  override val variants: Map<String, SquareVariantConfiguration>,
) : SquareProject
