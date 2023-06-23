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
 * Model describing a project of a build.
 *
 * @property name Name returned by [org.gradle.api.Project.getName]
 * @property namespace Name returned by [org.gradle.api.Project.getGroup]
 * @property pathToProject File path to this project relative to the build root directory
 * @property pluginUsed Name of the plugin used by this project (ex. App, Lib, Java)
 * @property variants Map of [SquareVariantConfiguration] objects derived from the project
 *
 */
public interface SquareProject : Serializable {
  public val name: String
  public val namespace: String
  public val pathToProject: String
  public val pluginUsed: String
  public val variants: Map<String, SquareVariantConfiguration>

  public companion object {
    private const val serialVersionUid: Long = 1L
  }
}
