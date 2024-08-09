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

import com.squareup.tooling.models.SquareDependency
import com.squareup.tooling.support.core.models.SquareDependency
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.internal.artifacts.DefaultProjectComponentIdentifier
import org.gradle.api.internal.artifacts.dependencies.AbstractExternalModuleDependency
import org.gradle.api.internal.artifacts.dependencies.AbstractModuleDependency

public fun ConfigurationContainer.extractSquareDependencies(
  project: Project,
  vararg configurationNames: String
): Sequence<SquareDependency> {
  val useIncludeBuild = project.findProperty("useIncludeBuild") == "true"
  return configurationNames.asSequence()
    .map { configurationName -> getByName(configurationName) }
    .flatMap { configuration ->
      sequence {
        yieldAll(configuration.extractDependencies()
          .map { it.extractSquareDependency(project) })
        if (useIncludeBuild) {
          yieldAll(configuration.extractResolvedProjectDependencies())
        }
      }
    }
}

/**
 * Extracts Gradle Dependency objects from the given configurations
 */
public fun Configuration.extractDependencies(): Sequence<Dependency> {
  return allDependencies.asSequence()
}

/**
 * Extracts project dependencies from the resolved artifacts of the configuration.
 *
 * In cases where an included build is used (e.g., through `includeBuild`), project dependencies
 * may not appear in the standard `Configuration.allDependencies` list. Instead, they are
 * resolved during the configuration resolution process.
 *
 * It only works if the configuration can be resolved (`isCanBeResolved` is true), which excludes
 * configurations like `compileOnly`.
 */
public fun Configuration.extractResolvedProjectDependencies(): Sequence<SquareDependency> {
  if (!isCanBeResolved) return emptySequence()

  val resolutionResult = incoming.resolutionResult
  val allDependencies = resolutionResult.allDependencies
  val directDependencies = resolutionResult.root.dependencies.toSet()

  return allDependencies.asSequence()
    .mapNotNull { it as? ResolvedDependencyResult }
    .mapNotNull { resolvedDependencyResult ->
      val identifier = resolvedDependencyResult.selected.id as? DefaultProjectComponentIdentifier
      identifier?.let {
        if (it.identityPath.path != ":") {
          val path = gradlePathToFilePath(it.identityPath.path)
          val isTransitive = resolvedDependencyResult !in directDependencies
          SquareDependency(
            target = path,
            tags = if (isTransitive) setOf("transitive") else emptySet()
          )
        } else {
          null
        }
      }
    }
}


// Converts the given Gradle Dependency into a SquareDependency use to construct the model project
public fun Dependency.extractSquareDependency(project: Project): SquareDependency {
  return when (this) {
    // Used by maven dependencies.
    is AbstractExternalModuleDependency -> {
      SquareDependency(target = "@maven://${group ?: "undefined"}:$name")
    }

    // Meant to capture non-project dependencies, but in reality captures project dependencies.
    is AbstractModuleDependency -> {
      SquareDependency(
        target = keyRelativeTo(project.rootProject.name),
        tags = if (isTransitive) setOf("transitive") else emptySet()
      )
    }

    // In case the "DefaultProjectDependency" object is not of type "AbstractModuleDependency"
    is ProjectDependency -> {
      SquareDependency(
        target = keyRelativeTo(project.rootProject.name),
        tags = if (isTransitive) setOf("transitive") else emptySet()
      )
    }

    // Meant to capture all other dependency types. Will not handle fileTree dependencies well.
    else -> {
      println("WARNING: Unknown dep type $javaClass")
      SquareDependency(target = keyRelativeTo(project.rootProject.name))
    }
  }
}

// Gives the name of this target relative to the path provided.
// Meant to de-duplicate project name from target string.
private fun Dependency.keyRelativeTo(relative: String = ""): String {
  if (this is ProjectDependency) {
    return gradlePathToFilePath(dependencyProject.path)
  }
  val s = group?.split(".", ":", "/") ?: emptyList()
  val result = when (s.firstOrNull()) {
    relative -> s.drop(1)
    else -> s
  }
  return result.plus(name).joinToString("") { "/$it" }
}

private fun gradlePathToFilePath(path: String): String {
  return path.replace(':', '/')
}
