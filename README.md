# Mixtape Haven

A modern Android music player application built with Kotlin and Jetpack Compose, featuring a beautiful Material 3 design system.

## Overview

Mixtape Haven is a native Android music streaming application that provides a seamless music listening experience with an intuitive interface and powerful playback capabilities.

## Features

- **Music Playback** - High-quality audio playback powered by Media3 ExoPlayer
- **Library Management** - Browse your music by songs, albums, and artists
- **Now Playing** - Full-featured now playing screen with playback controls
- **Material 3 Design** - Modern UI following Material Design 3 guidelines
- **Background Playback** - Continue listening while using other apps
- **Network Integration** - Stream music from remote sources

## Screenshots

_Coming soon_

## Tech Stack

### Architecture & UI
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Design System:** Material 3
- **Architecture Pattern:** MVVM (Model-View-ViewModel)
- **Navigation:** Jetpack Navigation Compose

### Key Libraries
- **Media Playback:** AndroidX Media3 (ExoPlayer)
- **Networking:** Retrofit 3.0 + OkHttp 5.3
- **Serialization:** Kotlinx Serialization
- **Image Loading:** Coil 2.7
- **Data Storage:** DataStore Preferences
- **Dependency Injection:** Manual DI

### Development
- **Min SDK:** 33 (Android 13)
- **Target SDK:** 36
- **Compile SDK:** 36
- **Java Version:** 11
- **Build System:** Gradle (Kotlin DSL)

## Requirements

- Android Studio Ladybug | 2024.2.1 or newer
- Android SDK 36
- JDK 11 or higher
- Android device or emulator running Android 13 (API 33) or higher

## Getting Started

### Clone the Repository

```bash
git clone https://github.com/yourusername/mixtapehaven-android.git
cd mixtapehaven-android
```

### Open in Android Studio

1. Open Android Studio
2. Select **File > Open**
3. Navigate to the cloned repository
4. Click **OK**

### Build the Project

```bash
./gradlew build
```

### Run the App

Connect an Android device or start an emulator, then:

```bash
./gradlew installDebug
```

Or use the **Run** button in Android Studio.

## Build Commands

### Testing

```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Run specific test
./gradlew test --tests pe.net.libre.mixtapehaven.ExampleUnitTest
```

### Code Quality

```bash
# Run lint checks
./gradlew lint

# Clean build
./gradlew clean
```

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/pe/net/libre/mixtapehaven/
│   │   │   ├── data/              # Data layer (repositories, services)
│   │   │   │   └── playback/      # Media playback service
│   │   │   ├── ui/                # UI layer
│   │   │   │   ├── theme/         # Theme configuration
│   │   │   │   ├── navigation/    # Navigation setup
│   │   │   │   ├── home/          # Home screen & components
│   │   │   │   ├── nowplaying/    # Now playing screen
│   │   │   │   ├── onboarding/    # Onboarding flow
│   │   │   │   └── troubleshoot/  # Troubleshooting screen
│   │   │   └── MainActivity.kt    # App entry point
│   │   └── res/                   # Resources (layouts, strings, etc.)
│   ├── test/                      # Unit tests
│   └── androidTest/               # Instrumented tests
└── build.gradle.kts               # App-level build configuration

gradle/
└── libs.versions.toml             # Version catalog for dependencies

CLAUDE.md                          # Project guidance for Claude Code
README.md                          # This file
```

## Architecture

Mixtape Haven follows modern Android development practices:

### UI Layer
- **Jetpack Compose** for declarative UI
- **Material 3** components and theming
- **ViewModels** for UI state management
- **Navigation Compose** for app navigation

### Data Layer
- **Retrofit** for network requests
- **DataStore** for local preferences
- **Media3 ExoPlayer** for audio playback
- **MediaPlaybackService** for background playback

### Design Patterns
- Single Activity architecture
- Unidirectional data flow
- Repository pattern (where applicable)
- Separation of concerns

## Permissions

The app requires the following permissions:
- `INTERNET` - Stream music from remote sources
- `ACCESS_NETWORK_STATE` - Check network connectivity
- `FOREGROUND_SERVICE` - Background music playback
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK` - Media playback notifications
- `POST_NOTIFICATIONS` - Display playback notifications
- `WAKE_LOCK` - Keep device awake during playback

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style
- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Write unit tests for new features
- Keep Compose components small and focused

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Built with [Jetpack Compose](https://developer.android.com/jetpack/compose)
- Media playback powered by [Media3 ExoPlayer](https://developer.android.com/media/media3)
- Networking with [Retrofit](https://square.github.io/retrofit/)
- Image loading with [Coil](https://coil-kt.github.io/coil/)

## Support

For issues, questions, or suggestions, please [open an issue](https://github.com/yourusername/mixtapehaven-android/issues) on GitHub.

---

Made with ❤️ using Kotlin and Jetpack Compose
