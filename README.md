# HabitMiner 📍🧠📱

**A Context‑Aware Mobile Framework for Daily Routine Discovery**

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)  
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-purple.svg)](https://kotlinlang.org/)  
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)

---

## 🎯 What HabitMiner is Currently Doing
HabitMiner is an offline-first Android application that goes beyond simple GPS tracking. It functions as a multi-modal edge sensor, tracking four core metrics simultaneously:
1. **Spatial Data (GPS)**: Live location tracking.
2. **Acoustic Data**: Ambient noise levels (dB) derived from microphone amplitude (without saving raw audio).
3. **Ambient Light (Lux)**: Captured via the hardware light sensor.
4. **Device Interaction**: Tracking screen-on/screen-off events.

The on-device **HabitEngine** aggregates this data during stationary "Stay Points" and applies heuristic rules to classify the user's behavior into semantic habits:
- **Sleeping**: (Night time + Dark + Quiet + Screen Off)
- **Deep Focus**: (Quiet + Screen Off)
- **Socializing / Active**: (Loud environments)
- **Phone Usage**: (High screen-on ratio)
- **Commuting**: (Transit between two distinct locations)

**Prediction & Predictability Scoring**:
In addition to classifying habits, the app features a **PredictionEngine** that evaluates your historical trajectories using a Markov-like model. 
- It generates a **Predictability Score** (0-100%) indicating how routine your movements are.
- It calculates your **Next Predicted Destination** (e.g., "🔮 Next Predicted Destination: Home (85% confidence)") based on your current location cluster and time of day.

These habits and predictions are visualized in a Jetpack Compose UI via the **Insights** screen and **Home** screen, while the **Map** screen plots the trajectories on a bundled, fully offline Leaflet map.

---

## ⚠️ Current Gaps & Flaws
While the app successfully demonstrates sensor fusion, it is currently a **prototype** and suffers from several architectural gaps:
1. **Severe Battery Drain**: The app polls GPS every 3 seconds and keeps a continuous lock on the microphone (`MediaRecorder`) to sample audio. In a real-world scenario, this drains the battery extremely fast. Production apps must use Duty Cycling (e.g., sample for 5 seconds every 5 minutes).
2. **Brittle Intelligence (Heuristics)**: The habit detection relies on hard-coded `if-else` thresholds. For example, if a user sleeps with a nightlight on or works night shifts, the static rules completely fail. A robust app requires Machine Learning (e.g., an on-device TFLite model) to learn non-linear patterns.
3. **Android OS Restrictions**: The app struggles with Android 11+ background location restrictions. It requires explicit user navigation to settings to grant "Allow all the time" location access, otherwise the OS kills the tracker in the background.

---

## 🌐 Dataset Drawbacks & Real-World Location
During development and demonstration, HabitMiner leverages the renowned **Microsoft GeoLife** dataset. However, this dataset comes with significant drawbacks for context-aware computing:
- **GPS-Only Limitation**: GeoLife only contains raw GPS coordinates (latitude, longitude, timestamp). It completely lacks the rich context (audio, light, screen state) that HabitMiner relies on. To make the demo work, the app mathematically injects **simulated, synthetic sensor data** into the GeoLife coordinates based on the hour of the day.
- **Beijing Bias**: The vast majority of GeoLife trajectories are centralized in Beijing, China. When loading the demo data, the map will snap to Beijing. 
- **Real-World Tracking (Your Location)**: Despite the demo data, HabitMiner's `LocationTrackingService` is fully functional worldwide. When you tap **Start Tracking**, the app will drop the Beijing demo data and begin recording your actual, real-world GPS coordinates and live sensor data wherever you are located.

---

## 🏗️ Architecture
```text
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

*The original Python research engine remains unchanged in the `/engine/` directory and can still be used on exported JSON data for deep Markov model analysis.*

---

## 🚀 Getting Started
### Prerequisites
- Android Studio Flamingo or newer (JDK 17).
- A physical Android device (emulators lack dynamic light/audio sensors).

### Build & Run
```bash
cd android
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
*Note: Ensure you grant Location (Allow all the time) and Microphone permissions for the app to function properly.*

## 📄 License
Distributed under the MIT License. See `LICENSE` for details.
