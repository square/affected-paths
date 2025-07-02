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

package com.squareup.plugins

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.utils.notCompatibleWithConfigurationCacheCompat
import java.io.File

internal fun Project.configureDokka() {
  apply<DokkaPlugin>()
  tasks.getByName<DokkaTask>("dokkaHtml") {
    notCompatibleWithConfigurationCacheCompat("https://github.com/Kotlin/dokka/issues/1217")

    // All projects should have the library name to use set in the project's gradle.properties file
    moduleName.set(findProperty("POM_NAME").toString())
    dokkaSourceSets.maybeCreate("main").apply {
      outputDirectory.set(layout.buildDirectory.dir("dokka").get().asFile)
      reportUndocumented.set(true)
      platform.set(Platform.jvm)
      sourceRoots.setFrom(File("src/main"))
      jdkVersion.set(17)

      // Set Gradle links
      externalDocumentationLink("https://docs.gradle.org/${gradle.gradleVersion}/javadoc/")

      perPackageOption {
        matchingRegex.set("kotlin($|\\.).*")
        skipDeprecated.set(false)
        reportUndocumented.set(true)
        includeNonPublic.set(false)
      }
    }
  }
}

internal fun Project.configureJVM() {
  plugins.apply("org.jetbrains.kotlin.jvm")

  configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  configure<KotlinJvmProjectExtension> {
    explicitApi()
    jvmToolchain {
      languageVersion.set(JavaLanguageVersion.of(17))
    }
  }

  tasks.withType<Test> {
    useJUnitPlatform()

    jvmArgs(
      "--add-opens=java.base/java.lang=ALL-UNNAMED"
    )
  }
}
