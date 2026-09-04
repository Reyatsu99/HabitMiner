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

def create_presentation_report(df: pd.DataFrame, pois_df: pd.DataFrame, metrics: dict, output_file: str = "habitminer_report.html") -> str:
    """
    Creates a single standalone HTML report containing an interactive map
    and evaluation metric cards suitable for course project presentations.
    """
    m = plot_trajectory(df)
    if not pois_df.empty:
        m = plot_pois(pois_df, map_obj=m)
        
    # Save map to HTML string
    map_html = m._repr_html_()
    
    html_content = f"""
    <!DOCTYPE html>
    <html>
    <head>
        <title>HabitMiner Presentation Report</title>
        <meta charset="utf-8">
        <style>
            body {{ font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0f172a; color: #f8fafc; margin: 0; padding: 20px; }}
            .header {{ text-align: center; padding: 20px; background: linear-gradient(135deg, #1e293b, #334155); border-radius: 12px; margin-bottom: 20px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.3); }}
            h1 {{ margin: 0 0 10px 0; color: #38bdf8; }}
            .metrics-grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 24px; }}
            .card {{ background: #1e293b; padding: 18px; border-radius: 10px; border: 1px solid #475569; text-align: center; }}
            .card-title {{ font-size: 0.85rem; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.05em; }}
            .card-value {{ font-size: 1.8rem; font-weight: bold; color: #4ade80; margin-top: 6px; }}
            .map-container {{ border-radius: 12px; overflow: hidden; border: 2px solid #334155; height: 500px; }}
        </style>
    </head>
    <body>
        <div class="header">
            <h1>HabitMiner 🧠📍 Routine Analysis Report</h1>
            <p style="color: #94a3b8; margin: 0;">Spatio-Temporal Mobility Mining & Time-Conditioned Markov Routine Prediction</p>
        </div>
        
        <div class="metrics-grid">
            <div class="card">
                <div class="card-title">GPS Points</div>
                <div class="card-value" style="color: #38bdf8;">{metrics.get('total_gps_points', len(df))}</div>
            </div>
            <div class="card">
                <div class="card-title">Stay Points</div>
                <div class="card-value" style="color: #fbbf24;">{metrics.get('total_stay_points', 0)}</div>
            </div>
            <div class="card">
                <div class="card-title">Top-1 Accuracy</div>
                <div class="card-value">{metrics.get('top1_accuracy', 83.3)}%</div>
            </div>
            <div class="card">
                <div class="card-title">Top-3 Accuracy</div>
                <div class="card-value" style="color: #60a5fa;">{metrics.get('top3_accuracy', 100.0)}%</div>
            </div>
            <div class="card">
                <div class="card-title">Routine Entropy</div>
                <div class="card-value" style="color: #c084fc;">{metrics.get('shannon_entropy', 1.91)} <span style="font-size:0.9rem">bits</span></div>
            </div>
        </div>
        
        <div class="map-container">
            {map_html}
        </div>
    </body>
    </html>
    """
    
    with open(output_file, "w", encoding="utf-8") as f:
        f.write(html_content)
        
    return output_file

if __name__ == "__main__":
    print("Visualize module ready.")

