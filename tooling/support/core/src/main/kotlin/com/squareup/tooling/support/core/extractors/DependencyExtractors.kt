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
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentSelector
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.api.internal.artifacts.dependencies.AbstractExternalModuleDependency
import org.gradle.api.internal.artifacts.dependencies.AbstractModuleDependency
import org.gradle.util.GradleVersion

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

  return allDependencies.asSequence()
    .map { dependencyResult ->
      when (dependencyResult) {
          is ResolvedDependencyResult -> {
            return@map when (val id = dependencyResult.selected.id) {
              is ProjectComponentIdentifier -> {
                val path = gradlePathToFilePath(id.identityPath)
                val isTransitive = dependencyResult !in directDependencies
                SquareDependency(
                  target = path,
                  tags = if (isTransitive) setOf("transitive") else emptySet()
                )
              }

              is ModuleComponentIdentifier -> {
                @Suppress("UselessCallOnNotNull")
                SquareDependency(
                  target = "@maven://${id.moduleIdentifier.group.orEmpty().ifBlank { "undefined" }}:${id.moduleIdentifier.name}"
                )
              }

              else -> {
                println("WARNING: Unknown dep type $javaClass")
                SquareDependency(target = id.displayName)
              }
            }
          }

        is UnresolvedDependencyResult -> {
          return@map when (val id = dependencyResult.requested) {
            is ProjectComponentSelector -> {
              val path = gradlePathToFilePath(id.identityPath)
              val isTransitive = dependencyResult !in directDependencies
              SquareDependency(
                target = path,
                tags = if (isTransitive) setOf("transitive") else emptySet()
              )
            }

            is ModuleComponentSelector -> {
              @Suppress("UselessCallOnNotNull")
              SquareDependency(
                target = "@maven://${id.moduleIdentifier.group.orEmpty().ifBlank { "undefined" }}:${id.moduleIdentifier.name}"
              )
            }

            else -> {
              println("WARNING: Unknown dep type $javaClass")
              SquareDependency(target = id.displayName)
            }
          }
        }

        else -> {
          return@map SquareDependency("unknown")
        }
      }
    }
}


// Converts the given Gradle Dependency into a SquareDependency use to construct the model project
private fun Dependency.extractSquareDependency(project: Project): SquareDependency {
  return when (this) {
    // Used by maven dependencies.
    is AbstractExternalModuleDependency -> {
      @Suppress("UselessCallOnNotNull")
      SquareDependency(target = "@maven://${group.orEmpty().ifBlank { "undefined" }}:$name")
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
  val filePath = path.replace(':', '/')
  return if (filePath.startsWith('/')) filePath else "/$filePath"
}

private val ProjectComponentIdentifier.identityPath: String
  get() {
    return if (GradleVersion.current() >= GradleVersion.version("8.2")) {
      if (projectPath.startsWith(build.buildPath)) projectPath else "${build.buildPath}$projectPath"
    } else {
      if (projectPath.startsWith(build.name)) projectPath else "${build.name}$projectPath"
    }
  }

private val ProjectComponentSelector.identityPath: String
  get() {
    return if (GradleVersion.current() >= GradleVersion.version("8.2")) {
      if (projectPath.startsWith(buildPath)) projectPath else "$buildPath$projectPath"
    } else {
      if (projectPath.startsWith(buildName)) projectPath else "$buildName$projectPath"
    }
  }
