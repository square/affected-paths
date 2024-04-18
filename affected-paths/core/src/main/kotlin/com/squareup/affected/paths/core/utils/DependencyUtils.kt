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

package com.squareup.affected.paths.core.utils

import com.squareup.affected.paths.core.models.AffectedResult
import com.squareup.tooling.models.SquareProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/*
 * Finds all the affected paths of the list of Square Projects with the given list of changed files
 */
internal suspend fun findAffectedPaths(
  projectList: List<SquareProject>,
  changedFiles: List<String>
): List<AffectedResult> {
  return coroutineScope {

    val slices = async(Dispatchers.Default) { projectList.getReverseDependencies() }
    val filesToDocs = async(Dispatchers.Default) {
      filesToProjects(changedFiles, projectList.associateBy { it.pathToProject })
    }
    return@coroutineScope findAffectedAddresses(slices.await(), filesToDocs.await())
  }
}

/*
 * Creates the "slices" of the different variants. Every slice contains a map of "edges" that is
 * essentially a reverse dependency index of all the projects. Each key is a project that
 * contains a set of all other projects that depend on the key project.
 */

// Ensure that the data sets are modified by a single thread to avoid concurrency issues
private suspend fun List<SquareProject>.getReverseDependencies()
  : Map<String, Map<String, Set<String>>> {
  val slices = hashMapOf<String, MutableMap<String, MutableSet<String>>>()

  channelFlow {
    forEach { doc ->
      doc.variants.forEach { (variantName, variant) ->
        // Offload the extra iterations to another coroutine
        launch(Dispatchers.Default) {
          variant.deps.forEach variant@{ dep ->
            if (dep.target.startsWith('/')) {
              // Paths don't start with a forward slash
              val target = dep.target.trim('/')
              if (target != doc.pathToProject) {
                send(Triple(variantName, target, doc.pathToProject))
              }
            }
          }
        }

        // Offload the extra iterations to another coroutine
        launch(Dispatchers.Default) {
          variant.tests.forEach { (testName, testConfig) ->
            val address = "${doc.pathToProject}:$variantName:$testName"
            testConfig.deps.forEach tests@{ dep ->
              if (dep.target.startsWith('/')) {
                // Paths don't start with a forward slash
                val target = dep.target.trim('/')
                if (target != address) {
                  send(Triple(variantName, target, address))
                }
              }
            }
          }
        }
      }
    }
  }
    .flowOn(Dispatchers.Default) // Flow on a worker thread
    .collect { (variantName, target, address) ->
      val slice = slices.getOrPut(variantName) { hashMapOf() }
      val edge = slice.getOrPut(target) { hashSetOf() }
      edge.add(address)
    }

  return slices
}

/*
 * Maps all files to the projects that contain the given files
 */
private fun filesToProjects(
  changedFiles: List<String>,
  projects: Map<String, SquareProject>
): Map<String, Set<SquareProject>> {
  val fileToDocsMap = hashMapOf<String, MutableSet<SquareProject>>()
  val list = changedFiles.mapNotNull { file ->
    val modulePath = arrayListOf<String>()
    val docSet = file.trim('/').split('/').mapNotNull docSet@ { element ->
      modulePath.add(element)
      return@docSet projects[modulePath.joinToString("/")]
    }
    val doc = docSet.lastOrNull() ?: return@mapNotNull null
    return@mapNotNull file to doc
  }

  list.forEach { (key, doc) ->
    val set = fileToDocsMap.getOrPut(key) { hashSetOf() }
    set.add(doc)
  }

  return fileToDocsMap
}

/*
 * Finds all paths/addresses of modules affected by the given list of files
 */
private fun findAffectedAddresses(
  slices: Map<String, Map<String, Set<String>>>,
  filesToDocs: Map<String, Set<SquareProject>>
): List<AffectedResult> {
  // Flatten out the file, doc, and slice associated to each other
  return filesToDocs.asSequence().flatMap { (file, docs) ->
    docs.asSequence().flatMap { doc ->
      doc.variants.asSequence().flatMap top@{ (variantName, variant) ->
        val slice = slices[variantName].orEmpty()
        // If there is a change on the regular source files, pass down both test and variant info
        if (variant.srcs.any { file.contains(it) }) {
          val variantResult = Triple(file, doc.pathToProject, slice to variantName)
          return@top variant.tests.keys.asSequence().map { testName ->
            return@map Triple(
              file,
              "${doc.pathToProject}:$variantName:$testName",
              slice to variantName
            )
          } + variantResult
        }
        // Only pass down test configuration if only tests are affected
        else if (variant.tests.values.flatMap { it.srcs }.any { file.contains(it) }) {
          return@top variant.tests.keys.asSequence().map { testName ->
            return@map Triple(
              file,
              "${doc.pathToProject}:$variantName:$testName",
              slice to variantName
            )
          }
        } else {
          // Non source file changed
          return@top sequenceOf(Triple(file, doc.pathToProject, slice to variantName))
        }
      }
    }
  }.map { (file, address, sliceVariant) ->
    // start: Find all affected project paths
    val affected = hashSetOf<String>()
    affected.add(address)

    val (slice, variant) = sliceVariant

    val seen = hashSetOf<String>()
    val queue = ArrayDeque<String>().apply { add(address) }
    while (queue.isNotEmpty()) {
      val current = queue.removeFirst()
      if (current in seen) continue
      else seen.add(current)
      affected.add(current)
      val deps = slice[current].orEmpty()
      queue.addAll(deps)
    }
    // end: Find all affected project paths

    return@map AffectedResult(affected, variant, file)
  }.toList()
}
