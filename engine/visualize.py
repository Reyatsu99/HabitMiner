import folium
import pandas as pd

def plot_trajectory(df: pd.DataFrame, map_obj: folium.Map = None) -> folium.Map:
    """
    Plots the raw GPS trajectory as a line on a Folium map.
    """
    if df.empty:
        return map_obj or folium.Map()
        
    if map_obj is None:
        start_lat = df['lat'].iloc[0]
        start_lon = df['lon'].iloc[0]
        map_obj = folium.Map(location=[start_lat, start_lon], zoom_start=14)
        
    coords = df[['lat', 'lon']].values.tolist()
    folium.PolyLine(coords, color='blue', weight=2.5, opacity=0.7).add_to(map_obj)
    
    return map_obj

def plot_pois(pois_df: pd.DataFrame, map_obj: folium.Map = None) -> folium.Map:
    """
    Plots Semantic POIs (clusters) as markers with varying radii based on visit frequency.
    """
    if pois_df.empty:
        return map_obj or folium.Map()
        
    if map_obj is None:
        start_lat = pois_df['centroid_lat'].iloc[0]
        start_lon = pois_df['centroid_lon'].iloc[0]
        map_obj = folium.Map(location=[start_lat, start_lon], zoom_start=13)
        
    for _, row in pois_df.iterrows():
        folium.CircleMarker(
            location=[row['centroid_lat'], row['centroid_lon']],
            radius=min(20, max(5, row['point_count'] / 5.0)), # Scale radius somewhat
            popup=f"POI {row['cluster_id']} (Visits: {row['point_count']})",
            color='red',
            fill=True,
            fill_color='red'
        ).add_to(map_obj)
        
    return map_obj

if __name__ == "__main__":
    print("Visualize module ready.")
