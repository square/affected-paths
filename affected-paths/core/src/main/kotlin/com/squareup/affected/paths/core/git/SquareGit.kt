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

package com.squareup.affected.paths.core.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry.ChangeType.ADD
import org.eclipse.jgit.diff.DiffEntry.ChangeType.COPY
import org.eclipse.jgit.diff.DiffEntry.ChangeType.DELETE
import org.eclipse.jgit.diff.DiffEntry.ChangeType.MODIFY
import org.eclipse.jgit.diff.DiffEntry.ChangeType.RENAME
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.slf4j.LoggerFactory
import java.nio.file.Path

/**
 * Helper class to access git via JGit
 */
internal interface SquareGit {

  /**
   * Gets the name of the current branch, a sha-1 hash, or empty string
   *
   * @see org.eclipse.jgit.lib.Repository.getBranch
   */
  val currentBranch: String

  /**
   * Gets the sha-1 hash of HEAD
   */
  val currentCommit: String

  /**
   * Gets the comparison commit to compare the current commit against. Used by [findChangedFiles]
   */
  val comparisonCommit: String

  /**
   * Gets the names of the branches associated to the current comparison commit
   */
  val comparisonLabels: Set<String>

  /**
   * Returns the list of file paths (relative to the repository root) that have changed between the
   * head and the comparison commit
   *
   * Functionally similar to `git diff --name-only <COMMIT_HASH>`
   */
  suspend fun findChangedFiles(): List<String>
}

private val LOGGER = LoggerFactory.getLogger(SquareGit::class.java)

internal class SquareGitImpl(
  rootDir: Path,
  override val comparisonCommit: String
) : SquareGit {

  private val repository = FileRepositoryBuilder()
    .readEnvironment()
    .findGitDir(rootDir.toFile())
    .build()

  private val git = Git(repository)

  private val headId = repository.resolve(Constants.HEAD)

  // If comparisonCommit is empty, force it to be headId instead
  private val comparisonMergeBaseId = repository.resolve(comparisonCommit) ?: headId

  override val currentBranch: String
    get() = repository.branch ?: ""

  override val currentCommit: String
    get() = headId.name

  override val comparisonLabels: Set<String> by lazy {
    return@lazy git
      .nameRev()
      .addPrefix(Constants.R_HEADS)
      .add(comparisonMergeBaseId)
      .call()
      .asSequence()
      .map {
        LOGGER.debug("Found comparison label {}", it.value)
        return@map it.value
      }
      .filterNot {
        // Ignore relational branches (ex HEAD~1)
        it.contains("~") || it.contains("^")
      }
      .toSet()
  }

  override suspend fun findChangedFiles(): List<String> {
    // Running in IO since we are mostly reading from the file system
    return withContext(Dispatchers.IO) {
      LOGGER.debug("Got comparison labels: {}", comparisonLabels)

      LOGGER.info("Resolved headId={}", headId)

      LOGGER.info("Resolved comparisonMergeBaseId={}", comparisonMergeBaseId)

      LOGGER.debug("Current branch={}", currentBranch)

      // To prevent unnecessary work if the coroutine was cancelled
      ensureActive()

      val diffs = repository.newObjectReader().use { reader ->

        val oldTreeItr = if (headId == comparisonMergeBaseId) {
          LOGGER.debug("headId and comparisonMergeBaseId match. Iterating from last commit")
          val prevCommit = repository.resolve("HEAD~1^{tree}")
          if (prevCommit == null) {
            LOGGER.warn("No previous commit found")
            return@use emptyList()
          }
          CanonicalTreeParser().apply { reset(reader, prevCommit) }
        } else {
          CanonicalTreeParser().apply { reset(reader, comparisonMergeBaseId.treeId) }
        }

        return@use git.diff()
          .setOldTree(oldTreeItr)
          .setNewTree(CanonicalTreeParser().apply { reset(reader, headId.treeId) })
          .call()
      }

      return@withContext diffs.asSequence().flatMap {
        when (it.changeType) {
          ADD, COPY, MODIFY -> return@flatMap sequenceOf(it.newPath)
          RENAME -> return@flatMap sequenceOf(it.oldPath, it.newPath)
          DELETE -> return@flatMap sequenceOf(it.oldPath)
          null -> return@flatMap emptySequence()
        }
      }.distinct().toList()
    }
  }

  // Helper function to get the treeId of a commit object.
  private val ObjectId.treeId: ObjectId
    get() {
      return RevWalk(repository).use { walk ->
        val commit = walk.parseCommit(this)
        val tree = walk.parseTree(commit.tree.id)
        val id = tree.id
        walk.dispose()
        return@use id
      }
    }
}
