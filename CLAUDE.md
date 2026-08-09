# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Mixtape Haven is an Android application built with Kotlin and Jetpack Compose. The project uses modern Android development practices with a Material 3 design system.

**Package:** `pe.net.libre.mixtapehaven`
**Min SDK:** 33
**Target SDK:** 36
**Compile SDK:** 37
**Java Version:** 11

## Build Commands

### Build the project
```bash
./gradlew build
```

### Run tests
```bash
# Unit tests
./gradlew test

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

### Install debug build on device
```bash
./gradlew installDebug
```

### Clean build
```bash
./gradlew clean
```

### Run specific test
```bash
# Unit test — filter on the variant task; the aggregate `test` task rejects --tests
./gradlew testDebugUnitTest --tests pe.net.libre.mixtapehaven.ExampleUnitTest

# Instrumented test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=pe.net.libre.mixtapehaven.ExampleInstrumentedTest
```

### Lint and code quality
```bash
./gradlew lint
```

### Debug signing
Debug builds are signed with a shared keystore so CI-built and locally built APKs can replace
each other on a device. The keystore is not in the repo: CI decodes it from the
`DEBUG_KEYSTORE_BASE64` secret, and local builds resolve it from `MIXTAPE_DEBUG_KEYSTORE` or the
`mixtape.debug.keystore` Gradle property. When unset the build falls back to AGP's per-machine
key and warns. See README.md > Debug signing.

## Architecture

### UI Layer
- **Compose-based UI:** The app uses Jetpack Compose with Material 3 for all UI components
- **Single Activity:** `MainActivity` serves as the entry point with `enableEdgeToEdge()` enabled
- **Theme System:** Custom theme defined in `ui/theme/` with `MixtapeHavenTheme`, `Color.kt`, and `Type.kt`

### Key Structure
- **Main Activity:** `MainActivity.kt` - Entry point using ComponentActivity with Compose
- **UI Theme:** Located in `app/src/main/java/pe/net/libre/mixtapehaven/ui/theme/`
  - `Theme.kt` - Main theme configuration
  - `Color.kt` - Color definitions
  - `Type.kt` - Typography definitions

### Dependencies
Dependencies are managed via Gradle version catalog in `gradle/libs.versions.toml`. Key dependencies:
- Jetpack Compose BOM (2026.06.01)
- Material 3
- Lifecycle Runtime KTX
- Activity Compose
- Testing: JUnit, Espresso, Compose UI Test

## Development Notes

### Gradle Configuration
- Uses Kotlin DSL (`.gradle.kts`) for build files
- Version catalog system for dependency management
- Repository configuration in `settings.gradle.kts` restricts to Google, Maven Central, and Gradle Plugin Portal

### Code Organization
- Main source: `app/src/main/java/pe/net/libre/mixtapehaven/`
- Test source: `app/src/test/java/pe/net/libre/mixtapehaven/`
- Instrumented tests: `app/src/androidTest/java/pe/net/libre/mixtapehaven/`
- Resources: `app/src/main/res/`
