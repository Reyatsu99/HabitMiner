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

## ⚠️ Current Gaps & Flaws: Why It Is Not a Usable App
While the app successfully demonstrates a UI concept for sensor fusion, it is fundamentally **unusable as a real-world daily habit tracker** in its current state. The key architectural flaws are:

1. **Post-Processing Delay (No Real-Time Action)**: The engine relies on finding complete "Stay Points" before it runs its logic. If you sit down to focus, the app will not recognize it until *after* you have left the location 4 hours later. A real habit tracker needs a real-time sliding window to provide actionable notifications (e.g., "You've been focusing for 2 hours").
2. **No Tangible Value or Goals**: The app acts strictly as a passive data logger. It provides no daily goals, no real-time feedback, and no interactive tracking mechanics. It just dumps predicted labels onto a static UI screen.
3. **Severe Battery Drain (Hardware Lock)**: To sample audio, the service holds a continuous lock on the microphone (`MediaRecorder`). This prevents the Android OS from ever entering Doze (deep sleep) mode. Running this app will drain a modern smartphone battery in a matter of hours, making it impossible to use as a "daily" tracker.
4. **Brittle, Easily Fooled Heuristics**: The `HabitEngine` uses rigid, top-down `if-else` statements. If you leave your phone on a quiet desk and walk away to socialize, the app will confidently record "Deep Focus." If you work a night shift, the app will fail to understand you are awake. 
5. **Android OS Restrictions**: The app struggles with Android 11+ background location restrictions. It requires manual user intervention in the Settings app to grant "Allow all the time" location access, otherwise the OS aggressively kills the background service.

---

## 🌐 Dataset Drawbacks: What GeoLife is Actually Doing
During development and demonstration, HabitMiner leverages the renowned **Microsoft GeoLife** dataset, which is accessed via the `Load Real GeoLife Dataset` button. However, using this dataset for a "Multi-Modal Sensor" app is entirely superficial:

- **GeoLife Has No Context Sensors**: GeoLife only contains raw GPS coordinates (latitude, longitude, timestamp). It completely lacks the audio, light, and screen state data that HabitMiner claims to analyze.
- **Faked (Synthetic) Data Injection**: To make the UI demo work, the app's `GeoLifeDataLoader` literally generates fake, mathematically simulated sensor values (e.g., injecting low light values if the timestamp says it's night) and attaches them to the GPS points. The "Insights" you see from the demo are completely hallucinated by the engine based on faked data, rather than real multi-sensor fusion.
- **Beijing Bias**: 90%+ of GeoLife trajectories are localized in Beijing, China. When replaying the demo data, the map will instantly snap to Beijing. 
- **Real-World Tracking (Your Location)**: Despite the demo data limitations, HabitMiner's `LocationTrackingService` is built to function globally. When you tap **Start Tracking**, the app ignores the GeoLife faked data and records your actual, real-world GPS coordinates and live hardware sensors.

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
