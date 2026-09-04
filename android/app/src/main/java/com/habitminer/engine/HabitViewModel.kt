package com.habitminer.engine

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.habitminer.data.AppDatabase
import com.habitminer.data.RawGpsEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HabitUiState(
    val isTracking: Boolean = false,
    val pointCount: Int = 0,
    val stayPoints: List<StayPoint> = emptyList(),
    val poiSummaries: List<POISummary> = emptyList(),
    val prediction: Prediction? = null,
    val predictabilityScore: Int = 0,
    val allPoints: List<RawGpsEntity> = emptyList()
)

class HabitViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getDatabase(app)

    private val _isTracking = MutableStateFlow(false)

    val uiState: StateFlow<HabitUiState> = combine(
        _isTracking,
        db.locationDao().getLocationCountFlow(),
        db.locationDao().getAllLocationsFlow()
    ) { tracking, count, points ->
        // Run intelligence pipeline on collected data
        val stayPoints = StayPointDetector.extract(points)
        val labeled = StayPointDetector.clusterIntoPOIs(stayPoints)
        val summaries = PredictionEngine.buildPOISummaries(labeled)

        // Detect current cluster from latest point
        val latestPoint = points.lastOrNull()
        val currentCluster = if (latestPoint != null && labeled.isNotEmpty()) {
            labeled.minByOrNull { sp ->
                StayPointDetector.haversineDistance(
                    latestPoint.latitude, latestPoint.longitude, sp.lat, sp.lon
                )
            }?.clusterId ?: -1
        } else -1

        val prediction = PredictionEngine.predict(summaries, currentCluster)
        val score = PredictionEngine.predictabilityScore(summaries)

        HabitUiState(
            isTracking = tracking,
            pointCount = count,
            stayPoints = labeled,
            poiSummaries = summaries,
            prediction = prediction,
            predictabilityScore = score,
            allPoints = points
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HabitUiState()
    )

    fun setTracking(value: Boolean) {
        _isTracking.value = value
    }

    fun refreshPrediction() {
        viewModelScope.launch {
            // Force recompute by emitting same state — the combine block handles it
            _isTracking.value = _isTracking.value
        }
    }
}
