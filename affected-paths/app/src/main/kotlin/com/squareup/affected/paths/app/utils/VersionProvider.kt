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

package com.squareup.affected.paths.app.utils

import picocli.CommandLine

/**
 * Provides the version information for all commands
 */
internal class VersionProvider : CommandLine.IVersionProvider {
  override fun getVersion(): Array<String> {
    return arrayOf(
      "affected-paths: ${javaClass.`package`.implementationVersion}".colorized(Color.YELLOW),
      "Picocli: ${CommandLine.VERSION}".colorized(Color.RED),
      "JVM: \${java.version} (\${java.vendor} \${java.vm.name} \${java.vm.version})".colorized(Color.BLUE),
      "OS: \${os.name} \${os.version} \${os.arch}".colorized(Color.GREEN)
    )
  }
}

private enum class Color {
  YELLOW, BLUE, RED, GREEN
}

private fun String.colorized(color: Color): String {
  return "@|${color.name.lowercase()} $this|@"
}
