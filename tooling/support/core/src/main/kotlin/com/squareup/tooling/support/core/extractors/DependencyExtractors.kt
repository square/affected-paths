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
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.internal.artifacts.dependencies.AbstractExternalModuleDependency
import org.gradle.api.internal.artifacts.dependencies.AbstractModuleDependency
import org.jetbrains.kotlin.util.prefixIfNot

/**
 * Extracts SquareDependency objects from the given configurations
 */
public fun ConfigurationContainer.extractSquareDependencies(
  project: Project,
  vararg configurationNames: String
): Sequence<SquareDependency> {
  return configurationNames.asSequence()
    .map { configurationName -> getByName(configurationName) }
    .flatMap { configuration ->
      configuration.extractResolvedProjectDependencies(project)
    }
}

/**
 * Extracts Gradle Dependency objects from the given configurations
 */
public fun Configuration.extractDependencies(): Sequence<Dependency> {
  return allDependencies.asSequence()
}

/**
 * Extracts dependencies from the resolved artifacts of the configuration.
 *
 * If configuration is not resolvable, it will extract dependencies from the configuration itself.
 */
public fun Configuration.extractResolvedProjectDependencies(project: Project): Sequence<SquareDependency> {
  if (!isCanBeResolved) return extractDependencies().map { it.extractSquareDependency(project) }

  val resolutionResult = incoming.resolutionResult
  val allDependencies = resolutionResult.allDependencies
  val directDependencies = resolutionResult.root.dependencies.toSet()

  return allDependencies.asSequence().filterIsInstance<ResolvedDependencyResult>()
    .map { resolvedDependencyResult ->
      val id = resolvedDependencyResult.selected.id
      return@map when (id) {
        is ProjectComponentIdentifier -> {
          val path = gradlePathToFilePath(id.identityPath)
          val isTransitive = resolvedDependencyResult !in directDependencies
          SquareDependency(
            target = path,
            tags = if (isTransitive) setOf("transitive") else emptySet()
          )
        }
        is ModuleComponentIdentifier -> {
          SquareDependency(target = "@maven://${id.moduleIdentifier.group}:${id.moduleIdentifier.name}")
        }
        else -> {
          println("WARNING: Unknown dep type $javaClass")
          SquareDependency(target = id.displayName)
        }
      }
    }
}


// Converts the given Gradle Dependency into a SquareDependency use to construct the model project
public fun Dependency.extractSquareDependency(project: Project): SquareDependency {
  return when (this) {
    // Used by maven dependencies.
    is AbstractExternalModuleDependency -> {
      SquareDependency(target = "@maven://${group}:$name")
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

private val ProjectComponentIdentifier.identityPath: String
  get() = projectPath.prefixIfNot(build.buildPath)

