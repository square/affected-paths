package com.squareup.tooling.support.core.extractors

import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import java.io.File

/**
 * Gets the root Gradle build, if one exists.
 */
public fun Gradle.getRootGradle(): Gradle {
    return parent?.getRootGradle() ?: this
}

/**
 * Finds the relative path of the project to the repo root.
 */
public fun Project.relativePathToRootProject(gitRoot: String?): String {
    return if (gitRoot.isNullOrBlank()) {
        projectDir.toRelativeString(rootDir)
    } else {
        projectDir.relativeTo(File(gitRoot)).path
    }
}
