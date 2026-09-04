import numpy as np
import pandas as pd
from typing import List, Tuple, Dict, Any

class TimeConditionedMarkovModel:
    """
    Time-Conditioned First-Order Markov Model.
    Predicts the next semantic POI given the current POI and Time-of-Day slot.
    """
    def __init__(self, num_clusters: int, alpha: float = 0.1):
        """
        Args:
            num_clusters (int): The total number of unique valid POIs (K).
            alpha (float): Laplace smoothing parameter to handle unseen transitions.
        """
        self.K = num_clusters
        self.alpha = alpha
        
        # 4 Time slots: Morning(0), Afternoon(1), Evening(2), Night(3)
        self.num_time_slots = 4
        
        # Transition Tensor: [time_slot, current_state, next_state]
        # Shape: (4, K, K)
        self.transition_counts = np.zeros((self.num_time_slots, self.K, self.K))
        self.transition_probs = np.zeros((self.num_time_slots, self.K, self.K))
        
    @staticmethod
    def get_time_slot(timestamp: int) -> int:
        """
        Maps a UTC timestamp to a local time slot (0-3).
        Assumption: using UTC hour for simplicity, in a real app convert to local tz.
        Morning (06:00-11:59): 0
        Afternoon (12:00-16:59): 1
        Evening (17:00-20:59): 2
        Night (21:00-05:59): 3
        """
        dt = pd.to_datetime(timestamp, unit='s')
        hour = dt.hour
        
        if 6 <= hour < 12:
            return 0
        elif 12 <= hour < 17:
            return 1
        elif 17 <= hour < 21:
            return 2
        else:
            return 3

    def fit(self, poi_sequence: List[Tuple[int, int]]):
        """
        Learns the transition probabilities from a sequence of visits.
        Args:
            poi_sequence: List of tuples (cluster_id, timestamp_of_visit)
        """
        # Filter out noise (-1)
        clean_seq = [(cid, ts) for cid, ts in poi_sequence if cid != -1]
        
        for i in range(len(clean_seq) - 1):
            curr_state = clean_seq[i][0]
            next_state = clean_seq[i+1][0]
            
            # The time slot context is based on the departure from curr_state
            # or arrival at next_state. We'll use departure time (next_state's timestamp approx)
            time_slot = self.get_time_slot(clean_seq[i+1][1])
            
            if curr_state < self.K and next_state < self.K:
                self.transition_counts[time_slot, curr_state, next_state] += 1
                
        self._calculate_probabilities()
        
    def _calculate_probabilities(self):
        """Applies Laplace smoothing and normalizes the transition tensor."""
        for t in range(self.num_time_slots):
            for i in range(self.K):
                # Count array for departing state i at time t
                counts = self.transition_counts[t, i, :]
                
                # Apply smoothing
                smoothed_counts = counts + self.alpha
                
                # Normalize
                total = np.sum(smoothed_counts)
                self.transition_probs[t, i, :] = smoothed_counts / total

    def predict_next(self, current_poi: int, target_timestamp: int) -> np.ndarray:
        """
        Predicts the probability distribution of the next POI.
        Args:
            current_poi: The current cluster_id
            target_timestamp: The time we are trying to predict the transition for
        Returns:
            np.ndarray of shape (K,) representing probabilities for each POI.
        """
        time_slot = self.get_time_slot(target_timestamp)
        
        # If current_poi is unseen or noise, fallback to a uniform distribution
        if current_poi == -1 or current_poi >= self.K:
            return np.ones(self.K) / self.K
            
        return self.transition_probs[time_slot, current_poi, :]

if __name__ == "__main__":
    print("Markov Model module ready.")
