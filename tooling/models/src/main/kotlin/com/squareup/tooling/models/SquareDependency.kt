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
 * Model describing varying [org.gradle.api.artifacts.Dependency] objects in Gradle
 *
 * @property target Target of this dependency. Used to describe either external dependencies,
 * defined by GAV coordinates (e.g., `'com.foo:bar:1.0'`);
 * or project dependencies, defined by paths (e.g., `':foo:bar'`).
 *
 * @property tags Used to describe dependency specific tags
 * (such as [org.gradle.api.artifacts.ModuleDependency.isTransitive])
 */
public interface SquareDependency : Serializable {
  public val target: String
  public val tags: Set<String>

  public companion object {
    private const val serialVersionUID: Long = 1L
  }
}
