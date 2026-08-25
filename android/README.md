# HabitMiner: Android Data Logger (Phase 2) 📱

The **Android Data Logger** implements a robust, on-device, privacy-preserving location collection tool. Its primary responsibility is to log GPS and motion data efficiently using Android Activity Recognition, store it locally using the Room Persistence Library, and export it for offline analysis by the Python engine.

---

## Planned Module Structure

```
android/
├── README.md               # Android Build & Export Guide
├── app/
│   ├── src/main/java/com/habitminer/
│   │   ├── data/           # Room DB entities and DAOs for Raw GPS logs
│   │   ├── service/        # Activity-Aware Location Collector Service
│   │   ├── worker/         # WorkManager jobs for bulk export (JSON/SQLite)
│   │   └── ui/             # Simple Dashboard to Start/Stop logging and Export
│   └── build.gradle.kts
└── build.gradle.kts
```

---

## Technical Specifications

- **Language**: Kotlin 1.9+
- **Min SDK**: API Level 26 (Android 8.0 Oreo)
- **Target SDK**: API Level 34 (Android 14)
- **Persistence**: Room Database SQLite (SQLCipher for encryption)
- **Background Jobs**: AndroidX WorkManager (for JSON/SQLite exports)
- **Location API**: Google Play Services Fused Location Provider
- **Context API**: Google Play Services Activity Recognition API

---

## Scope Guardrails

> [!CAUTION]
> The Android app explicitly **DOES NOT** perform algorithmic clustering or Markov modeling on the device. Its sole purpose is highly efficient data collection and structured export for the Python Research Engine.
