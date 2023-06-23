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

import com.vanniktech.maven.publish.MavenPublishPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.plugin.devel.plugins.JavaGradlePluginPlugin

/*
 * Convention plugin for developing Gradle plugin projects
 */
class JavaGradlePlugin : Plugin<Project> {

  override fun apply(target: Project) {
    target.apply<KotlinLibPlugin>()
    target.configureJavaGradlePlugin()
    target.configureDokka()
  }
}

private fun Project.configureJavaGradlePlugin() {
  apply<JavaGradlePluginPlugin>()
  apply<MavenPublishPlugin>()
}
