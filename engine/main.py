import os
import pandas as pd
import numpy as np
from parser import load_user_trajectory
from stay_point import extract_stay_points
from stdbscan import stdbscan_cluster, aggregate_pois
from markov_model import TimeConditionedMarkovModel
from routine_analytics import sequence_entropy, normalized_sequence_entropy
import visualize

def run_pipeline(data_path: str):
    """
    Runs the full Phase 1 processing pipeline on a GeoLife user directory.
    """
    print(f"--- HabitMiner Engine Pipeline ---")
    
    if not os.path.exists(data_path):
        print(f"[!] Data path not found: {data_path}. Please download GeoLife dataset or provide synthetic data.")
        return
        
    print(f"[1] Loading Trajectory Data from {data_path}...")
    df = load_user_trajectory(data_path)
    if df.empty:
        print("[!] No valid trajectory data found.")
        return
        
    print(f"    Loaded {len(df)} valid GPS points.")
    
    print("[2] Extracting Stay Points (D_th=200m, T_th=1200s)...")
    stay_points_df = extract_stay_points(df, D_th=200, T_th=1200)
    print(f"    Extracted {len(stay_points_df)} stay points.")
    
    if stay_points_df.empty:
        return
        
    print("[3] Clustering into Semantic POIs (ST-DBSCAN)...")
    clustered_sp = stdbscan_cluster(stay_points_df, eps1=300, eps2=3600, min_pts=3)
    pois_df = aggregate_pois(clustered_sp)
    num_pois = len(pois_df)
    print(f"    Discovered {num_pois} unique Semantic POIs.")
    
    print("[4] Training Time-Conditioned Markov Model...")
    # Prepare sequence for Markov Model
    sequence = []
    for _, row in clustered_sp.iterrows():
        sequence.append((int(row['cluster_id']), int(row['start_time'])))
        
    model = TimeConditionedMarkovModel(num_clusters=num_pois, alpha=0.1)
    model.fit(sequence)
    print("    Model trained successfully.")
    
    print("[5] Routine Analytics...")
    if num_pois > 0:
        visit_counts = pois_df['point_count'].values
        h_seq = sequence_entropy(visit_counts)
        h_norm = normalized_sequence_entropy(visit_counts, num_pois)
        print(f"    Sequence Entropy (H_seq): {h_seq:.3f}")
        print(f"    Normalized Entropy (H_norm): {h_norm:.3f}")
        if h_norm < 0.5:
            print("    -> Routine is highly predictable.")
        else:
            print("    -> Routine is highly variable/irregular.")
            
    print("[6] Visualizing Results...")
    map_obj = visualize.plot_trajectory(df.sample(min(1000, len(df))) if len(df) > 1000 else df) # Downsample for plotting
    map_obj = visualize.plot_pois(pois_df, map_obj)
    
    output_map = "habitminer_map.html"
    map_obj.save(output_map)
    print(f"    Map saved to {output_map}")
    print("Pipeline Complete.")

if __name__ == "__main__":
    # Example usage: Replace with actual path to GeoLife user folder, e.g., 'data/geolife/Data/000/'
    sample_path = "../data/geolife/Data/000/" 
    run_pipeline(sample_path)
