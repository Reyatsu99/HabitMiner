# HabitMiner: Android Prototype (Phase 2) 📱

The **Android Prototype** implements on-device, privacy-preserving routine extraction and replay simulation using Kotlin, Room Persistence Library, and WorkManager.

---

## Planned Module Structure

```
android/
├── README.md               # Android Build & Replay Guide
├── app/
│   ├── src/main/java/com/habitminer/
│   │   ├── data/           # Room DB entities, DAOs, and Repositories
│   │   ├── domain/         # StayPoint, ST-DBSCAN & Markov Kotlin engines
│   │   ├── service/        # Adaptive Location Collector & Replay Manager
│   │   ├── worker/         # WorkManager background periodic jobs
│   │   └── ui/             # Jetpack Compose / Dashboard ViewModels
│   └── build.gradle.kts
└── build.gradle.kts
```

---

## Technical Specifications

- **Language**: Kotlin 1.9+
- **Min SDK**: API Level 26 (Android 8.0 Oreo)
- **Target SDK**: API Level 34 (Android 14)
- **Persistence**: Room Database SQLite
- **Background Jobs**: AndroidX WorkManager
- **Location API**: Google Play Services Fused Location Provider
