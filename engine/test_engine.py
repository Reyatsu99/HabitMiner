import os
import unittest
import numpy as np
import pandas as pd
from datetime import datetime, timedelta

from parser import parse_geolife_plt
from stay_point import haversine_distance, extract_stay_points
from stdbscan import stdbscan_cluster, aggregate_pois
from markov_model import TimeConditionedMarkovModel
from routine_analytics import sequence_entropy, normalized_sequence_entropy, calculate_js_divergence

class TestStayPointExtraction(unittest.TestCase):
    def test_haversine_distance(self):
        # Distance between two known points (approx 0 meters for identical points)
        dist_same = haversine_distance(37.7749, -122.4194, 37.7749, -122.4194)
        self.assertAlmostEqual(dist_same, 0.0, places=3)

        # Approximate distance between SF and LA (~550km)
        dist_sf_la = haversine_distance(37.7749, -122.4194, 34.0522, -118.2437)
        self.assertGreater(dist_sf_la, 500000) # > 500 km
        self.assertLess(dist_sf_la, 600000)    # < 600 km

    def test_extract_stay_points(self):
        # Generate synthetic stationary trajectory (staying around lat 39.9, lon 116.3 for 30 mins)
        base_time = int(datetime(2026, 1, 1, 10, 0, 0).timestamp())
        records = []
        for i in range(30):
            records.append({
                'lat': 39.9000 + (np.random.rand() - 0.5) * 0.0001, # ~10m jitter
                'lon': 116.3000 + (np.random.rand() - 0.5) * 0.0001,
                'timestamp': base_time + i * 60 # 1 point per min
            })
        df = pd.DataFrame(records)

        # D_th = 200m, T_th = 1200s (20 mins)
        sp_df = extract_stay_points(df, D_th=200.0, T_th=1200.0)
        self.assertEqual(len(sp_df), 1)
        self.assertAlmostEqual(sp_df.iloc[0]['lat'], 39.9000, places=3)
        self.assertAlmostEqual(sp_df.iloc[0]['lon'], 116.3000, places=3)
        self.assertGreaterEqual(sp_df.iloc[0]['duration_seconds'], 1740)

class TestSTDBSCAN(unittest.TestCase):
    def test_stdbscan_clustering(self):
        # Create stay points clustered around 2 locations
        sp_data = [
            {'lat': 39.9001, 'lon': 116.3001, 'start_time': 1000},
            {'lat': 39.9002, 'lon': 116.3002, 'start_time': 1050},
            {'lat': 39.9001, 'lon': 116.3000, 'start_time': 1100},
            {'lat': 40.0000, 'lon': 116.5000, 'start_time': 5000},
            {'lat': 40.0001, 'lon': 116.5001, 'start_time': 5050},
            {'lat': 40.0002, 'lon': 116.5002, 'start_time': 5100},
        ]
        sp_df = pd.DataFrame(sp_data)

        clustered = stdbscan_cluster(sp_df, eps1=500.0, eps2=3600.0, min_pts=3)
        self.assertIn('cluster_id', clustered.columns)

        pois = aggregate_pois(clustered)
        self.assertEqual(len(pois), 2) # Should detect 2 POIs

class TestMarkovModel(unittest.TestCase):
    def test_markov_fit_predict(self):
        model = TimeConditionedMarkovModel(num_clusters=2, alpha=0.1)
        # Trajectory visits POI 0 then POI 1 repeatedly
        seq = [(0, 10000), (1, 10100), (0, 10200), (1, 10300)]
        model.fit(seq)

        preds = model.predict_next(0, 10400)
        self.assertEqual(len(preds), 2)
        self.assertGreater(preds[1], preds[0]) # Next POI from 0 should favor 1

class TestRoutineAnalytics(unittest.TestCase):
    def test_entropy(self):
        # Single POI visited always -> entropy = 0
        counts_single = np.array([10, 0, 0])
        self.assertAlmostEqual(sequence_entropy(counts_single), 0.0)

        # Uniform distribution across 2 POIs -> entropy = 1.0 bit
        counts_uniform = np.array([5, 5])
        self.assertAlmostEqual(sequence_entropy(counts_uniform), 1.0)
        self.assertAlmostEqual(normalized_sequence_entropy(counts_uniform, K=2), 1.0)

    def test_js_divergence(self):
        p = np.array([0.5, 0.5])
        q = np.array([0.5, 0.5])
        # Identical distributions -> JS div = 0
        self.assertAlmostEqual(calculate_js_divergence(p, q), 0.0, places=4)

        r = np.array([1.0, 0.0])
        # Completely disjoint distributions -> JS div > 0
        self.assertGreater(calculate_js_divergence(p, r), 0.0)

if __name__ == "__main__":
    unittest.main()
