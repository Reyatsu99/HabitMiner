# HabitMiner Python Evaluation Engine

The HabitMiner evaluation engine validates **context-aware personalized behavioral data mining**. 
It focuses on frequency-based sequential pattern mining, time-conditioned Markov prediction, z-score deviation detection, and temporal clustering. This is purely statistical and context-aware—**not deep learning**.

## Pipeline
1. **Parse**: Ingest StudentLife, UCI HAR, or generate synthetic usage data.
2. **Features**: Extract time-binned features (total duration, sessions, category profile).
3. **Pattern Mining**: Frequency-based sequential pattern extraction.
4. **Baseline**: Build a personal behavioral baseline using mean/std and category distribution.
5. **Deviation Detection**: Z-score and KL-divergence to find behavioral anomalies.
6. **Evaluate**: Run 3 standard experiments.
7. **Report**: Generate self-contained HTML reports with Chart.js.

## Experiments
- **Experiment 1 (Habit Discovery)**: Evaluates confidence and support of mined habits.
- **Experiment 2 (Next-Behavior Prediction)**: TimeConditionedMarkovModel vs baseline prediction.
- **Experiment 3 (Deviation Detection)**: Inject controlled anomalies and evaluate Precision/Recall/F1.

## Setup & Data
Install requirements:
```bash
pip install -r requirements.txt
```

Datasets:
- **StudentLife**: Validates habit mining.
- **UCI HAR**: Validates motion-state classification (accelerometer → STILL/WALKING/ACTIVE).

*If datasets are not present, the engine falls back to realistic synthetic data generation.*

## Usage
Evaluate habits (Runs all 3 experiments & generates report):
```bash
python main.py --mode evaluate --dataset synthetic --num-days 14
```

Activity Recognition:
```bash
python main.py --mode activity_recognition --dataset uci_har --data-dir path/to/dataset
```
