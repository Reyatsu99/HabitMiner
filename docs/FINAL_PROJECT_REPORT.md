# HabitMiner: A Context-Aware Daily Routine Analyzer
## Pervasive Computing - Final Project Report

---

## 1. Abstract & Objective
**HabitMiner** is an offline-first, mobile pervasive computing application designed to track, detect, and analyze user daily routines. Traditional habit trackers require manual input or rely strictly on GPS trajectory mining, which often lacks the context to differentiate between being at an office desk vs. socializing in an office cafeteria. 

To solve this, HabitMiner employs **Multi-Modal Sensor Fusion**. By combining GPS spatial data with ambient context (audio levels, light levels, and device interaction states), the system constructs a rich contextual profile of the user's environment. The primary objective is to intelligently classify raw trajectory data into semantic habit categories (e.g., Deep Focus, Sleeping, Socializing) entirely on-device, preserving user privacy.

---

## 2. Current Implementation & Architecture

### 2.1 Sensor Fusion & Data Collection
The Android client utilizes a `ForegroundService` to continuously collect an array of sensor inputs, effectively turning the smartphone into a pervasive edge sensor:
1. **Spatial Data (GPS)**: Acquired via the `FusedLocationProviderClient`.
2. **Acoustic Data (Audio Amplitude)**: Collected via `MediaRecorder` max amplitude polling. It converts raw microphone input into a baseline Decibel (dB) metric without storing raw audio, preserving privacy.
3. **Ambient Light (Lux)**: Acquired via `Sensor.TYPE_LIGHT` to differentiate indoor, outdoor, and nocturnal environments.
4. **Interaction State**: Captured via `BroadcastReceiver` listening for `ACTION_SCREEN_ON` and `ACTION_SCREEN_OFF` intents.

All data is asynchronously written to a local SQLite database using **Room**, defined by the `RawGpsEntity` schema.

### 2.2 Offline Intelligence & Stay-Point Detection
Data processing is handled entirely on-device. 
1. **Stay-Point Extraction**: The engine processes raw GPS data to find "Stay Points"—locations where the user remains within a defined spatial radius (e.g., 50 meters) for a minimum temporal threshold (e.g., 20 minutes).
2. **Context Aggregation**: For each Stay Point, the engine aggregates the temporal context. It calculates the `avgAudioDb`, `avgLightLux`, and `screenOnRatio` for the duration of the stay.

### 2.3 The Heuristic Engine
The current logic relies on a **Deterministic Heuristic Engine** (`HabitEngine.kt`). It applies rigid `if-else` thresholds to map aggregated context vectors into semantic habits:
- **Sleeping**: High probability if `hour` is nocturnal, `avgLight` < 15 Lux, `avgAudio` < 45 dB, and `screenRatio` < 10%.
- **Deep Focus**: Triggered by low audio (< 50 dB) and low screen interaction.
- **Socializing**: Triggered by sustained high ambient noise (> 65 dB).
- **Commuting**: Detected via temporal-spatial gaps (>10 minutes) between distinct Stay Point clusters.

### 2.4 Presentation Layer
The UI is constructed using **Jetpack Compose**. To maintain offline capabilities and avoid reliance on Google Maps API billing, the app utilizes an embedded `WebView`. It loads a locally bundled HTML file containing **Leaflet.js** and CartoDB/OpenStreetMap tiles, to which the Android client bridges raw JSON coordinates.

---

## 3. Shortcomings & Current Limitations

While the current prototype effectively demonstrates the viability of sensor fusion, it suffers from several severe architectural flaws that prevent it from being production-ready.

### 3.1 Severe Battery Drain
The implementation is highly energy-inefficient. 
- **Microphone Lock**: The `MediaRecorder` runs continuously to sample decibels. This prevents the CPU from entering deep sleep (Doze mode) and heavily drains the battery.
- **Aggressive Polling**: Location is polled every 3 seconds (`3000ms`). In a production setting, polling should occur in the magnitude of minutes.

### 3.2 Brittle Intelligence (The Heuristic Flaw)
The hardcoded rule-based system is rigid and fails in edge cases.
- **Non-Linear Context**: The current logic evaluates top-down. If a user is studying in a busy, loud café, the high audio threshold will incorrectly trigger the "Socializing" habit, completely ignoring the fact that their screen ratio is 0 (indicating they aren't using their phone and might be reading a book).
- **Mutual Exclusivity**: Thresholds do not account for environmental noise variance (e.g., a quiet bar vs. a loud office).

### 3.3 Operating System Non-Compliance
- **Background Limitations**: Android 11+ enforces strict background location limitations. The current permission flow does not adequately request `ACCESS_BACKGROUND_LOCATION`, meaning the OS will kill the tracker when the app is backgrounded.
- **Service Configuration**: The service lacks explicit foreground types (`foregroundServiceType="location|microphone"`), causing instant crashes on Android 14 target devices.

---

## 4. Production Roadmap & Future Plan

To transition HabitMiner into a robust, publishable application, the following architectural upgrades are planned.

### Phase 1: Machine Learning (TFLite) Integration
Replace the brittle heuristic engine with a **Multi-Layer Perceptron (MLP)**.
1. **Data Engineering**: Utilize the existing synthetic data generator and GeoLife datasets to compile a labeled CSV of feature vectors (e.g., `[avgAudio, stdAudio, avgLight, screenRatio, isNight, speed]`).
2. **Model Training**: Train a lightweight Neural Network (< 50KB) using TensorFlow.
3. **Edge Inference**: Deploy the model via `TensorFlow Lite for Android`. The app will pass feature vectors into the local interpreter to yield probabilistic habit classifications, effectively solving the rigid edge-case flaws of the current engine.

### Phase 2: Sensor Duty Cycling & Battery Optimization
Implement aggressive battery optimization strategies.
1. **Audio Sampling**: Refactor the service to sample audio for only 3 seconds every 5 minutes using byte-buffer `AudioRecord` rather than continuous file-based `MediaRecorder`.
2. **Location Batching**: Increase the `LocationRequest` interval to 60 seconds and set `maxWaitTime` to 5 minutes, allowing the Android OS to batch GPS callbacks and put the hardware to sleep.

### Phase 3: Robust Security & Compliance
1. **Permission Flow**: Implement an onboarding wizard that explicitly explains the necessity of background tracking and guides the user through the Android settings menu to grant "Allow all the time" location access.
2. **Data Export & Privacy**: Implement end-to-end encryption for the exported JSON trajectories to ensure user movement histories remain strictly confidential.

---
**Conclusion:**
HabitMiner successfully proves that fusing ambient context (audio/light/screen) with spatial trajectory data drastically improves semantic habit detection. By addressing the current battery inefficiencies and shifting from rigid heuristics to a lightweight TFLite machine learning model, HabitMiner is poised to become a highly accurate, privacy-first daily routine analyzer.
