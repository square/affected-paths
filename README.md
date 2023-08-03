# Affected-Paths

Affected-Paths is a Gradle specific tool that allows parsing Gradle builds and identifies all projects affected
(directly and indirectly) given a list of file changes between two git commits.

## Getting Started

In order to use the `affected-paths`, the `tooling-support` Gradle plugin must be applied to every project `build.gradle`.

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

This plugin provides a [ToolingModelBuilder][1] that allows a JVM app using the `affected-paths` library to analyze the project.

## Demo

Once the above plugin has been applied, the following command can be called to run the demo app that will print an example analysis output:

```shell
./gradlew :affected-paths:app:run --args="--dir=/path/to/project"
```

The output from this demo will display the list of files changed from the project's current and previous commits, as well as all the projects, variants, and tests affected by these files.

## Affected-Paths Core Usage

With this plugin, the `affected-paths` core library can be used to query and analyze all projects and determine if they are affected given the
git diff between `HEAD` and the commit hash passed in. An example of how to use this core library can be found in [BaseCommand.kt][2].

The `affected-paths` core library can be imported as follows:

```groovy
implementation 'com.squareup.affected.paths:affected-paths-core:0.1.0'
implementation 'com.squareup.affected.paths:tooling-models:0.1.0' // Also needed for interacting with models from core
```

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

[1]:https://docs.gradle.org/current/javadoc/org/gradle/tooling/provider/model/ToolingModelBuilder.html
[2]:affected-paths/app/src/main/kotlin/com/squareup/affected/paths/app/commands/BaseCommand.kt
