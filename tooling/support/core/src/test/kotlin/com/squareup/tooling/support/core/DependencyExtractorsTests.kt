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

package com.squareup.tooling.support.core

import com.squareup.tooling.support.core.extractors.extractDependencies
import com.squareup.tooling.support.core.extractors.extractResolvedProjectDependencies
import com.squareup.tooling.support.core.extractors.extractSquareDependency
import com.squareup.tooling.support.core.models.SquareDependency
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DependencyExtractorsTests {

  @Test
  fun `Test extractDependencies() extension`() {
    // Setup
    val project = ProjectBuilder.builder().build()
    project.configurations.create("testConfig")
    project.dependencies.add("testConfig", "com.squareup:foo")
    project.dependencies.add("testConfig", "com.squareup:bar")

    val result = project.configurations.getByName("testConfig").extractDependencies().toList()

    // Test
    assertEquals(2, result.size)
    assertTrue(result.map { it.name }.containsAll(listOf("foo", "bar")))
  }

  @Test
  fun `Test extractSquareDependency() extension with AbstractExternalModuleDependency`() {
    // Setup
    val project = ProjectBuilder.builder().build()
    project.configurations.create("testConfig")
    project.dependencies.add("testConfig", "com.squareup:foo")

    // Test
    val configuration = project.configurations.getByName("testConfig")

    val result = configuration.dependencies.map { it.extractSquareDependency(project) }.first()

    assertTrue("AbstractExternalModuleDependency should not set tags") {
      return@assertTrue result.tags.isEmpty()
    }
    assertTrue("AbstractExternalModuleDependency target incorrect") {
      return@assertTrue result.target ==
        "@maven://com.squareup:foo"
    }
  }

  @Test
  fun `Test extractSquareDependency() with AbstractExternalModuleDependency undefined group`() {
    // Setup
    val project = ProjectBuilder.builder().build()
    project.configurations.create("testConfig")
    project.dependencies.add("testConfig", ":foo")

    // Test
    val configuration = project.configurations.getByName("testConfig")

    val result = configuration.dependencies.map { it.extractSquareDependency(project) }.first()

    assertTrue("AbstractExternalModuleDependency should not set tags") {
      return@assertTrue result.tags.isEmpty()
    }
    println(result)
    assertTrue("AbstractExternalModuleDependency target incorrect") {
      return@assertTrue result.target == "@maven://undefined:foo"
    }
  }

  @Test
  fun `Test extractSquareDependency() extension with AbstractModuleDependency`() {
    // Setup
    val project = ProjectBuilder.builder().build()
    val projectDependency = ProjectBuilder.builder().withName("squareTest").withParent(project).build()
    project.configurations.create("testConfig")
    project.dependencies.add("testConfig", projectDependency)

    // Test
    val configuration = project.configurations.getByName("testConfig")

    val result = configuration.dependencies.map { it.extractSquareDependency(project) }.first()

    assertEquals(
      expected = 1,
      actual = result.tags.size,
      message = "Transitive tag not applied"
    )
    println(result)
    assertTrue("AbstractModuleDependency target incorrect") {
      return@assertTrue result.target == "/squareTest"
    }
  }

  @Test
  fun `test extractResolvedProjectDependencies() with empty project dependencies`() {
    // Setup
    val project = ProjectBuilder.builder().build()
    project.configurations.create("testConfig")

    // Test
    val configuration = project.configurations.getByName("testConfig")

    val result = configuration.extractResolvedProjectDependencies()
    assertTrue(result.none(), "The result should be an empty sequence")
  }

  @Test
  fun `test extractResolvedProjectDependencies() with resolved project dependencies`() {
    // Setup
    val project = ProjectBuilder.builder().build()
    project.configurations.create("testConfig")
    val projectDependency = ProjectBuilder.builder().withName("squareTest").withParent(project).build()
    projectDependency.configurations.create("default")
    project.dependencies.add("testConfig", projectDependency)

    // Test
    val configuration = project.configurations.getByName("testConfig")

    val result = configuration.extractResolvedProjectDependencies()
    assertEquals(1, result.count())
    assertTrue(result.contains(SquareDependency(target = "/squareTest")))
  }

}
