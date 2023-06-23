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

import com.squareup.tooling.models.SquareDependency

/**
 * Helper function for creating [SquareDependency] objects
 */
public fun SquareDependency(
  target: String,
  tags: Set<String> = emptySet()
): SquareDependency = SquareDependencyImpl(
  target = target,
  tags = tags
)

// Internal implementation, hidden from the world
private data class SquareDependencyImpl(
  override val target: String,
  override val tags: Set<String>
) : SquareDependency
