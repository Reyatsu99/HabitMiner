import numpy as np
from scipy.spatial.distance import jensenshannon

def sequence_entropy(visit_counts: np.ndarray) -> float:
    """
    Calculates the Shannon Entropy (H_seq) of a sequence of visits.
    Args:
        visit_counts: Array of visit frequencies for each POI [c1, c2, ... cK]
    Returns:
        float: Entropy value >= 0
    """
    total_visits = np.sum(visit_counts)
    if total_visits == 0:
        return 0.0
        
    probs = visit_counts / total_visits
    # Filter out 0 probabilities to avoid log(0)
    probs = probs[probs > 0]
    
    return -np.sum(probs * np.log2(probs))

def normalized_sequence_entropy(visit_counts: np.ndarray, K: int) -> float:
    """
    Calculates Normalized Sequence Entropy (H_norm).
    H_norm -> 0: Highly regular routine.
    H_norm -> 1: Highly irregular.
    """
    if K <= 1:
        return 0.0
    h_seq = sequence_entropy(visit_counts)
    return h_seq / np.log2(K)

def calculate_js_divergence(p: np.ndarray, q: np.ndarray) -> float:
    """
    Calculates Bounded Jensen-Shannon (JS) Divergence between two distributions.
    Symmetric and bounded in [0, 1].
    """
    # scipy's jensenshannon returns the distance (sqrt of divergence).
    # We square it to get the divergence.
    # Note: Ensure p and q are normalized probability distributions.
    p_norm = p / np.sum(p)
    q_norm = q / np.sum(q)
    return jensenshannon(p_norm, q_norm, base=2) ** 2

class DriftDetector:
    """
    Detects routine drift using a rolling z-score over historical JS-Divergence values.
    """
    def __init__(self, baseline_dist: np.ndarray):
        self.baseline_dist = baseline_dist
        self.historical_js = []
        
    def check_drift(self, current_window_dist: np.ndarray) -> float:
        """
        Calculates JS divergence and returns the z-score.
        """
        js_div = calculate_js_divergence(self.baseline_dist, current_window_dist)
        
        if len(self.historical_js) < 2:
            self.historical_js.append(js_div)
            return 0.0 # Not enough history for standard deviation
            
        mu = np.mean(self.historical_js)
        sigma = np.std(self.historical_js)
        
        # Add current observation to history
        self.historical_js.append(js_div)
        
        if sigma == 0:
            return 0.0
            
        z_score = (js_div - mu) / sigma
        return z_score
        
if __name__ == "__main__":
    print("Routine Analytics module ready.")
