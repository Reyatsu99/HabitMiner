"""
GeoLife export using gap-based stay point detection.
In GeoLife, users stop recording GPS when stationary.
A stay point = the gap between end of file_i and start of file_{i+1}
where: end_location ≈ start_location AND gap_duration >= T_th.
"""
import sys, os, json, glob
import pandas as pd
import numpy as np
from collections import defaultdict

def haversine(lat1, lon1, lat2, lon2):
    R = 6_371_000.0
    dlat = np.radians(lat2 - lat1)
    dlon = np.radians(lon2 - lon1)
    a = np.sin(dlat/2)**2 + np.cos(np.radians(lat1))*np.cos(np.radians(lat2))*np.sin(dlon/2)**2
    return 2 * R * np.arcsin(np.sqrt(max(0.0, min(1.0, float(a)))))

def parse_plt(path):
    try:
        df = pd.read_csv(path, skiprows=6, header=None,
                         names=["lat","lon","dummy","alt","date_num","date_str","time_str"],
                         on_bad_lines="skip")
        # Use fractional date_num for accurate timestamps (days since Dec 30, 1899)
        # Convert to unix: subtract epoch offset and multiply by seconds per day
        epoch_offset = pd.Timestamp("1899-12-30").timestamp()
        df["timestamp"] = df["date_num"].astype(float) * 86400 + epoch_offset
        df = df[(df.lat.between(-90,90)) & (df.lon.between(-180,180))]
        df = df.sort_values("timestamp").reset_index(drop=True)
        return df[["lat","lon","timestamp"]]
    except:
        return pd.DataFrame()

def detect_stay_points_gap_based(trajectory_files, d_th=200.0, t_th=900.0):
    """
    Detects stay points from gaps between consecutive PLT files.
    A stay occurs when: distance(end_i, start_{i+1}) < d_th AND time_gap > t_th.
    """
    # Parse all files and keep first/last point + timestamps
    file_data = []
    for f in trajectory_files:
        df = parse_plt(f)
        if len(df) < 2:
            continue
        file_data.append({
            "path":       f,
            "first_lat":  float(df.iloc[0]["lat"]),
            "first_lon":  float(df.iloc[0]["lon"]),
            "first_ts":   float(df.iloc[0]["timestamp"]),
            "last_lat":   float(df.iloc[-1]["lat"]),
            "last_lon":   float(df.iloc[-1]["lon"]),
            "last_ts":    float(df.iloc[-1]["timestamp"]),
            "df":         df
        })

    # Sort by start time
    file_data.sort(key=lambda x: x["first_ts"])

    stay_points = []
    for i in range(len(file_data) - 1):
        curr = file_data[i]
        nxt  = file_data[i+1]

        gap = nxt["first_ts"] - curr["last_ts"]
        dist = haversine(curr["last_lat"], curr["last_lon"],
                         nxt["first_lat"],  nxt["first_lon"])

        if gap >= t_th and dist <= d_th:
            stay_points.append({
                "lat":      (curr["last_lat"] + nxt["first_lat"]) / 2,
                "lon":      (curr["last_lon"] + nxt["first_lon"]) / 2,
                "startTime": int(curr["last_ts"]),
                "duration":  int(gap)
            })

    return stay_points, file_data

def cluster_pois(stay_pts, radius=400.0):
    centroids, labeled = [], []
    for sp in stay_pts:
        found = -1
        for idx, (clat, clon) in enumerate(centroids):
            if haversine(sp["lat"], sp["lon"], clat, clon) <= radius:
                found = idx; break
        if found == -1:
            centroids.append((sp["lat"], sp["lon"]))
            found = len(centroids) - 1
        labeled.append({**sp, "clusterId": found})
    return labeled

def run(user_folder, output_path):
    plt_files = sorted(glob.glob(os.path.join(user_folder, "Trajectory", "*.plt")))
    print(f"[1] Found {len(plt_files)} PLT files in {user_folder}")

    stay_pts, file_data = detect_stay_points_gap_based(plt_files, d_th=200, t_th=900)
    print(f"    Stay points (gap-based): {len(stay_pts)}")

    labeled    = cluster_pois(stay_pts, radius=400)
    poi_groups = defaultdict(list)
    for sp in labeled:
        poi_groups[sp["clusterId"]].append(sp)
    print(f"    POI clusters: {len(poi_groups)}")

    # Collect all GPS points
    all_dfs = [fd["df"] for fd in file_data]
    full_df = pd.concat(all_dfs).sort_values("timestamp").reset_index(drop=True)
    print(f"    Total GPS points: {len(full_df):,}")

    # Subsample to max 5000 for Android
    step    = max(1, len(full_df) // 5000)
    sampled = full_df.iloc[::step].reset_index(drop=True)

    gps_records = [
        {"latitude": round(float(r.lat),6), "longitude": round(float(r.lon),6),
         "timestamp": int(r.timestamp), "accuracy": 5.0, "activityState": "GEOLIFE"}
        for r in sampled.itertuples()
    ]

    poi_summaries = [
        {
            "clusterId":        cid,
            "centroidLat":      round(float(np.mean([s["lat"] for s in sps])), 6),
            "centroidLon":      round(float(np.mean([s["lon"] for s in sps])), 6),
            "visitCount":       len(sps),
            "totalDurationMin": max(1, sum(s["duration"] for s in sps) // 60)
        }
        for cid, sps in sorted(poi_groups.items())
    ]

    output = {
        "source":        "GeoLife Microsoft Research — User 000 (Beijing 2008-2012)",
        "gps_count":     len(gps_records),
        "poi_count":     len(poi_summaries),
        "gps_records":   gps_records,
        "poi_summaries": poi_summaries
    }

    os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
    with open(output_path, "w") as f:
        json.dump(output, f)

    size_mb = os.path.getsize(output_path) / (1024*1024)
    print(f"[2] Exported → {output_path}  ({size_mb:.1f} MB)")
    print(f"    {len(gps_records):,} GPS pts | {len(poi_summaries)} POIs | {len(stay_pts)} stay events")

if __name__ == "__main__":
    user_path = sys.argv[1] if len(sys.argv) > 1 else "data/Geolife Trajectories 1.3/Data/000"
    out_path  = sys.argv[2] if len(sys.argv) > 2 else "data/demo_trajectory.json"
    run(user_path, out_path)
