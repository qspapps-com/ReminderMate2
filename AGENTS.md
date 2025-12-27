# AGENT.md: AI Coding Assistant Guide for ReminderMate

This document provides the necessary context for an AI assistant to understand and contribute to this project effectively. It outlines the technology stack, project architecture, coding conventions, and key commands.

## 1. Project Overview

*   **Project Name:** ReminderMate
*   **Description:** Reminder Mate is a simple productivity app to help users manage reminders.
*   **Core Functionality:**
    *   allows users to add reminders at specific time and get reminded through notifications.
    *   provides an easy way to snooze reminders or mark completed through notifications itself.
    *   allows users to set recurring reminders with complex recurrence patterns like weekdays, weekends, weekly, monthly.
    *   completely offline and does not share any user data.

## 2. Technology Stack & Frameworks

This project is a native Android application written in Kotlin, using the Jetpack Compose toolkit. The primary dependencies are listed below.

*   **Core:**
    *   **Language:** Kotlin
    *   **UI Toolkit:** Jetpack Compose
    *   **Build System:** Gradle with Kotlin DSL (`.kts`)

*   **Jetpack & AndroidX Libraries:**
    *   **Architecture Components:**
        *   `ViewModel`: For managing UI-related data in a lifecycle-conscious way.
        *   `Navigation Compose`: For handling in-app navigation between composable screens.
        *   `Lifecycle`: For observing Android lifecycles (`collectAsStateWithLifecycle`).
    *   **Asynchronous Programming:**
        *   `Kotlin Coroutines & Flow`: For managing background threads and handling asynchronous data streams.
    *   **Dependency Injection:**
        *   `Hilt`: For managing dependencies throughout the app. We use `@HiltViewModel` and `@AndroidEntryPoint`.
    *   **Data Persistence (Local):**
        *   `Room`: For storing reminders.
        *   `DataStore`: For storing user preferences and settings.
    *   **UI & Theming:**
        *   `Compose Material 3`: The primary design system for UI components.
        *   `Coil` : Coil for loading images from the network asynchronously.
    *   **Background Tasks:**
        *   `WorkManager`: For deferrable, guaranteed background execution (cleanup and daily checks).
        *   `Hilt Work`: For dependency injection into Worker classes.

*   **Networking:**
    *   `Retrofit`: For making type-safe HTTP requests to our REST API.
    *   `OkHttp`: As the underlying HTTP client for Retrofit (often used for interceptors).
    *   `Moshi` : Moshi for parsing JSON responses into Kotlin data classes.

*   **Testing:**
    *   `JUnit 4/5`: For unit testing ViewModels and Repositories.
    *   `MockK` : MockK for creating mocks in unit tests.
    *   `Turbine`: For testing Kotlin Flow emissions.
    *   `Compose Test Suite`: For UI and integration testing of composable functions.

## 3. Project Structure & Architecture

The project follows the **MVVM (Model-View-ViewModel)** architecture pattern, organized by feature.

```
com.qspapps.remindermate/
├── data/                  # Data layer implementation
│   ├── model/             # Data Transfer Objects (DTOs) and domain models
│   ├── local/             # Room DAO and database definitions
│   └── repository/        # Repository implementations
├── di/                    # Feature-specific or app-wide Hilt modules
├── notifications/         # Notification-related classes and services
├── ui/                    # Presentation layer (Jetpack Compose)
│   ├── navigation/        # Navigation graph and route definitions (e.g., AppNavigation.kt)
│   ├── theme/             # App theme, colors, typography (Theme.kt, Color.kt)
|   |── core/              # Reusable, generic composables
│   └── feature_name/      # A specific feature screen (e.g., home, profile)
│       ├── HomeScreen.kt  # The main Composable function for the screen
│       └── HomeViewModel.kt # The ViewModel for the screen
|──utils/                  # Utility functions and extensions
|──workers/                # All periodic and app related background tasks
│── MyApplication.kt       # Simple wrapper for Hilt entry point
└── MainActivity.kt        # The main entry point of the app
```

### Key Architectural Principles:

*   **View (Composables):** Reside in the `ui/feature_name` packages. They are responsible for displaying state and forwarding user events to the ViewModel. They should be as "dumb" as possible and observe state from a `StateFlow`.
*   **ViewModel:** Resides alongside its screen composable. It contains the business logic for the screen, exposes UI state via a `StateFlow<UiState>`, and is injected with repositories or use cases. All asynchronous work is launched in `viewModelScope`.
*   **Repository:** The single source of truth for data. It fetches data from remote (network) or local (database) sources and abstracts the data source from the ViewModel.
*   **Dependency Injection:** Hilt is used to provide dependencies. ViewModels are injected using `@HiltViewModel`, and dependencies like repositories are provided in Hilt Modules (`@Module`, `@Provides`).
*   **Background Maintenance:** All periodic tasks (cleaning old data, 6 AM overdue checks) must be implemented using `WorkManager`. Do not put this logic in `MainActivity`.
*   **Worker Initialization:** WorkManager uses custom initialization via `Configuration.Provider` in `MyApplication` to support Hilt injection.

## 4. Coding Style & Conventions

*   **Language:** Follow the official [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html).
*   **Compose Naming:**
    *   Composable functions are named in `PascalCase`, e.g., `UserProfileScreen`.
    *   Reusable, generic composables are in the `ui/core` package.
*   **ViewModel State Management:**
    *   Each ViewModel should expose a single `StateFlow` representing the UI state.
    *   A `data class` is used to define the state, e.g., `data class HomeUiState(...)`.
    *   The state is collected in the Composable using `viewModel.uiState.collectAsStateWithLifecycle()`.
*   **Immutability:** Prefer `val` over `var` and use immutable data structures (e.g., `List` instead of `MutableList`) for UI state to ensure unidirectional data flow.
*   **Coroutines:** Use structured concurrency. Launch coroutines from `viewModelScope`. Use `Dispatchers.IO` for network/database operations and `Dispatchers.Default` for CPU-intensive work.
*   Use DateTimeUtils for most of the date and time related functionality.

## 5. Setup & Build Instructions

This project uses the Gradle wrapper to ensure a consistent build environment.

**Prerequisites:**
*   Android Studio [e.g., Hedgehog or newer]
*   JDK 17 or higher

**Setup:**
1.  Clone the repository: `git clone git@github.com:qspapps-com/ReminderMate2.git`
2.  Open the project in Android Studio. It will automatically sync Gradle.

**Common Gradle Commands (run from the project root directory):**

*   **Clean the project:**
    ```bash
    ./gradlew clean
    ```
*   **Build a debug APK:**
    ```bash
    ./gradlew assembleDebug
    ```
*   **Build a release APK:**
    ```bash
    ./gradlew assembleRelease
    ```
*   **Install the debug app on a connected device/emulator:**
    ```bash
    ./gradlew installDebug
    ```
*   **Run all unit tests:**
    ```bash
    ./gradlew testDebugUnitTest
    ```
*   **Run all instrumented (UI) tests:**
    ```bash
    ./gradlew connectedDebugAndroidTest
    ```
*   **On Windows**, use `gradlew.bat` instead of `./gradlew`.

## 6. How to Add a New Feature (Screen)

Follow these steps to add a new feature, for example, a "Settings" screen:

1.  **Create Package:** Create a new package `ui.settings`.
2.  **Create ViewModel:** Create `SettingsViewModel.kt` inside `ui.settings`. Annotate it with `@HiltViewModel` and define its `SettingsUiState`.
3.  **Create Screen:** Create `SettingsScreen.kt`. Define the composable `SettingsScreen()` that takes the `SettingsViewModel` as a parameter (using `hiltViewModel()`).
4.  **Update Navigation:** Add a new route for the settings screen in your navigation graph file (e.g., `ui/navigation/AppNavigation.kt`).
5.  **Data Layer (if needed):** If the feature requires new data, add new functions to the relevant `Repository` and create any necessary `DAO` or network API endpoints.
6.  **Dependency Injection (if needed):** If you create new repositories or data sources, provide them in a Hilt module.
