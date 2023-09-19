package com.squareup.affected.paths.core.git

import com.squareup.affected.paths.core.CoreAnalyzer
import com.squareup.affected.paths.core.CoreOptions
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.time.Duration.Companion.minutes

class AffectedPathsTest {

    @TempDir
    lateinit var root: File

    @Test
    fun `Basic affected paths test`() = runTest(timeout = 3.minutes) {
        // Prep
        createSettingsFile(
            rootDir = root,
            contents = """
                rootProject.name = 'blah'
                include 'app'
                include 'library'
            """.trimIndent()
        )

        createModule(
            rootDir = root,
            name = "app",
            contents =
            """
                plugins {
                    id 'application'
                }
                
                dependencies {
                    implementation(':library')
                }
                """.trimIndent()
        )

        createModule(
            rootDir = root,
            name = "library",
            contents =
            """
                plugins {
                    id 'java'
                }
                """.trimIndent()
        )

        // Test
        val analyzer = CoreAnalyzer(
            CoreOptions(
                directory = root.toPath(),
                changedFiles = listOf("app/build.gradle")
            )
        )

        val result = analyzer.analyze()

        // Results
        assertContentEquals(listOf("app", "library"), result.projectMap.keys)
        assertContentEquals(listOf("app"), result.affectedResults.flatMap { it.affectedProjectPaths }.distinct())

    }

    @Test
    fun `Included builds test`() = runTest(timeout = 3.minutes) {
        // Prep
        val build1 = File(root, "build1").apply { mkdirs() }
        val build2 = File(build1, "build2").apply { mkdirs() }
        createSettingsFile(
            rootDir = build1,
            contents = """
                rootProject.name = 'blah'
                includeBuild 'build2'
                include 'app'
                include 'library'
            """.trimIndent()
        )

        createModule(
            rootDir = build1,
            name = "app",
            contents =
            """
                plugins {
                    id 'application'
                }
                
                dependencies {
                    implementation(':library')
                }
                """.trimIndent()
        )

        createModule(
            rootDir = build1,
            name = "library",
            contents =
            """
                plugins {
                    id 'java'
                }
                """.trimIndent()
        )

        createSettingsFile(
            rootDir = build2,
            contents = """
                rootProject.name = 'blah2'
                include 'foobar'
            """.trimIndent()
        )

        createModule(
            rootDir = build2,
            name = "foobar",
            contents =
            """
                plugins {
                    id 'java'
                }
                """.trimIndent()
        )

        val analyzer = CoreAnalyzer(
            CoreOptions(
                directory = build1.toPath(),
                changedFiles = listOf("library/build.gradle")
            )
        )

        val result = analyzer.analyze()

        assertContentEquals(listOf("build2/foobar", "app", "library"), result.projectMap.keys)
        assertContentEquals(listOf("library"), result.affectedResults.flatMap { it.affectedProjectPaths }.distinct())
    }

    @Test
    fun `Do not use included builds test`() = runTest(timeout = 3.minutes) {
        // Prep
        val build1 = File(root, "build1").apply { mkdirs() }
        val build2 = File(build1, "build2").apply { mkdirs() }
        createSettingsFile(
            rootDir = build1,
            contents = """
                rootProject.name = 'blah'
                includeBuild 'build2'
                include 'app'
                include 'library'
            """.trimIndent()
        )

        createModule(
            rootDir = build1,
            name = "app",
            contents =
            """
                plugins {
                    id 'application'
                }
                
                dependencies {
                    implementation(':library')
                }
                """.trimIndent()
        )

        createModule(
            rootDir = build1,
            name = "library",
            contents =
            """
                plugins {
                    id 'java'
                }
                """.trimIndent()
        )

        createSettingsFile(
            rootDir = build2,
            contents = """
                rootProject.name = 'blah2'
                include 'foobar'
            """.trimIndent()
        )

        createModule(
            rootDir = build2,
            name = "foobar",
            contents =
            """
                plugins {
                    id 'java'
                }
                """.trimIndent()
        )

        val analyzer = CoreAnalyzer(
            CoreOptions(
                directory = build1.toPath(),
                changedFiles = listOf("library/build.gradle"),
                useIncludeBuild = false
            )
        )

        val result = analyzer.analyze()

        assertContentEquals(listOf("app", "library"), result.projectMap.keys)
        assertContentEquals(listOf("library"), result.affectedResults.flatMap { it.affectedProjectPaths }.distinct())
    }

    @Test
    fun `Included builds with dependency substitution test`() = runTest(timeout = 3.minutes) {
        // Prep
        val build1 = File(root, "build1").apply { mkdirs() }
        val build2 = File(build1, "build2").apply { mkdirs() }
        createSettingsFile(
            rootDir = build1,
            contents = """
                rootProject.name = 'blah'
                includeBuild ('build2') {
                    dependencySubstitution {
                        substitute module('com.squareup:blah') using project(':foobar')
                    }
                }
                include 'app'
                include 'library'
            """.trimIndent()
        )

        createModule(
            rootDir = build1,
            name = "app",
            contents =
            """
                plugins {
                    id 'application'
                }
                
                dependencies {
                    implementation(':library')
                    implementation('com.squareup:blah:0.0.1')
                }
                """.trimIndent()
        )

        createModule(
            rootDir = build1,
            name = "library",
            contents =
            """
                plugins {
                    id 'java'
                }
                """.trimIndent()
        )

        createSettingsFile(
            rootDir = build2,
            contents = """
                rootProject.name = 'blah2'
                include 'foobar'
            """.trimIndent()
        )

        createModule(
            rootDir = build2,
            name = "foobar",
            contents =
            """
                plugins {
                    id 'java'
                }
                """.trimIndent()
        )

        val analyzer = CoreAnalyzer(
            CoreOptions(
                directory = build1.toPath(),
                changedFiles = listOf("build2/foobar/build.gradle")
            )
        )

        val result = analyzer.analyze()

        assertContentEquals(listOf("build2/foobar", "app", "library"), result.projectMap.keys)
        assertContentEquals(listOf("build2/foobar"), result.affectedResults.flatMap { it.affectedProjectPaths }.distinct())
    }

    private fun createSettingsFile(rootDir: File, contents: String) {
        File(rootDir, "settings.gradle").apply {
            createNewFile()
            writeText(contents)
        }
    }

    private fun createModule(rootDir: File, name: String, contents: String): File {
        return File(rootDir, name).apply {
            mkdirs()
            File(this, "build.gradle").apply {
                createNewFile()
                writeText(contents)
            }
        }
    }
}
