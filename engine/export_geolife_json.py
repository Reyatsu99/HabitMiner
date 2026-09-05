"""
Processes one GeoLife user's trajectory data and exports a JSON file
that can be pre-loaded into the Android app's Room database.
Usage:
    python3 engine/export_geolife_json.py <user_folder_path> [output_json]
Example:
    python3 engine/export_geolife_json.py "data/Geolife Trajectories 1.3/Data/000" data/demo_trajectory.json
"""

import sys
import os
import json

# Add engine dir to path
sys.path.insert(0, os.path.dirname(__file__))

from parser import load_user_trajectory
from stay_point import extract_stay_points
from stdbscan import stdbscan_cluster, aggregate_pois
from evaluate import evaluate_pipeline


def export_for_android(user_folder: str, output_json: str = "data/demo_trajectory.json"):
    print(f"[1] Loading GeoLife trajectory from: {user_folder}")
    df = load_user_trajectory(user_folder)

    if df.empty:
        print("[!] No data found. Check the path.")
        return

    print(f"    Loaded {len(df):,} GPS points")

    # Down-sample to max 5000 points to keep the JSON small for Android
    if len(df) > 5000:
        df = df.sample(5000).sort_values("timestamp").reset_index(drop=True)
        print(f"    Down-sampled to 5,000 points for Android import")

    # Run evaluation
    print("[2] Running evaluation pipeline...")
    full_df_for_eval = load_user_trajectory(user_folder)
    metrics = evaluate_pipeline(full_df_for_eval)
    print(f"    Top-1 Accuracy : {metrics.get('top1_accuracy')}%")
    print(f"    Top-3 Accuracy : {metrics.get('top3_accuracy')}%")
    print(f"    Shannon Entropy: {metrics.get('shannon_entropy')} bits")

    # Extract stay points
    print("[3] Extracting stay points and POIs...")
    stay_points = extract_stay_points(full_df_for_eval, D_th=200, T_th=900)
    clustered = stdbscan_cluster(stay_points, eps1=500, eps2=86400, min_pts=2)
    pois = aggregate_pois(clustered)
    print(f"    {len(stay_points)} stay points → {len(pois)} POI clusters")

    # Build GPS records list (for Android Room DB import)
    gps_records = []
    for _, row in df.iterrows():
        gps_records.append({
            "latitude": round(row["lat"], 6),
            "longitude": round(row["lon"], 6),
            "timestamp": int(row["timestamp"]),
            "accuracy": 5.0,
            "activityState": "GEOLIFE"
        })

    # Build POI summary for map display
    poi_records = []
    for _, row in pois.iterrows():
        poi_records.append({
            "clusterId": int(row["cluster_id"]),
            "centroidLat": round(row["centroid_lat"], 6),
            "centroidLon": round(row["centroid_lon"], 6),
            "visitCount": int(row["point_count"])
        })

    output = {
        "source": "GeoLife Microsoft Research Dataset - User 000",
        "metrics": metrics,
        "gps_count": len(gps_records),
        "poi_count": len(poi_records),
        "gps_records": gps_records,
        "poi_summaries": poi_records
    }

    os.makedirs(os.path.dirname(output_json), exist_ok=True)
    with open(output_json, "w") as f:
        json.dump(output, f, indent=2)

    size_mb = os.path.getsize(output_json) / (1024 * 1024)
    print(f"[4] Exported to: {output_json} ({size_mb:.1f} MB)")
    print("Done! Copy this file into android/app/src/main/assets/demo_trajectory.json")


if __name__ == "__main__":
    user_path = sys.argv[1] if len(sys.argv) > 1 else "data/Geolife Trajectories 1.3/Data/000"
    out_path  = sys.argv[2] if len(sys.argv) > 2 else "data/demo_trajectory.json"
    export_for_android(user_path, out_path)
