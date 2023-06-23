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

package com.squareup.affected.paths.core.models

import com.squareup.affected.paths.core.CoreOptions
import com.squareup.tooling.models.SquareProject

/**
 * Analysis result containing information about the current build between the current commit and
 * the [CoreOptions.comparisonCommit]
 *
 * @property changedFiles List of changed files from Git. This is determined by getting the diff of the
 * current head and the comparison commit passed in via [CoreOptions.comparisonCommit], or by diff of the current head
 * and the last commit.
 *
 * @param projectMap Map of all [SquareProject] objects returned via Gradle Tooling API. This map
 * is a key-value pair of the [SquareProject.pathToProject] to [SquareProject].
 *
 * @param affectedResults List of [AffectedResult] objects, which contain the set of projects
 * affected, the variant of the projects, and the file that caused them to be affected.
 */
public data class AnalysisResult(
  val changedFiles: List<String>,
  val projectMap: Map<String, SquareProject>,
  val affectedResults: List<AffectedResult>
)
