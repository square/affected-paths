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

package com.squareup.tooling.support.android

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import com.squareup.tooling.models.SquareTestConfiguration
import com.squareup.tooling.support.core.extractors.extractResolvedProjectDependencies
import com.squareup.tooling.support.core.models.SquareTestConfiguration
import org.gradle.api.Project

// Ensure extension function only exists for "UnitTestVariant"
internal fun UnitTestVariant.extractSquareTestConfiguration(
  project: Project
): SquareTestConfiguration {
  return (this as BaseVariant).extractSquareTestConfiguration(project)
}

// Ensure extension function only exists for "TestVariant"
internal fun TestVariant.extractSquareTestConfiguration(project: Project): SquareTestConfiguration {
  return (this as BaseVariant).extractSquareTestConfiguration(project)
}

// Extracts test configurations and dependencies from the given variant. Not meant for all variants.
private fun BaseVariant.extractSquareTestConfiguration(project: Project): SquareTestConfiguration {
  val dirs = sourceSets.asSequence()
    .flatMap { sp ->
      sequenceOf(
        sp.assetsDirectories,
        sp.cDirectories,
        sp.cppDirectories,
        sp.javaDirectories,
        sp.jniLibsDirectories,
        sp.renderscriptDirectories,
        sp.resDirectories,
        sp.resourcesDirectories,
        sp.shadersDirectories
      ).flatten() + sp.manifestFile
    }.map { it.toRelativeString(project.projectDir) }
  return SquareTestConfiguration(
    srcs = dirs.toSet(),
    deps = compileConfiguration.extractResolvedProjectDependencies(project).toSet()
  )
}
