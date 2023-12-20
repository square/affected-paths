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

package com.squareup.tooling.support.core.extractors

import com.squareup.tooling.models.SquareProject
import org.gradle.api.Project
import java.util.ServiceConfigurationError
import java.util.ServiceLoader
import kotlin.streams.asSequence

/**
 * Main extractor that provides [SquareProject] objects from the given [Project].
 *
 * Currently, implementations are provided via [ServiceLoader] pattern in order to allow custom extractors for other
 * project types.
 */
public interface SquareProjectExtractor {

  /**
   * Extract the [SquareProject] from the given [Project]. May return `null` if nothing can be extracted.
   */
  public fun extractSquareProject(project: Project, rootDir: String? = null): SquareProject?

  public companion object {

    /**
     * Provides the list of all extractors loaded via [ServiceLoader].
     * Will throw an exception if no extractors are found, or if there is an issue during loading.
     */
    @get:Throws(IllegalStateException::class, ServiceConfigurationError::class)
    public val squareProjectExtractors: List<SquareProjectExtractor> by lazy {
      ServiceLoader.load(SquareProjectExtractor::class.java)
        .stream()
        .use { stream ->
          stream.asSequence()
          .sortedBy { provider ->
            when (provider.type().name) {
              "com.squareup.tooling.support.android.SquareProjectExtractorImpl" -> 0
              "com.squareup.tooling.support.jvm.SquareProjectExtractorImpl" -> 1
              else -> Int.MAX_VALUE
            }
          }.map { it.get() }
          .toList()
          .ifEmpty { throw SquareProjectExtractorLoaderException("No extractors found") }
        }
    }
  }
}

private class SquareProjectExtractorLoaderException(message: String) : IllegalStateException(message)
