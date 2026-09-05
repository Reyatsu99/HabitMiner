# HabitMiner 📍🧠📱

**A Context‑Aware Mobile Framework for Daily Routine Discovery**

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)  
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-purple.svg)](https://kotlinlang.org/)  
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)

---

## 🎯 Goal
HabitMiner now goes beyond raw GPS. It fuses **four on‑device sensors** (Location, Ambient Audio, Light, Screen state) to infer high‑level daily habits such as:
- **Sleeping** (night, dark, quiet, screen off)
- **Deep Focus** (quiet, screen off)
- **Socializing / Active** (loud environments)
- **Phone Usage** (high screen‑on ratio)
- **Commuting** (gap between stay points)
- **Other** (fallback)

The Android app records this multi‑modal data, stores it in a Room database, and the built‑in **HabitEngine** class classifies the behavior into a **Behavioral Timeline** displayed in the Insights screen.

---

## 🏗️ Architecture
```
+-------------------+        +----------------------+        +-------------------+
| Android App       |  -->   | Room DB (habitminer) |  -->   | HabitEngine (Kotlin) |
| - Location (Fused) |       | - latitude            |       | - Detect stay points |
| - Audio (mic amp) |       | - longitude           |       | - Classify habits   |
| - Light sensor    |       | - timestamp           |       | - Detect commutes  |
| - Screen state    |       | - accuracy            |       +-------------------+
+-------------------+       | - activity_state      |
                            | - audio_level        |
                            | - light_level        |
                            | - is_screen_on      |
                            +----------------------+
```

*The original Python research engine (stay‑point extraction, Markov models, entropy analysis) remains unchanged and can still be used on the exported JSON data.*

---

## 📦 Repository Layout
```
Pervasive Computing/
├─ README.md                # <- You are here
├─ android/                 # Android source (Kotlin)
│   ├─ app/                # Gradle module
│   │   └─ src/main/java/com/habitminer/
│   │       ├─ data/               # Room entities & DB
│   │       ├─ engine/             # ViewModel, StayPointDetector, HabitEngine
│   │       ├─ service/            # LocationTrackingService (multisensor)
│   │       └─ ui/                 # Compose UI (Insights, Map, etc.)
│   └─ build.gradle                 # App Gradle file
├─ engine/                  # Python research engine (unchanged)
├─ data/                    # GeoLife dataset (git‑ignored)
└─ docs/                    # Architecture & methodology docs
```

---

## 🚀 Getting Started
### Prerequisites
- Android Studio Flamingo or newer (JDK 17)
- A physical Android device (or emulator with microphone & light sensor support) **or** grant microphone permission on a real device.
- (Optional) Python 3.10+ if you want to run the offline research engine on exported JSON.

### Build & Run the Android App
```bash
# From the project root
cd android
./gradlew assembleDebug   # builds the APK (gradlew wrapper is included)
# Install on a device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
When the app launches, it will request **Location** and **Microphone** permissions. Tap **Start Tracking** to begin collecting data.

### Export / Replay
- **Export**: In the Home screen tap **Load Real GeoLife Dataset** (or press the export button) – this writes `demo_trajectory.json` to `android/app/src/main/assets/`.
- **Replay**: The app can replay any exported JSON via the **Map** screen.

---

## 📊 Features
- **Multi‑sensor collection** (GPS, audio amplitude, ambient light, screen state)
- **Stay‑point detection** (spatial‑temporal thresholds)
- **POI clustering** (ST‑DBSCAN, unchanged from original)
- **HabitEngine** → Behavioral Timeline UI component
- **Predictability score** (from original Markov model) still shown on the Home screen
- **Export** of data to JSON for offline Python analysis

---

## 🧪 Demo
1. Open the app and start tracking.
2. Simulate a *sleep* scenario: place the phone in a dark drawer, keep it silent for ~10 minutes, screen off.
3. Simulate *phone‑usage*: turn the screen on and interact for a few minutes.
4. Go to **Insights** – you will see entries like:
   - `😴 Sleeping · Home (420 m)`
   - `📱 Phone Usage · In Transit (15 m)`
5. The **Predictability** score updates as more data is collected.

---

## 📚 Further Reading
- `docs/ARCHITECTURE.md` – detailed system diagram.
- `engine/README.md` – how to run the Python research pipeline on exported data.

---

## 📄 License
Distributed under the MIT License. See `LICENSE` for details.
