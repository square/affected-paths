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

package com.squareup.affected.paths.core.di

import com.squareup.affected.paths.core.git.SquareGit
import com.squareup.affected.paths.core.git.SquareGitImpl
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Module to inject the [SquareGit] class
 */
internal fun gitModule() = module {
  single<SquareGit> {
    SquareGitImpl(
      rootDir = get(),
      comparisonCommit = get(named("comparison"))
    )
  }
}
