# HabitMiner: Algorithmic & Mathematical Methodology 📐

This document provides formal mathematical descriptions, pseudo-code algorithms, and parameter selections for the **HabitMiner** processing engine.

---

## 1. Stay Point Extraction Algorithm

### 1.1 Mathematical Formulation

A raw GPS trajectory $T$ is represented as an ordered sequence of $n$ spatio-temporal points:

$$T = \{p_1, p_2, \dots, p_n\}$$

where each point $p_i = (\text{lat}_i, \text{lon}_i, t_i)$.

A **Stay Point** $s = (\bar{\text{lat}}, \bar{\text{lon}}, t_{\text{start}}, t_{\text{end}}, \Delta t)$ represents a geographic region where a user remains stationary within a distance threshold $D_{th}$ for at least a duration threshold $T_{th}$.

#### Distance Metric (Haversine Formula)

The spatial distance $\text{Dist}(p_i, p_j)$ between two coordinate pairs $p_i = (\phi_1, \lambda_1)$ and $p_j = (\phi_2, \lambda_2)$ in radians on an Earth of radius $R = 6,371,000 \text{ m}$ is computed via:

$$a = \sin^2\left(\frac{\Delta \phi}{2}\right) + \cos(\phi_1) \cdot \cos(\phi_2) \cdot \sin^2\left(\frac{\Delta \lambda}{2}\right)$$

$$c = 2 \cdot \text{atan2}\left(\sqrt{a}, \sqrt{1-a}\right)$$

$$\text{Dist}(p_i, p_j) = R \cdot c$$

#### Stay Condition Criteria

Starting at point $p_i$, we find the maximum index $j > i$ such that:

$$\text{Dist}(p_i, p_j) \le D_{th}$$

If the temporal duration satisfies:

$$\Delta t = t_j - t_i \ge T_{th}$$

then points $\{p_i, p_{i+1}, \dots, p_j\}$ form a valid stay point $s$.

#### Centroid Calculation

The spatial centroid $(\bar{\text{lat}}, \bar{\text{lon}})$ of stay point $s$ is computed as the arithmetic mean of coordinates in the candidate set:

$$\bar{\text{lat}} = \frac{1}{|k|} \sum_{m=i}^{j} \text{lat}_m, \quad \bar{\text{lon}} = \frac{1}{|k|} \sum_{m=i}^{j} \text{lon}_m$$

---

### 1.2 Pseudo-Code: Stay Point Extraction

```python
def extract_stay_points(trajectory, D_th=200, T_th=1200):
    """
    trajectory: List of tuples (lat, lon, timestamp)
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
                delta_t = trajectory[j - 1].timestamp - trajectory[i].timestamp
                if delta_t >= T_th:
                    # Compute mean centroid
                    mean_lat = mean([p.lat for p in trajectory[i:j]])
                    mean_lon = mean([p.lon for p in trajectory[i:j]])
                    t_start = trajectory[i].timestamp
                    t_end = trajectory[j - 1].timestamp

                    stay_points.append(
                        StayPoint(mean_lat, mean_lon, t_start, t_end, delta_t)
                    )
                    i = j  # Move pointer past the stay point
                else:
                    i += 1  # Not enough time spent
                break
            j += 1
        if j >= n:
            delta_t = trajectory[n - 1].timestamp - trajectory[i].timestamp
            if delta_t >= T_th:
                mean_lat = mean([p.lat for p in trajectory[i:n]])
                mean_lon = mean([p.lon for p in trajectory[i:n]])
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

Stay points are unlabelled geographical centroids. **ST-DBSCAN** clusters stay points into semantic Points of Interest (POIs) considering spatial radius $\epsilon_1$, temporal window constraint $\epsilon_2$, and minimum neighborhood points $MinPts$.

### 2.1 Spatio-Temporal Neighborhood Condition

For two stay points $s_a = (\phi_a, \lambda_a, t_a)$ and $s_b = (\phi_b, \lambda_b, t_b)$, $s_b$ belongs to the spatio-temporal $\epsilon$-neighborhood $N_{(\epsilon_1, \epsilon_2)}(s_a)$ if:

$$N_{(\epsilon_1, \epsilon_2)}(s_a) = \{ s_b \in S \mid \text{Dist}(s_a, s_b) \le \epsilon_1 \land |t_a - t_b| \pmod{86400} \le \epsilon_2 \}$$

*Note: $|t_a - t_b| \pmod{86400}$ enforces daily time-of-day alignment (e.g., distinguishing a location visited at 08:00 AM vs 11:00 PM).*

### 2.2 Core Point Classification

A stay point $s_a$ is classified as a **Core Point** if:

$$|N_{(\epsilon_1, \epsilon_2)}(s_a)| \ge MinPts$$

Otherwise, it is categorized as a border point or noise point. Dense clusters formed by density-reachable core points define semantic POIs ($C_1 = \text{Home}, C_2 = \text{Workplace}, C_3 = \text{Gym}, \dots$).

---

## 3. Routine Sequence Mining with Markov Chains

### 3.1 Sequence Tokenization

After ST-DBSCAN assigns each stay point to a cluster ID $c \in \{1, 2, \dots, K\}$, a daily trajectory is transformed into a discrete sequence of POI visits sorted chronologically:

$$S_{\text{day}} = \langle (c_1, t_1), (c_2, t_2), \dots, (c_m, t_m) \rangle$$

### 3.2 First-Order Markov Transition Model

We model transitions between discrete semantic places using a First-Order Markov Chain. The state space $S = \{c_1, c_2, \dots, c_K, \text{Unclustered}\}$.

The transition probability $P_{ij}$ from POI state $s_i$ to POI state $s_j$ is given by:

$$P_{ij} = P(S_{t+1} = s_j \mid S_t = s_i) = \frac{N(s_i \rightarrow s_j)}{\sum_{k=1}^{K} N(s_i \rightarrow s_k)}$$

where $N(s_i \rightarrow s_j)$ is the empirical count of transitions observed from POI $s_i$ to POI $s_j$.

### 3.3 State Transition Matrix

The learned routine model is stored as a stochastic transition matrix $\mathbf{P} \in \mathbb{R}^{K \times K}$:

$$\mathbf{P} = \begin{bmatrix}
P_{11} & P_{12} & \dots & P_{1K} \\
P_{21} & P_{22} & \dots & P_{2K} \\
\vdots & \vdots & \ddots & \vdots \\
P_{K1} & P_{K2} & \dots & P_{KK}
\end{bmatrix}, \quad \text{subject to } \sum_{j=1}^{K} P_{ij} = 1 \; \forall i$$

---

## 4. Routine Analysis: Entropy & Drift Metrics

### 4.1 Routine Predictability via Shannon Entropy

To quantify the degree of randomness vs. structured regularity in a user's daily mobility, we compute the **Shannon Entropy** $H(X)$ over location visit probability distribution $P(X)$:

$$H(X) = -\sum_{i=1}^{K} P(x_i) \log_2 P(x_i)$$

where $P(x_i)$ is the relative frequency of spending time at POI cluster $c_i$.

#### Normalized Routine Entropy ($H_{\text{norm}}$)

To allow comparison across users with varying numbers of discovered POIs ($K$):

$$H_{\text{norm}}(X) = \frac{H(X)}{\log_2(K)}$$

- $H_{\text{norm}} \to 0$: Highly predictable, rigid routine (e.g., spending time exclusively between Home and Work).
- $H_{\text{norm}} \to 1$: Unpredictable, high-entropy mobility pattern.

---

### 4.2 Routine Drift Detection via Kullback-Leibler (KL) Divergence

Routine drift occurs when a user significantly alters their behavior (e.g., changing jobs, moving homes, starting a new fitness schedule).

We compute the **KL-Divergence** $D_{KL}(P \parallel Q)$ to compare a historical baseline location transition distribution $P$ against a recent observation window distribution $Q$ (e.g., current week):

$$D_{KL}(P \parallel Q) = \sum_{i=1}^{K} P(x_i) \log_2 \left( \frac{P(x_i)}{Q(x_i) + \epsilon} \right)$$

*where $\epsilon = 10^{-6}$ is a Laplacian smoothing term preventing division by zero for unvisited states.*

#### Drift Classification Thresholds

| $D_{KL}$ Value Range | Routine Status | System Action |
| :--- | :--- | :--- |
| $D_{KL} < 0.25$ | Stable Routine | Maintain current transition model. |
| $0.25 \le D_{KL} \le 0.75$ | Moderate Variation | Adapt transition matrix with exponential smoothing. |
| $D_{KL} > 0.75$ | Significant Routine Drift | Trigger baseline retraining & issue routine drift notification. |

---

## 5. Hyperparameter Reference Table

| Hyperparameter | Symbol | Recommended Default | Description |
| :--- | :--- | :--- | :--- |
| Stay Point Distance | $D_{th}$ | $200 \text{ m}$ | Maximum spatial displacement during a stay. |
| Stay Point Duration | $T_{th}$ | $1200 \text{ s}$ ($20 \text{ min}$) | Minimum required time duration. |
| ST-DBSCAN Spatial Radius | $\epsilon_1$ | $150 \text{ m}$ | Max spatial distance between cluster core points. |
| ST-DBSCAN Temporal Window | $\epsilon_2$ | $3600 \text{ s}$ ($1 \text{ hr}$) | Max daily time window difference. |
| Minimum Density Points | $MinPts$ | $3 \text{ points}$ | Minimum stay points to constitute a POI cluster. |
| KL Drift Threshold | $\theta_{\text{drift}}$ | $0.75$ | Threshold indicating structural routine shift. |
