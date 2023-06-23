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

import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertContains

class SquareGitTest {

  @TempDir
  lateinit var tempFile: File

  lateinit var git: Git

  @BeforeTest
  fun setup() {
    git = Git.init().setDirectory(tempFile).call()
    git.commit().setAll(true).setMessage("Initial commit").call()
  }

  @AfterTest
  fun tearDown() {
    git.close()
    tempFile.deleteRecursively()
  }

  @Test
  fun `SquareGit detects changed files`() = runBlocking {
    // Setup
    File(tempFile, "foo.txt").apply { createNewFile() }
    git.add().addFilepattern(".").call()
    git.commit().setAll(true).setMessage("Second commmit").call()

    // Test
    val squareGit = SquareGitImpl(tempFile.toPath(), "")
    val changedFiles = squareGit.findChangedFiles()

    // Result
    assertContains(changedFiles, "foo.txt")
  }
}
