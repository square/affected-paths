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

/**
 * Singleton container for the [SquareProjectModelBuilder]. This is required due to an issue with
 * injecting [org.gradle.tooling.provider.model.ToolingModelBuilder] as reported here:
 * [gradle/gradle/issues/17559](https://github.com/gradle/gradle/issues/17559)
 */
public object SquareModelsBuildService {

  public val squareProjectModelBuilder: SquareProjectModelBuilder = SquareProjectModelBuilder()
}
