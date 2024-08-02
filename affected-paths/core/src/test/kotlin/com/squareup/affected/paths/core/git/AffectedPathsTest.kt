package com.squareup.affected.paths.core.git

import com.squareup.affected.paths.core.CoreAnalyzer
import com.squareup.affected.paths.core.CoreOptions
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.time.Duration.Companion.minutes

class AffectedPathsTest {

    @TempDir
    lateinit var root: Path

    @Test
    fun `Can find affected project with single file change`() = runTest(timeout = 3.minutes) {
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
                    implementation project(':library')
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
                directory = root,
                changedFiles = listOf("app/build.gradle")
            )
        )

        val result = analyzer.analyze()

        // Results
        assertContentEquals(listOf("app", "library"), result.projectMap.keys)
        assertContentEquals(listOf("app"), result.affectedResults.flatMap { it.affectedProjectPaths }.distinct())

    }

    @Test
    fun `Can find projects from included builds by default`() = runTest(timeout = 3.minutes) {
        // Prep
        val build1 = root.resolve("build1").createDirectories()
        val build2 = build1.resolve("build2").createDirectories()
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
                    implementation project(':library')
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
                directory = build1,
                changedFiles = listOf("library/build.gradle")
            )
        )

        val result = analyzer.analyze()

        assertContentEquals(listOf("build2/foobar", "app", "library"), result.projectMap.keys)
        assertContentEquals(listOf("app", "app:debug:debugUnitTest", "library", "app:release:releaseUnitTest"), result.affectedResults.flatMap { it.affectedProjectPaths }.distinct())
    }

    @Test
    fun `Ignores projects in included builds when useIncludeBuild is false`() = runTest(timeout = 3.minutes) {
        // Prep
        val build1 = root.resolve("build1").createDirectories()
        val build2 = build1.resolve("build2").createDirectories()
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
                    implementation project(':library')
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
                directory = build1,
                changedFiles = listOf("library/build.gradle"),
                useIncludeBuild = false
            )
        )

        val result = analyzer.analyze()

        assertContentEquals(listOf("app", "library"), result.projectMap.keys)
        assertContentEquals(listOf("app", "app:debug:debugUnitTest", "library", "app:release:releaseUnitTest"), result.affectedResults.flatMap { it.affectedProjectPaths }.distinct())
    }

    @Test
    fun `Can find affected projects owned by included builds given a changed file`() = runTest(timeout = 3.minutes) {
        // Prep
        val build1 = root.resolve("build1").createDirectories()
        val build2 = build1.resolve("build2").createDirectories()
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
                    implementation project(':library')
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
                directory = build1,
                changedFiles = listOf("build2/foobar/build.gradle")
            )
        )

        val result = analyzer.analyze()

        assertContentEquals(listOf("build2/foobar", "app", "library"), result.projectMap.keys)
        assertContentEquals(
            listOf("app", "app:debug:debugUnitTest", "build2/foobar", "app:release:releaseUnitTest"),
            result.affectedResults.flatMap { it.affectedProjectPaths }.distinct()
        )
    }

    @Test
    fun `Proper project is mapped for nested projects`() = runTest(timeout = 3.minutes) {
        // Prep
        val build = root.resolve("build").createDirectories()
        createSettingsFile(
            rootDir = build,
            contents = """
                rootProject.name = 'blah'
                include ':app'
                include ':library'
                include ':library:foobar'
            """.trimIndent()
        )

        createModule(
            rootDir = build,
            name = "app",
            contents =
            """
                plugins {
                    id 'application'
                }
                
                dependencies {
                    implementation project(':library:foobar')
                    implementation('com.squareup:blah:0.0.1')
                }
                """.trimIndent()
        )

        createModule(
            rootDir = build,
            name = "library",
            contents =
            """
                plugins {
                    id 'java'
                }
                """.trimIndent()
        )

        createModule(
            rootDir = build,
            name = "library/foobar",
            contents =
            """
                plugins {
                    id 'java'
                }
                """.trimIndent()
        )

        val analyzer = CoreAnalyzer(
            CoreOptions(
                directory = build,
                changedFiles = listOf("library/foobar/build.gradle")
            )
        )

        val result = analyzer.analyze()

        assertContentEquals(listOf("app", "library", "library/foobar"), result.projectMap.keys)
        assertContentEquals(listOf("app", "app:debug:debugUnitTest", "library/foobar", "app:release:releaseUnitTest"), result.affectedResults.flatMap { it.affectedProjectPaths }.distinct())
    }

    private fun createSettingsFile(rootDir: Path, contents: String) {
        rootDir.resolve("settings.gradle").apply {
            writeText(contents)
        }
    }

    private fun createModule(rootDir: Path, name: String, contents: String): Path {
        return rootDir.resolve(name).createDirectories().apply {
            resolve("build.gradle").apply {
                writeText(contents)
            }
        }
    }
}
