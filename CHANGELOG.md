## Unreleased

## v0.1.9
- Migrate from `*MetadataConfigurationName` to `*ConfigurationName` - Kotlin 2.3.x compatibility

## v0.1.7
- Align `affected-paths-core` version with a `tooling-support` that it uses

## v0.1.6
- `tooling-support-core`: Support for Gradle 9

## v0.1.5
- `tooling-support`: Support SqlDelight 2.x
- `affected-paths-core`: Minimum JDK version is now 17

## v0.1.4
- `affected-paths-core`: Add in flag for enabling build scans on affected-paths runs
- `affected-paths-core`: Allow better control of the Gradle daemon

## v0.1.3
- `affected-paths-core`: Fix custom Gradle flags not being properly set
- `tooling-support-android`: Evaluate projects with `com.android.test` plugin

## v0.1.2
- `affected-paths-core`, `tooling-support-*`: Add in support for composite builds being analyzed
- `affected-paths-core`: Remove filter of root project
- `affected-paths-core`: Fix improper project mapping for file changes in nested projects
- `affected-paths-core`: Allow a custom Gradle installation path to be passed in to the Gradle Tooling API

## v0.1.1
- `tooling-support`: Fix crash from `SquareProjectModelBuilder` when used on a non-Java/Android project
- `affected-paths-core`: Adds `autoInjectPlugin` flag to `CoreOptions`, which auto-injects the "com.squareup.tooling" plugin to the build

## v0.1.0
- Initial public release

## v0.0.3
- `tooling-support-core`: Close ServiceLoader stream once all extractors are found
- `tooling-support`: Close ServiceLoader stream once all extractors are found
- `affected-paths-core`: Allow for custom changed files to be analyzed

## v0.0.2
- `tooling-support-core`: Fix import statements
- `tooling-support`: Fix ServiceLoader not loading `SquareProjectExtractor` implementations
- `affected-paths-core`: Allow for SquareProject list to be provided to `CoreAnalyzer.analyze()`

## v0.0.1
- Initial Release
