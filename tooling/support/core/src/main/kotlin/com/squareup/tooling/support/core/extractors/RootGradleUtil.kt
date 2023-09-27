package com.squareup.tooling.support.core.extractors

import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.invocation.DefaultGradle

/**
 * Gets the root Gradle build, if one exists.
 */
public fun Gradle.getRootGradle(): Gradle {
    return parent?.getRootGradle() ?: this
}

/**
 * Finds the relative path of the project to the root build directory, if there is a relative root.
 * Otherwise, returns `null`.
 */
public fun Project.relativePathToRootBuild(): String? {
    val buildRootFile = (gradle.getRootGradle() as DefaultGradle).owner.buildRootDir
    return projectDir.relativeToOrNull(buildRootFile)?.path
}

/**
 * Finds the relative path of the project to the repo root.
 */
public fun Project.relativePathToRootProject(): String {
    return projectDir.toRelativeString(rootDir)
}
