package com.habitminer.engine

import java.util.Calendar

data class Prediction(
    val label: String,       // e.g. "POI_0 (Home)"
    val confidence: Int,     // 0-100
    val timeSlot: String,    // "Morning", "Afternoon", "Evening", "Night"
    val clusterId: Int
)

data class POISummary(
    val clusterId: Int,
    val label: String,
    val visitCount: Int,
    val totalDurationMin: Long,
    val centroidLat: Double,
    val centroidLon: Double
)

object PredictionEngine {

    // Human-readable POI names (best-guess by visit time patterns)
    private val poiNames = listOf("Home", "Work / College", "Gym", "Cafe", "Other Place 1", "Other Place 2")

    fun getTimeSlot(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 6..11  -> "Morning"
            hour in 12..16 -> "Afternoon"
            hour in 17..20 -> "Evening"
            else           -> "Night"
        }
    }

    /**
     * Summarises discovered POIs from stay points with visit counts and centroids.
     */
    fun buildPOISummaries(labeledPoints: List<StayPoint>): List<POISummary> {
        val grouped = labeledPoints.filter { it.clusterId >= 0 }.groupBy { it.clusterId }
        return grouped.map { (id, points) ->
            POISummary(
                clusterId = id,
                label = poiNames.getOrElse(id) { "Place $id" },
                visitCount = points.size,
                totalDurationMin = points.sumOf { it.durationSeconds } / 60,
                centroidLat = points.map { it.lat }.average(),
                centroidLon = points.map { it.lon }.average()
            )
        }.sortedByDescending { it.visitCount }
    }

    /**
     * Predicts next most likely destination based on time of day and visit frequency.
     * Simple heuristic: during certain time slots, rank POIs by visit frequency weighted by time match.
     */
    fun predict(summaries: List<POISummary>, currentClusterId: Int): Prediction? {
        if (summaries.isEmpty()) return null

        val timeSlot = getTimeSlot()

        // Exclude current location from prediction
        val candidates = summaries.filter { it.clusterId != currentClusterId }
        if (candidates.isEmpty()) return summaries.firstOrNull()?.let {
            Prediction(it.label, 70, timeSlot, it.clusterId)
        }

        // Time-slot heuristic: prefer home in evening/night, work in morning/afternoon
        val scored = candidates.map { poi ->
            val timeBonus = when (timeSlot) {
                "Morning", "Afternoon" -> if (poi.label.contains("Work") || poi.label.contains("College")) 3 else 1
                "Evening", "Night"     -> if (poi.label.contains("Home")) 3 else 1
                else -> 1
            }
            Pair(poi, poi.visitCount * timeBonus)
        }.sortedByDescending { it.second }

        val best = scored.first().first
        val totalScore = scored.sumOf { it.second }.toDouble()
        val confidence = ((scored.first().second / totalScore) * 100).toInt().coerceIn(40, 95)

        return Prediction(
            label = best.label,
            confidence = confidence,
            timeSlot = timeSlot,
            clusterId = best.clusterId
        )
    }

    /**
     * Computes a predictability score 0-100 based on Shannon entropy of visit counts.
     * Lower entropy = more predictable = higher score.
     */
    fun predictabilityScore(summaries: List<POISummary>): Int {
        if (summaries.isEmpty()) return 0
        val total = summaries.sumOf { it.visitCount }.toDouble()
        if (total == 0.0) return 0

        val entropy = summaries.fold(0.0) { acc, poi ->
            val p = poi.visitCount / total
            if (p > 0) acc - p * Math.log(p) / Math.log(2.0) else acc
        }
        val maxEntropy = Math.log(summaries.size.toDouble()) / Math.log(2.0)
        val normalised = if (maxEntropy > 0) entropy / maxEntropy else 0.0
        return ((1.0 - normalised) * 100).toInt().coerceIn(0, 100)
    }
}
