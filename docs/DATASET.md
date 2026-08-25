# Microsoft GeoLife Dataset Guide & Parsing Specs 💾

This document details dataset structure, ingestion pipelines, file schemas, and preprocessing steps for validating **HabitMiner** using the **Microsoft GeoLife GPS Trajectory Dataset**.

---

## 1. Dataset Overview & Evaluation Considerations

The [Microsoft GeoLife GPS Trajectory Dataset](https://www.microsoft.com/en-us/research/publication/geolife-gps-trajectory-dataset-user-guide/) contains 17,855 trajectories collected from 178 users over a period of 5 years (April 2007 to August 2012).

- **Total Distance**: ~1,290,000 km
- **Total Duration**: 50,000+ hours
- **Sampling Rate**: 91.5% of trajectory points are logged every 1–5 seconds or 5–10 meters.
- **Geographic Primary Region**: Beijing, China (with global travel tracks).

### Dataset Characteristics & Bias Notes
- **Transportation Focus**: ~30% of trajectories include ground-truth transport mode labels (walk, bike, bus, car, subway).
- **User Demographics**: Collected predominantly by researchers and students in academic settings, resulting in structured work/campus routines.
- **Trajectory Bounding**: Spatial filtering should be applied **per user trajectory** or per analysis target region rather than applying a strict global bounding box that discards travel/out-of-town routines.

---

## 2. Directory Layout & Storage Structure

When unpacked into HabitMiner's local `data/` folder, the GeoLife dataset follows this hierarchy:

```
data/
└── geolife/
    ├── Data/
    │   ├── 000/
    │   │   ├── Trajectory/
    │   │   │   ├── 20081023025304.plt
    │   │   │   ├── 20081024020959.plt
    │   │   │   └── ...
    │   │   └── labels.txt (optional transport mode labels)
    │   ├── 001/
    │   │   └── Trajectory/
    │   │       └── ...
    │   └── ... (up to user 177)
    └── User Guide.pdf
```

---

## 3. GeoLife PLT File Format Specification

Each `.plt` file is a plain text document containing a 6-line header followed by comma-separated GPS record lines.

### File Schema

```
Line 1-6: Header Metadata (Ignore during parsing)
Line 7+:  Latitude, Longitude, Reserved (0), Altitude (ft), DateNum, DateString, TimeString
```

### Sample Raw Lines

```csv
Geolife trajectory
WGS 84
Altitude is in Feet
Reserved 3
0,2,255,5000
0
39.984702,116.318417,0,492,39744.1202546296,2008-10-23,02:53:04
39.984683,116.318450,0,492,39744.1202662037,2008-10-23,02:53:05
39.984686,116.318417,0,492,39744.1202777778,2008-10-23,02:53:06
```

### Field Breakdown

| Field Index | Field Name | Data Type | Description | Conversion / Usage |
| :--- | :--- | :--- | :--- | :--- |
| `0` | Latitude | `float` | WGS84 Latitude in decimal degrees. | Range: `[-90.0, 90.0]` |
| `1` | Longitude | `float` | WGS84 Longitude in decimal degrees. | Range: `[-180.0, 180.0]` |
| `2` | Reserved | `int` | Always `0`. | Unused. |
| `3` | Altitude | `float` | Altitude in feet (`-777` indicates invalid). | Convert to meters (`alt * 0.3048`). |
| `4` | DateNum | `float` | Excel serial date number. | Unused. |
| `5` | DateString | `string` | Date formatted as `YYYY-MM-DD`. | Combine with `TimeString`. |
| `6` | TimeString | `string` | Time formatted as `HH:MM:SS`. | Parse to UTC Unix Epoch timestamp. |

---

## 4. Python Ingestion & Preprocessing Pipeline

To clean raw `.plt` files into standardized DataFrames, the `engine/parser.py` module applies the following steps:

1. **Header Stripping**: Skip the first 6 lines of each `.plt` file.
2. **Timestamp Unification**: Merge `DateString` and `TimeString` into UTC datetime object `pandas.to_datetime(...)` and derive Unix timestamp (seconds).
3. **Outlier & Velocity Filtering**:
   - Filter invalid coordinate bounds ($\text{lat} \in [-90, 90]$, $\text{lon} \in [-180, 180]$).
   - Filter points with impossible point-to-point velocity ($> 150 \text{ km/h}$).
4. **Per-User Processing**: Preserve complete user trajectory history across trips while removing single isolated outlier points.

### Reference Python Loader Snippet

```python
from datetime import datetime
import pandas as pd


def parse_geolife_plt(file_path: str) -> pd.DataFrame:
    """Parses a single GeoLife .plt file into a clean Pandas DataFrame."""
    df = pd.read_csv(
        file_path,
        skiprows=6,
        header=None,
        names=[
            "lat",
            "lon",
            "dummy",
            "altitude_ft",
            "date_num",
            "date_str",
            "time_str",
        ],
    )

    # Convert altitude to meters
    df["altitude"] = df["altitude_ft"] * 0.3048

    # Combine date and time string into timestamp
    df["datetime"] = pd.to_datetime(df["date_str"] + " " + df["time_str"])
    df["timestamp"] = df["datetime"].astype("int64") // 10**9

    # Clean unused columns
    df = df[["lat", "lon", "altitude", "datetime", "timestamp"]]
    return df
```

---

## 5. Standardized Dataset Output Schemas

For down-stream evaluation in Phase 1 & 3, preprocessed output data is cached in `data/interim/`:

### `stay_points.csv`
```csv
user_id,stay_point_id,lat,lon,start_time,end_time,duration_seconds
000,sp_000_1,39.984702,116.318417,1224730384,1224732184,1800
000,sp_000_2,39.991200,116.321100,1224735000,1224763800,28800
```

### `semantic_pois.csv`
```csv
cluster_id,centroid_lat,centroid_lon,point_count,semantic_label
0,39.984710,116.318420,142,Home/Dorm
1,39.991215,116.321110,98,Lab/Office
```
