# Affected-Paths

Affected-Paths is a Java library that utilizes the Gradle Tooling API to parse Gradle based projects and identifies all modules affected
(directly and indirectly) given the file changes from git.

## Quick Demo

To get started quickly with the demo app, first build the distribution of it:
```shell
./gradlew :affected-paths:app:installDist
```

Once the distribution has been built, the demo app can be run on a given project by calling:
```shell
./affected-paths/app/build/install/affected-paths/bin/affected-paths --log-gradle --inject-plugin --dir=/path/to/project
```

**Note:** Affected-Paths will only work on:
- Projects that are version controlled **AND**
- Contain at least 1 JVM or Android module (apply either `java` or `android-*` plugins)

This should output something similar to this:
```text
Changed file: app/src/main/kotlin/com/example/Main.kt
Projects affected by this changed file:
    - :app
    - :app:debug:debugAndroidTest
    - :app:debug:debugUnitTest
    - :app:release:releaseUnitTest


Changed file: library/src/main/kotlin/com/example/Library.kt
Projects affected by this changed file:
    - :app
    - :app:debug:debugAndroidTest
    - :app:debug:debugUnitTest
    - :app:release:releaseUnitTest
    - :library
    - :library:debug:debugAndroidTest
    - :library:debug:debugUnitTest
    - :library:release:releaseUnitTest

```

## Usage
The affected-paths library can be found on [MavenCentral][1]:

### Gradle
Groovy
```groovy
implementation 'com.squareup.affected.paths:affected-paths-core:0.1.0'
```

Kotlin
```kotlin
implementation("com.squareup.affected.paths:affected-paths-core:0.1.0")
```

### Maven
```xml
<dependency>
  <groupId>com.squareup.affected.paths</groupId>
  <artifactId>affected-paths-core</artifactId>
  <version>0.1.0</version>
</dependency>
```

**NOTE:** The affected-paths library is a Kotlin first project, so it uses [Kotlin Coroutines][2].

A simple use case that outputs all affected project paths:

```kotlin
val coreAnalyzer = CoreAnalyzer()

suspend fun getSquareProjects(): List<String> {
    // Performs analysis of files changed between HEAD and the previous commit
    val analysisResult = coreAnalyzer.analyze()
    
    // Flattens each project path found in each result
    return analysisResult.affectedResults.flatMap { it.affectedProjectPaths }
}
```

The `CoreAnalyzer` class can be passed a `CoreOptions` object, that provides extra configuration for analysis:

```kotlin
import kotlin.io.path.Path

val coreAnalyzer = CoreAnalyzer(
    coreOptions = CoreOptions(
        // Output all the Gradle logs
        logGradle = true,

        // If the JVM is run from a different directory, pass in the project path
        directory = Path("path/to/project"),

        // The SHA-1 hash of the commit to compare against the current HEAD
        comparisonCommit = "abcd1234"
        
        // Alternatively, a list of files can be passed to be used for analysis
        // changedFiles = list("file1.kt", "path/to/file2.kt")
    )
)
```

## How this works

Internally, the affected-paths library uses JGit to find the files changed between commits, and the Gradle Tooling API to configure and gather the `SquareProject` models, which are then analyzed by the `CoreAnalyzer` to provide the `AnalysisResult`.

The Gradle Tooling API cannot normally gather the `SquareProject` models, unless a [`ToolingModelBuilder`][3] (which defines how to construct the models) is registered on each module. A tooling plugin that registers the `SquareProjectModelBuilder` is automatically applied by `CoreAnalyzer` on all projects during the analysis, but can be disabled from `CoreOptions` as follows:

```kotlin
val analyzer = CoreAnalyzer(
    options = CoreOptions(
        // Disable auto-injecting the tooling plugin
        autoInjectPlugin = false
    )
)
```

If the auto-inject flag is disabled, the tooling plugin will have to be applied manually for each project that should be analyzed:

Gradle DSL
```groovy
plugins {
  id 'com.squareup.tooling' version '0.1.0'
}
```

Legacy
```groovy
buildscript {
  dependencies {
    classpath "com.squareup.affected.paths:tooling-support:0.1.0"
  }
}

apply plugin: "com.squareup.tooling"
```

**WARNING:** Do not apply the plugin if the auto-inject flag is enabled. The analysis will fail due to the same plugin being applied twice.

## License
```
   Copyright (c) 2023 Square, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```

[1]:https://search.maven.org/artifact/com.squareup.affected.paths/affected-paths-core
[2]:https://github.com/Kotlin/kotlinx.coroutines
[3]:https://docs.gradle.org/current/javadoc/org/gradle/tooling/provider/model/ToolingModelBuilder.html