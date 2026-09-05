# HabitMiner: Offline Intelligence Engine (Phase 1) 🐍

The **Offline Intelligence Engine** is built in Python to implement, evaluate, and benchmark HabitMiner's algorithms against the Microsoft GeoLife trajectory dataset.

---

## Planned Architecture & Modules

```
engine/
├── README.md               # Module Overview & Developer Instructions
├── requirements.txt        # Python dependencies
├── parser.py               # GeoLife PLT dataset loader & trajectory cleaner
├── stay_point.py           # Stay Point Extraction algorithm (D_th, T_th)
├── stdbscan.py             # ST-DBSCAN spatio-temporal clustering engine
├── markov_model.py         # First-order Markov chain sequence miner & predictor
├── routine_analytics.py    # Shannon Entropy & KL-Divergence drift detector
└── visualize.py            # Folium map rendering & analytics plot generator
```

---

## Key Dependencies

- `pandas`: Data manipulation and temporal indexing.
- `numpy`: Matrix transformations & Markov transition calculations.
- `scikit-learn` / `scipy`: Clustering & statistical routines.
- `geopandas` / `shapely` / `folium`: Geospatial processing and visual map rendering.

---

## Quick Start

```bash
# From the project root (Pervasive Computing/)
# 1. Create and activate the virtual environment (one-time setup)
python3 -m venv .venv2
.venv2/bin/pip install pandas numpy scikit-learn scipy folium

# 2. Run the full pipeline (synthetic 7-day benchmark data)
cd engine
../.venv2/bin/python main.py

# 3. Open the generated report in your browser
xdg-open habitminer_report.html   # Linux
# or just drag the file into your browser
```

## Execution Workflow (Phase 1 Baseline)

1. Load dataset via `parser.py`.
2. Extract stay points via `stay_point.py`.
3. Cluster semantic POIs via `stdbscan.py`.
4. Train Markov sequence predictor via `markov_model.py`.
5. Compute routine stability and drift via `routine_analytics.py`.
6. Generate the interactive HTML dashboard via `visualize.py`.
