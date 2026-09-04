import pandas as pd
import numpy as np
from typing import List, Dict

def haversine_distance(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """Calculates spatial distance between two points in meters."""
    # Convert latitude and longitude to radians
    lat1, lon1, lat2, lon2 = map(np.radians, [lat1, lon1, lat2, lon2])
    
    # Haversine formula
    dlon = lon2 - lon1
    dlat = lat2 - lat1
    a = np.sin(dlat/2)**2 + np.cos(lat1) * np.cos(lat2) * np.sin(dlon/2)**2
    c = 2 * np.arcsin(np.sqrt(np.clip(a, 0, 1)))
    r = 6371000 # Radius of earth in meters
    return c * r

def extract_stay_points(df: pd.DataFrame, D_th: float = 200.0, T_th: float = 1200.0) -> pd.DataFrame:
    """
    Extracts stay points from a raw GPS trajectory DataFrame.
    
    Args:
        df: Pandas DataFrame with columns ['lat', 'lon', 'timestamp']
        D_th: Distance threshold in meters (default 200m)
        T_th: Time threshold in seconds (default 1200s / 20 mins)
        
    Returns:
        DataFrame containing extracted stay points with columns:
        ['lat', 'lon', 'start_time', 'end_time', 'duration_seconds']
    """
    if df.empty:
        return pd.DataFrame()
        
    lats = df['lat'].values
    lons = df['lon'].values
    timestamps = df['timestamp'].values
    
    n = len(df)
    stay_points = []
    
    i = 0
    while i < n:
        j = i + 1
        while j < n:
            dist = haversine_distance(lats[i], lons[i], lats[j], lons[j])
            
            if dist > D_th:
                # Exited the spatial bound. Check if time spent meets threshold.
                delta_t = timestamps[j-1] - timestamps[i]
                if delta_t >= T_th:
                    # Valid stay point
                    mean_lat = np.mean(lats[i:j])
                    mean_lon = np.mean(lons[i:j])
                    
                    stay_points.append({
                        'lat': mean_lat,
                        'lon': mean_lon,
                        'start_time': timestamps[i],
                        'end_time': timestamps[j-1],
                        'duration_seconds': delta_t
                    })
                    i = j # Move pointer past the stay point window
                else:
                    i += 1 # Not enough time spent, increment anchor
                break
            j += 1
            
        # Reached the end of the trajectory
        if j >= n:
            delta_t = timestamps[n-1] - timestamps[i]
            if delta_t >= T_th:
                mean_lat = np.mean(lats[i:n])
                mean_lon = np.mean(lons[i:n])
                stay_points.append({
                    'lat': mean_lat,
                    'lon': mean_lon,
                    'start_time': timestamps[i],
                    'end_time': timestamps[n-1],
                    'duration_seconds': delta_t
                })
            break
            
    if not stay_points:
        return pd.DataFrame(columns=['lat', 'lon', 'start_time', 'end_time', 'duration_seconds'])
        
    return pd.DataFrame(stay_points)

if __name__ == "__main__":
    print("Stay point extractor ready.")
