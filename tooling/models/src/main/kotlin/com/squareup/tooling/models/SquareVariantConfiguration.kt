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

package com.squareup.tooling.models

import java.io.Serializable

/**
 * Model describing the variant configuration of a project
 *
 * @property srcs Source files of this configuration
 * @property deps List of dependencies for this specific configuration
 * @property tests Map of tests associated with this variant
 */
public interface SquareVariantConfiguration : Serializable {
  public val srcs: Set<String>
  public val deps: Set<SquareDependency>
  public val tests: Map<String, SquareTestConfiguration>

  public companion object {
    private const val serialVersionUID: Long = 1L
  }
}
