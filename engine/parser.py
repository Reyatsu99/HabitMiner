import pandas as pd
import numpy as np
from datetime import datetime
import os
import glob

def parse_geolife_plt(file_path: str) -> pd.DataFrame:
    """
    Parses a single GeoLife .plt file into a clean Pandas DataFrame.
    Filters out invalid bounds and impossible velocities (>150 km/h).
    """
    try:
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
            on_bad_lines="skip", # Handle any malformed lines gracefully
        )
    except Exception as e:
        print(f"Error reading {file_path}: {e}")
        return pd.DataFrame()

    if df.empty:
        return df

    # Convert altitude to meters (-777 indicates invalid, we'll keep it as is or could replace with NaN)
    df["altitude"] = np.where(df["altitude_ft"] == -777, np.nan, df["altitude_ft"] * 0.3048)

    # Combine date and time string into timestamp
    # Using format parameter speeds up parsing significantly
    try:
        df["datetime"] = pd.to_datetime(df["date_str"] + " " + df["time_str"], format="%Y-%m-%d %H:%M:%S")
    except Exception:
        # Fallback to mixed parsing if format fails
        df["datetime"] = pd.to_datetime(df["date_str"] + " " + df["time_str"])
        
    df["timestamp"] = df["datetime"].astype("int64") // 10**9

    # Clean unused columns
    df = df[["lat", "lon", "altitude", "datetime", "timestamp"]]
    
    # 1. Filter invalid bounds
    valid_bounds = (df["lat"] >= -90) & (df["lat"] <= 90) & (df["lon"] >= -180) & (df["lon"] <= 180)
    df = df[valid_bounds].copy()
    
    # Sort by timestamp just in case
    df = df.sort_values("timestamp").reset_index(drop=True)
    
    # 2. Filter velocity > 150 km/h (approx 41.67 m/s)
    # Calculate distance to previous point (Haversine)
    if len(df) > 1:
        lat1 = np.radians(df["lat"].shift(1))
        lon1 = np.radians(df["lon"].shift(1))
        lat2 = np.radians(df["lat"])
        lon2 = np.radians(df["lon"])
        
        dlon = lon2 - lon1
        dlat = lat2 - lat1
        
        a = np.sin(dlat / 2.0)**2 + np.cos(lat1) * np.cos(lat2) * np.sin(dlon / 2.0)**2
        c = 2 * np.arcsin(np.sqrt(np.clip(a, 0, 1)))
        
        dist_meters = 6371000 * c
        time_diff = df["timestamp"].diff()
        
        # Avoid division by zero
        velocity = dist_meters / np.where(time_diff == 0, np.nan, time_diff)
        
        # Keep first point (velocity is NaN), and points where velocity <= 41.67 m/s
        valid_velocity = velocity.isna() | (velocity <= 41.67)
        df = df[valid_velocity].reset_index(drop=True)

    return df

def load_user_trajectory(user_folder_path: str) -> pd.DataFrame:
    """
    Loads all .plt files for a specific user and concatenates them into a single trajectory.
    """
    trajectory_dir = os.path.join(user_folder_path, "Trajectory")
    if not os.path.exists(trajectory_dir):
        print(f"Directory not found: {trajectory_dir}")
        return pd.DataFrame()
        
    plt_files = glob.glob(os.path.join(trajectory_dir, "*.plt"))
    
    dfs = []
    for f in plt_files:
        df = parse_geolife_plt(f)
        if not df.empty:
            dfs.append(df)
            
    if not dfs:
        return pd.DataFrame()
        
    full_df = pd.concat(dfs, ignore_index=True)
    full_df = full_df.sort_values("timestamp").reset_index(drop=True)
    
    # After concatenation, we might want to re-run the velocity check across file boundaries
    if len(full_df) > 1:
        lat1 = np.radians(full_df["lat"].shift(1))
        lon1 = np.radians(full_df["lon"].shift(1))
        lat2 = np.radians(full_df["lat"])
        lon2 = np.radians(full_df["lon"])
        
        dlon = lon2 - lon1
        dlat = lat2 - lat1
        
        a = np.sin(dlat / 2.0)**2 + np.cos(lat1) * np.cos(lat2) * np.sin(dlon / 2.0)**2
        c = 2 * np.arcsin(np.sqrt(np.clip(a, 0, 1)))
        
        dist_meters = 6371000 * c
        time_diff = full_df["timestamp"].diff()
        
        velocity = dist_meters / np.where(time_diff == 0, np.nan, time_diff)
        valid_velocity = velocity.isna() | (velocity <= 41.67)
        full_df = full_df[valid_velocity].reset_index(drop=True)
        
    return full_df

if __name__ == "__main__":
    # Quick test logic
    print("GeoLife parser ready. Run with actual data path to test.")
