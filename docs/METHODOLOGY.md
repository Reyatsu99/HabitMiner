# HabitMiner: Algorithmic & Mathematical Methodology 📐

This document provides formal mathematical descriptions, pseudo-code algorithms, and parameter selections for the **HabitMiner** processing engine.

---

## 1. Stay Point Extraction Algorithm

### 1.1 Mathematical Formulation

A raw GPS trajectory $T$ is represented as an ordered sequence of $n$ spatio-temporal points:

$$T = \{p_1, p_2, \dots, p_n\}$$

where each point $p_i = (\text{lat}_i, \text{lon}_i, t_i)$.

A **Stay Point** $s = (\bar{\text{lat}}, \bar{\text{lon}}, t_{\text{start}}, t_{\text{end}}, \Delta t)$ represents a geographic region where a user remains stationary within a spatial distance threshold $D_{th}$ for at least a duration threshold $T_{th}$.

#### Distance Metric (Haversine Formula)

The spatial distance $\text{Dist}(p_i, p_j)$ between two coordinate pairs $p_i = (\phi_1, \lambda_1)$ and $p_j = (\phi_2, \lambda_2)$ in radians on an Earth of radius $R = 6,371,000 \text{ m}$ is computed via:

$$a = \sin^2\left(\frac{\Delta \phi}{2}\right) + \cos(\phi_1) \cdot \cos(\phi_2) \cdot \sin^2\left(\frac{\Delta \lambda}{2}\right)$$

$$c = 2 \cdot \text{atan2}\left(\sqrt{a}, \sqrt{1-a}\right)$$

$$\text{Dist}(p_i, p_j) = R \cdot c$$

#### Stay Condition Criteria

Starting at anchor point $p_i$, we evaluate candidate points $p_j$ ($j > i$). We find the first point $p_j$ that exits the spatial bound from anchor $p_i$:

$$\text{Dist}(p_i, p_j) > D_{th}$$

If the temporal duration of the preceding candidate set $\{p_i, p_{i+1}, \dots, p_{j-1}\}$ satisfies:

$$\Delta t = t_{j-1} - t_i \ge T_{th}$$

then points $\{p_i, \dots, p_{j-1}\}$ form a valid stay point $s$.

#### Centroid Calculation

The spatial centroid $(\bar{\text{lat}}, \bar{\text{lon}})$ of stay point $s$ is computed as the mean of coordinates in the candidate set:

$$\bar{\text{lat}} = \frac{1}{j - i} \sum_{m=i}^{j-1} \text{lat}_m, \quad \bar{\text{lon}} = \frac{1}{j - i} \sum_{m=i}^{j-1} \text{lon}_m$$

---

### 1.2 Pseudo-Code: Stay Point Extraction

```python
def haversine_distance(p1, p2):
    """Calculates spatial distance between two points in meters."""
    # Haversine implementation
    ...


def extract_stay_points(trajectory, D_th=200, T_th=1200):
    """
    trajectory: List of StayPoint objects (lat, lon, timestamp)
    D_th: Distance threshold in meters (default: 200m)
    T_th: Time threshold in seconds (default: 1200s / 20 mins)
    """
    stay_points = []
    i = 0
    n = len(trajectory)

    while i < n:
        j = i + 1
        while j < n:
            dist = haversine_distance(trajectory[i], trajectory[j])
            if dist > D_th:
                # Check if points between i and (j-1) satisfy duration T_th
                delta_t = trajectory[j - 1].timestamp - trajectory[i].timestamp
                if delta_t >= T_th:
                    # Compute mean centroid for valid window [i, j-1]
                    mean_lat = sum(p.lat for p in trajectory[i:j]) / (j - i)
                    mean_lon = sum(p.lon for p in trajectory[i:j]) / (j - i)
                    t_start = trajectory[i].timestamp
                    t_end = trajectory[j - 1].timestamp

                    stay_points.append(
                        StayPoint(mean_lat, mean_lon, t_start, t_end, delta_t)
                    )
                    i = j  # Move pointer past the stay point window
                else:
                    i += 1  # Not enough time spent, increment anchor
                break
            j += 1
        if j >= n:
            # End of trajectory evaluation
            delta_t = trajectory[n - 1].timestamp - trajectory[i].timestamp
            if delta_t >= T_th:
                mean_lat = sum(p.lat for p in trajectory[i:n]) / (n - i)
                mean_lon = sum(p.lon for p in trajectory[i:n]) / (n - i)
                stay_points.append(
                    StayPoint(
                        mean_lat,
                        mean_lon,
                        trajectory[i].timestamp,
                        trajectory[n - 1].timestamp,
                        delta_t,
                    )
                )
            break
    return stay_points
```

---

## 2. Spatio-Temporal Clustering (ST-DBSCAN)

Stay points are unlabelled geographical centroids. **ST-DBSCAN** clusters stay points into semantic Points of Interest (POIs) considering spatial radius $\epsilon_1$, time-of-day temporal window constraint $\epsilon_2$, and minimum neighborhood density $MinPts$.

### 2.1 Time-of-Day Cyclic Distance

To ensure correct clustering across daily cycles (e.g. 23:30 and 00:30 the next day differ by only 1 hour), temporal distance is computed on the time-of-day component ($t_{\text{tod}} = t \pmod{86400}$):

$$\Delta t_{\text{tod}}(s_a, s_b) = \min\left( |t_{\text{tod}, a} - t_{\text{tod}, b}|, \; 86400 - |t_{\text{tod}, a} - t_{\text{tod}, b}| \right)$$

### 2.2 Spatio-Temporal Neighborhood Condition

For two stay points $s_a = (\phi_a, \lambda_a, t_a)$ and $s_b = (\phi_b, \lambda_b, t_b)$, $s_b$ belongs to the spatio-temporal neighborhood $N_{(\epsilon_1, \epsilon_2)}(s_a)$ if:

$$N_{(\epsilon_1, \epsilon_2)}(s_a) = \{ s_b \in S \mid \text{Dist}(s_a, s_b) \le \epsilon_1 \land \Delta t_{\text{tod}}(s_a, s_b) \le \epsilon_2 \}$$

*Note: $\epsilon_1$ must be $\ge D_{th}$ (typically $\epsilon_1 \ge 300\text{ m}$) so stay points from the same physical place group into one POI cluster.*

### 2.3 Core Point Classification

A stay point $s_a$ is classified as a **Core Point** if:

$$|N_{(\epsilon_1, \epsilon_2)}(s_a)| \ge MinPts$$

Otherwise, it is categorized as a border point or noise point. Dense clusters formed by density-reachable core points define semantic POIs ($C_1 = \text{Home}, C_2 = \text{Workplace}, C_3 = \text{Gym}, \dots$).

---

## 3. Routine Sequence Mining with Time-Conditioned Markov Chains

### 3.1 Time-Conditioned Sequence Tokenization

After ST-DBSCAN assigns each stay point to a cluster ID $c \in \{1, 2, \dots, K\}$, a trajectory is transformed into a discrete sequence of POI visits associated with time-of-day slot $\tau$ (e.g., Morning $\tau_1$, Afternoon $\tau_2$, Evening $\tau_3$, Night $\tau_4$):

$$S = \langle (c_1, \tau_1), (c_2, \tau_2), \dots, (c_m, \tau_m) \rangle$$

### 3.2 Time-Conditioned Markov Model

To capture time-dependent routines (e.g. going to Gym after Work on Friday vs returning Home), we use a **Time-Conditioned First-Order Markov Model**:

$$P_{ij}(\tau) = P(S_{t+1} = s_j \mid S_t = s_i, \text{TimeSlot} = \tau) = \frac{N(s_i \xrightarrow{\tau} s_j) + \alpha}{\sum_{k=1}^{K} N(s_i \xrightarrow{\tau} s_k) + \alpha \cdot K}$$

where $\alpha = 0.1$ is additive Laplace smoothing to handle unseen state transitions gracefully.

---

## 4. Routine Analysis: Entropy & Drift Metrics

### 4.1 Routine Predictability via Sequence & Occupancy Entropy

We evaluate routine predictability using two complementary Shannon Entropy metrics:

1. **Visit Sequence Entropy ($H_{\text{seq}}$)**: Measures predictability of location transitions over visit sequence distribution $P_{\text{visit}}(c_i)$:

$$H_{\text{seq}}(X) = -\sum_{i=1}^{K} P_{\text{visit}}(c_i) \log_2 P_{\text{visit}}(c_i)$$

2. **Normalized Sequence Entropy ($H_{\text{norm}}$)**:

$$H_{\text{norm}}(X) = \frac{H_{\text{seq}}(X)}{\log_2(K)}$$

- $H_{\text{norm}} \to 0$: Highly regular routine (deterministic movement sequence).
- $H_{\text{norm}} \to 1$: Highly irregular, random trajectory behavior.

---

### 4.2 Routine Drift Detection via Bounded Jensen-Shannon (JS) Divergence

To prevent numerical divergence when comparing historical baseline distribution $P$ against current window distribution $Q$, we use **Jensen-Shannon (JS) Divergence**, which is symmetric and bounded in $[0, 1]$:

$$M = \frac{1}{2}(P + Q)$$

$$D_{JS}(P \parallel Q) = \frac{1}{2} D_{KL}(P \parallel M) + \frac{1}{2} D_{KL}(Q \parallel M)$$

where $D_{KL}(P \parallel M) = \sum_{i=1}^{K} P(x_i) \log_2 \left( \frac{P(x_i)}{M(x_i)} \right)$.

#### Adaptive Drift Classification ($z$-score)

Rather than using fixed unscaled thresholds, drift is detected adaptively using a **rolling $z$-score** over historical JS-Divergence values:

$$z = \frac{D_{JS, \text{current}} - \mu_{JS}}{\sigma_{JS}}$$

| $z$-Score Range | Routine Status | System Action |
| :--- | :--- | :--- |
| $z < 1.5$ | Normal Routine | Retain baseline transition matrix. |
| $1.5 \le z < 2.5$ | Mild Shift | Adapt transition probabilities using exponential smoothing ($\beta = 0.2$). |
| $z \ge 2.5$ | Structural Routine Drift | Trigger baseline model retraining and notify user of routine change. |

---

## 5. Evaluation Metrics Specification

To benchmark HabitMiner against baselines, the following metrics are defined:

1. **Next-Location Prediction Accuracy (Top-1 / Top-3)**:

$$\text{Acc}@k = \frac{1}{N} \sum_{i=1}^{N} \mathbb{I}(\text{actual}_{i} \in \text{Top-k}(\hat{P}_i))$$

2. **Mean Reciprocal Rank (MRR)**:

$$\text{MRR} = \frac{1}{N} \sum_{i=1}^{N} \frac{1}{\text{rank}_i}$$

3. **Cluster Quality (Silhouette Score)**: Measures spatial compactness and separation of discovered semantic POIs.

---

## 6. Hyperparameter Reference Table

| Hyperparameter | Symbol | Recommended Default | Rationale / Constraint |
| :--- | :--- | :--- | :--- |
| Stay Point Distance | $D_{th}$ | $200 \text{ m}$ | Maximum displacement during a stay episode. |
| Stay Point Duration | $T_{th}$ | $1200 \text{ s}$ ($20 \text{ min}$) | Minimum stationary time required. |
| ST-DBSCAN Spatial Radius | $\epsilon_1$ | $300 \text{ m}$ | Must be $\ge D_{th}$ to aggregate spatial stay points into POIs. |
| ST-DBSCAN Temporal Window | $\epsilon_2$ | $3600 \text{ s}$ ($1 \text{ hr}$) | Cyclic time-of-day difference window. |
| Minimum Density Points | $MinPts$ | $3 \text{ points}$ | Min stay points required to form a POI. |
| Markov Smoothing Factor | $\alpha$ | $0.1$ | Laplace additive parameter. |
| Drift $z$-Score Threshold | $z_{\text{drift}}$ | $2.5$ | Statistical anomaly threshold for routine drift. |
