import pandas as pd
import numpy as np
from sklearn.cluster import DBSCAN
from typing import Tuple

def haversine_distance_matrix(lats: np.ndarray, lons: np.ndarray) -> np.ndarray:
    """Computes pairwise Haversine distance matrix."""
    lats_rad = np.radians(lats)
    lons_rad = np.radians(lons)
    
    dlat = lats_rad[:, np.newaxis] - lats_rad
    dlon = lons_rad[:, np.newaxis] - lons_rad
    
    a = np.sin(dlat/2)**2 + np.cos(lats_rad[:, np.newaxis]) * np.cos(lats_rad) * np.sin(dlon/2)**2
    c = 2 * np.arcsin(np.sqrt(np.clip(a, 0, 1)))
    r = 6371000
    
    return c * r

def time_of_day_distance_matrix(timestamps: np.ndarray) -> np.ndarray:
    """Computes cyclic time-of-day pairwise distance matrix in seconds."""
    # Modulo 86400 (seconds in a day)
    tod = timestamps % 86400
    
    dtod = np.abs(tod[:, np.newaxis] - tod)
    
    # Cyclic difference: min(|t1 - t2|, 86400 - |t1 - t2|)
    cyclic_dtod = np.minimum(dtod, 86400 - dtod)
    return cyclic_dtod

def stdbscan_cluster(
    stay_points_df: pd.DataFrame,
    eps1: float = 300.0, # Spatial threshold (meters)
    eps2: float = 3600.0, # Temporal threshold (seconds)
    min_pts: int = 3
) -> pd.DataFrame:
    """
    Clusters stay points into Semantic POIs using ST-DBSCAN logic.
    
    To use scikit-learn's DBSCAN efficiently with both spatial and temporal constraints,
    we compute a custom distance matrix where:
    D(i, j) = 0 if (spatial_dist <= eps1 AND temporal_dist <= eps2) else infinity.
    
    This guarantees DBSCAN (with eps=0.5) will only cluster points satisfying both.
    """
    if stay_points_df.empty:
        return stay_points_df
        
    df = stay_points_df.copy()
    
    # We use start_time for the temporal clustering
    lats = df['lat'].values
    lons = df['lon'].values
    timestamps = df['start_time'].values
    
    # Compute matrices
    spatial_dist = haversine_distance_matrix(lats, lons)
    temporal_dist = time_of_day_distance_matrix(timestamps)
    
    # Construct combined boolean adjacency matrix
    # True if neighbors in BOTH space and time of day
    adjacency = (spatial_dist <= eps1) & (temporal_dist <= eps2)
    
    # Convert to a distance matrix suitable for DBSCAN
    # 0 for connected (adjacent), 1 for not connected
    custom_dist = np.where(adjacency, 0.0, 1.0)
    
    # Run DBSCAN with eps = 0.5 to only group 0.0 distances
    db = DBSCAN(eps=0.5, min_samples=min_pts, metric='precomputed')
    cluster_labels = db.fit_predict(custom_dist)
    
    df['cluster_id'] = cluster_labels
    return df

def aggregate_pois(clustered_df: pd.DataFrame) -> pd.DataFrame:
    """
    Aggregates clustered stay points into distinct POIs.
    Ignores noise points (cluster_id == -1).
    """
    if clustered_df.empty or 'cluster_id' not in clustered_df.columns:
        return pd.DataFrame()
        
    # Filter out noise
    valid_clusters = clustered_df[clustered_df['cluster_id'] != -1]
    
    if valid_clusters.empty:
        return pd.DataFrame()
        
    pois = []
    for cluster_id, group in valid_clusters.groupby('cluster_id'):
        pois.append({
            'cluster_id': cluster_id,
            'centroid_lat': group['lat'].mean(),
            'centroid_lon': group['lon'].mean(),
            'point_count': len(group)
        })
        
    return pd.DataFrame(pois)

if __name__ == "__main__":
    print("ST-DBSCAN module ready.")
