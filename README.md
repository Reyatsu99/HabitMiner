# HabitMiner 📱🧠

**A Context-Aware Digital Habit & Routine Tracker for Android**

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)  
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple.svg)](https://kotlinlang.org/)  
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)

---

## 🎯 What is HabitMiner?
HabitMiner is an offline-first Android application designed to discover and analyze your digital habits by fusing app usage statistics with physical context sensors. 

Instead of relying on battery-draining GPS tracking or microphone recording, the modern HabitMiner engine uses a lightweight combination of:
1. **App Usage Stats**: Digital behavior via `UsageStatsManager` (Duration, Session counts).
2. **Device State**: Screen-on/unlock events and Battery state.
3. **Physical Context**: Activity Recognition (Still, Walking, Active) and Ambient Light (Lux).
4. **Notifications**: (Optional) Tracks notification volumes to gauge digital interruptions.

The on-device **HabitEngine** aggregates this data to build temporal baselines (e.g., Weekday Mornings vs. Weekend Nights), discovers frequent app sequences (e.g., `Instagram → YouTube → Browser`), and detects anomalies or **Deviations** when your digital routine changes significantly.

---

## ✨ Features

- **Dynamic App Identity**: Resolves package names to human-readable labels and intelligently filters out Launcher/Home apps from your usage statistics.
- **Predictive Modeling**: Calculates a predictability score and guesses your next likely app based on your current routine and time of day.
- **Deviation Detection**: Alerts you to unusual screen time, temporal shifts in app usage, or missing routines by calculating Z-scores against your historical baseline.
- **100% Offline & Private**: All data collection and machine learning happens on-device using Room Database. No data is sent to the cloud.
- **Battery Efficient**: Relies on `WorkManager` for periodic background collection, ensuring minimal impact on battery life.

---

## 🏗️ Modern Android Architecture (2026)

HabitMiner has been completely refactored from the ground up to adhere to modern 2026 Android development best practices:

- **UI Layer**: Built entirely with **Jetpack Compose** and Material Design 3.
- **State Management**: **Unidirectional Data Flow (UDF)** using `StateFlow` and `@HiltViewModel`.
- **Dependency Injection**: Fully integrated with **Dagger Hilt** to eliminate manual dependency passing and static singletons.
- **Data Layer**: **Repository Pattern** backed by **Room 2.6.1** with KSP for robust local persistence.
- **Background Tasks**: Managed via **WorkManager 2.9.0**.
- **Code Quality**: Strict formatting enforced via **ktlint** and Spotless.

```text
+---------------------+        +-------------------------+        +--------------------------+
| UI Layer (Compose)  |  <--   | ViewModel (StateFlow)   |  <--   | Domain Layer (Engines)   |
| - HomeScreen        |        | - HabitViewModel        |        | - HabitEngine            |
| - HabitsScreen      |        +-------------------------+        | - PredictionEngine       |
| - InsightsScreen    |                                           | - DeviationDetector      |
+---------------------+                                           +--------------------------+
                                                                             ^
                                                                             |
                                                                  +--------------------------+
                                                                  | Data Layer (Repository)  |
                                                                  | - HabitRepository        |
                                                                  | - ContextRepository      |
                                                                  +--------------------------+
                                                                             ^
                                                                             |
                                                                  +--------------------------+
                                                                  | Room Database            |
                                                                  | - AppUsageEntity         |
                                                                  | - DiscoveredHabitEntity  |
                                                                  +--------------------------+
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (JDK 17 or newer).
- A physical Android device (API 26+). *Emulators may not generate realistic usage stats or sensor data.*

### Build & Run
```bash
cd android
./gradlew spotlessApply test assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Permissions Required
The app requires the following permissions to function fully:
- **Usage Access**: To read app usage statistics.
- **Physical Activity**: To detect your current motion state.
- **Notifications**: To monitor digital interruptions.

The app provides a seamless onboarding flow to grant these permissions upon first launch.

---

## 📄 License
Distributed under the MIT License.
