import os
import pandas as pd
import numpy as np
from parser import load_user_trajectory
from stay_point import extract_stay_points
from stdbscan import stdbscan_cluster, aggregate_pois
from markov_model import TimeConditionedMarkovModel
from routine_analytics import sequence_entropy, normalized_sequence_entropy
import visualize

def run_pipeline(data_path: str = None):
    """
    Runs the full Phase 1 processing pipeline on a GeoLife user directory or synthetic benchmark dataset.
    """
    print(f"--- HabitMiner Engine Pipeline ---")
    
    if data_path and os.path.exists(data_path):
        print(f"[1] Loading Trajectory Data from {data_path}...")
        df = load_user_trajectory(data_path)
    else:
        print("[1] Data path not provided or not found. Generating 7-day benchmark synthetic trajectory...")
        from parser import generate_synthetic_trajectory
        df = generate_synthetic_trajectory(num_days=7)
        
    print(f"    Loaded {len(df)} valid GPS points.")
    
    print("[2] Running System Evaluation & Accuracy Metrics...")
    from evaluate import evaluate_pipeline
    metrics = evaluate_pipeline(df)
    
    print("[3] Extracting Stay Points & POI Clusters...")
    stay_points_df = extract_stay_points(df, D_th=200, T_th=900)
    clustered_sp = stdbscan_cluster(stay_points_df, eps1=500, eps2=86400, min_pts=2)
    pois_df = aggregate_pois(clustered_sp)
    
    print(f"    Mined {metrics.get('total_stay_points', len(stay_points_df))} stay points into {len(pois_df)} POI clusters.")
    print(f"    Top-1 Accuracy: {metrics.get('top1_accuracy')}% | Top-3 Accuracy: {metrics.get('top3_accuracy')}%")
    print(f"    Routine Entropy: {metrics.get('shannon_entropy')} bits")
    
    print("[4] Generating Presentation HTML Report...")
    output_html = visualize.create_presentation_report(df, pois_df, metrics, output_file="habitminer_report.html")
    print(f"    Report saved to file://{os.path.abspath(output_html)}")
    print("Pipeline Complete.")

if __name__ == "__main__":
    run_pipeline()

