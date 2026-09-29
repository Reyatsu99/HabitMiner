import pytest
import pandas as pd
from datetime import datetime
from parser import generate_synthetic_usage
from feature_extraction import extract_time_bin_features, extract_sequences
from habit_discovery import mine_frequent_sequences
from baseline_model import PersonalBaseline
from deviation_detector import detect_deviations, DeviationEvent
from markov_model import TimeConditionedMarkovModel

@pytest.fixture
def synthetic_data():
    return generate_synthetic_usage(num_users=1, num_days=7)

def test_generate_synthetic_usage(synthetic_data):
    assert not synthetic_data.empty
    assert "user_id" in synthetic_data.columns
    assert "app_category" in synthetic_data.columns
    assert len(synthetic_data["user_id"].unique()) == 1

def test_feature_extraction(synthetic_data):
    features = extract_time_bin_features(synthetic_data)
    assert not features.empty
    assert "total_duration_seconds" in features.columns
    assert "category_duration_dict" in features.columns

def test_mine_frequent_sequences():
    seqs = {
        "u1|WEEKDAY|MORNING": [
            ["SOCIAL", "PRODUCTIVITY"],
            ["SOCIAL", "PRODUCTIVITY"],
            ["SOCIAL", "PRODUCTIVITY"],
            ["SOCIAL", "GAMING"]
        ]
    }
    habits = mine_frequent_sequences(seqs, min_support=2, min_confidence=0.5)
    assert len(habits) >= 1
    # Check if SOCIAL -> PRODUCTIVITY is found
    found = False
    for h in habits:
        if h.pattern == ["SOCIAL", "PRODUCTIVITY"]:
            found = True
            break
    assert found

def test_baseline_build(synthetic_data):
    features = extract_time_bin_features(synthetic_data)
    pb = PersonalBaseline(user_id="user_1")
    pb.build(features)
    assert isinstance(pb.bins, dict)

def test_markov_model():
    seqs = {
        "u1|WEEKDAY|MORNING": [
            ["SOCIAL", "PRODUCTIVITY"],
            ["SOCIAL", "PRODUCTIVITY"],
        ]
    }
    mm = TimeConditionedMarkovModel()
    mm.fit(seqs)
    preds = mm.predict_next("SOCIAL", "WEEKDAY", "MORNING", top_k=1)
    assert preds[0][0] == "PRODUCTIVITY"
