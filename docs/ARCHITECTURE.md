# HabitMiner: Architecture Specification 🏗️

This document outlines the software architecture, data processing tiers, edge-computing privacy guarantees, database design, and cold-start mitigations for **HabitMiner**.

---

## 1. Architectural Philosophy & Principles

HabitMiner is engineered around three core principles:

1. **Privacy-Preserving Edge Intelligence**: Raw GPS coordinates contain sensitive private information (home address, personal habits, medical visits). HabitMiner performs spatio-temporal clustering and routine discovery locally on the client device without transmitting raw coordinates to external cloud infrastructure.
2. **Resource-Aware Pervasive Computing**: Continuous GPS polling drains smartphone battery rapidly. The system leverages **Android Activity Recognition API** (e.g. `STILL`, `WALKING`, `IN_VEHICLE`) and geofencing transition triggers rather than aggressive timer-based GPS polling.
3. **Interpretability & Explainability**: Rather than relying on opaque deep neural networks, HabitMiner utilizes transparent probabilistic models (Time-Conditioned Markov Chains) and Information Theory metrics (Shannon Entropy, Bounded JS-Divergence) that provide clear, inspectable behavioral indicators.

---

## 2. Multi-Tier Context Extraction Pipeline

The architecture is divided into five logical tiers operating sequentially:

```mermaid
graph TD
    subgraph Tier1["TIER 1: Sensing & Ingestion"]
        GPS[Raw GPS Stream / GeoLife PLT Parser]
        FLT[Noise Filter & Velocity Outlier Removal]
    end

    subgraph Tier2["TIER 2: Spatio-Temporal Extraction"]
        SPE[Stay Point Extraction Engine]
        SPDB[(Stay Point Store)]
    end

    subgraph Tier3["TIER 3: Semantic Discovery"]
        ST[ST-DBSCAN Clustering Engine]
        POI[(Semantic POI Catalog)]
    end

    subgraph Tier4["TIER 4: Routine Mining & Prediction"]
        SM[Time-Conditioned Sequence Formatter]
        MC[Time-Conditioned Markov Engine]
    end

    subgraph Tier5["TIER 5: Routine Stability & Drift Analysis"]
        SE[Sequence Entropy Calculator]
        JSD[JS-Divergence Drift Detector]
        DASH[Analytics & Dashboard]
    end

    GPS --> FLT --> SPE --> SPDB
    SPDB --> ST --> POI
    POI --> SM --> MC
    MC --> SE & JSD --> DASH
```

---

## 3. Android Mobile Architecture (Phase 2 Prototype)

The Android prototype implements clean architecture principles using Kotlin, Jetpack components, and Room database.

```mermaid
graph LR
    subgraph UI["UI & Presentation Layer"]
        MVVM[ViewModel / Jetpack Compose / Dashboard]
    end

    subgraph DOMAIN["Domain & Business Logic Layer"]
        SPE_M[StayPointExtractor]
        STD_M[STDBSCANClusterer]
        MC_M[TimeConditionedMarkovEngine]
        DRIFT_M[RoutineDriftAnalyzer]
    end

    subgraph DATA["Data & Persistence Layer"]
        ROOM[(Room SQLite Database + SQLCipher)]
        REPLAY[TrajectoryReplayManager]
        FUSED[FusedLocationProviderClient + ActivityRecognition]
    end

    MVVM --> DOMAIN
    DOMAIN --> DATA
    FUSED --> ROOM
    REPLAY --> ROOM
```

### Key Android Components

1. **Context-Aware Sensing Service**:
   - Leverages `ActivityRecognitionClient` to detect user motion state.
   - When motion state is `STILL`, location tracking pauses until `IN_MOTION` transition or geofence exit occurs, drastically reducing battery draw.
2. **Trajectory Replay Engine**:
   - Reads offline GeoLife `PLT` files or simulated GPS tracks to emulate real-world sensor streams for repeatable testing and demonstrations.
3. **WorkManager Scheduler**:
   - Executes compute-intensive tasks (ST-DBSCAN clustering and Markov matrix updating) during idle device states or overnight charging windows (`RequiresCharging = true`).
4. **Room Database Schema**:

```mermaid
erDiagram
    RAW_GPS {
        long id PK
        double latitude
        double longitude
        long timestamp
        float accuracy
    }
    SEMANTIC_POI {
        long id PK
        int cluster_id UK
        double centroid_lat
        double centroid_lon
        string label
    }
    STAY_POINT {
        long id PK
        long semantic_poi_id FK
        double mean_latitude
        double mean_longitude
        long start_time
        long end_time
        long duration_seconds
    }
    ROUTINE_TRANSITION {
        long id PK
        int source_poi_id FK
        int target_poi_id FK
        int time_slot
        int day_of_week
        int transition_count
    }

    RAW_GPS ||--o{ STAY_POINT : aggregates
    SEMANTIC_POI ||--o{ STAY_POINT : groups
    SEMANTIC_POI ||--o{ ROUTINE_TRANSITION : transition_from
```

---

## 4. Cold-Start Strategy & Mitigation

To handle new application installations before sufficient user routines are observed:

1. **Warmup Phase (Days 1–7)**: The system operates in passive sensing mode. Stay points and preliminary clusters are accumulated without exposing low-confidence predictions to the user interface.
2. **Time-of-Day Spatial Priors**: When sequence data is sparse, the predictor falls back to a time-of-day stationary prior ($P(s_j \mid \tau)$) rather than requiring full $s_i \to s_j$ transition history.
3. **Confidence Scoring Threshold**: Next-location predictions are only surfaced in the UI when the transition state probability exceeds confidence threshold $\theta_{\text{conf}} \ge 0.40$.

---

## 5. Phase 1 vs Phase 2 Data Interchange

To ensure seamless transitions between Python engine research (Phase 1) and Kotlin Android development (Phase 2), standardized JSON schema artifacts are used:

- `stay_points.json`: Exported stay points vector formatted with standardized timestamps (ISO-8601 UTC).
- `semantic_pois.json`: Centroid definitions, cluster radii, and discovered POI IDs.
- `transition_matrix.json`: Learned Markov state transition probabilities.

---

## 6. Security & Privacy Guardrails

- **Local Storage Encryption**: SQLite Room database encrypted using SQLCipher (`SupportFactory` initialized with KeyStore managed keys).
- **On-Device Execution**: No network socket transmission of raw GPS coordinates.
- **Zero Third-Party Analytics**: Analytics remain localized within the client device environment.
