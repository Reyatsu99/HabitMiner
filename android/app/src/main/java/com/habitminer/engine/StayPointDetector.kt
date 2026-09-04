package com.habitminer.engine

import com.habitminer.data.RawGpsEntity
import kotlin.math.*

data class StayPoint(
    val lat: Double,
    val lon: Double,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val clusterId: Int = -1
)

object StayPointDetector {

    private const val EARTH_RADIUS_M = 6_371_000.0

    fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    /**
     * Extracts stay points from a list of GPS entities.
     * D_th: Distance threshold in metres (default 200m)
     * T_th: Time threshold in seconds (default 900s = 15 min)
     */
    fun extract(points: List<RawGpsEntity>, dTh: Double = 200.0, tTh: Long = 900L): List<StayPoint> {
        if (points.size < 2) return emptyList()

        val sorted = points.sortedBy { it.timestamp }
        val result = mutableListOf<StayPoint>()
        var i = 0

        while (i < sorted.size) {
            var j = i + 1
            var found = false

            while (j < sorted.size) {
                val dist = haversineDistance(
                    sorted[i].latitude, sorted[i].longitude,
                    sorted[j].latitude, sorted[j].longitude
                )
                if (dist > dTh) {
                    val deltaT = sorted[j - 1].timestamp - sorted[i].timestamp
                    if (deltaT >= tTh) {
                        val meanLat = sorted.subList(i, j).map { it.latitude }.average()
                        val meanLon = sorted.subList(i, j).map { it.longitude }.average()
                        result.add(
                            StayPoint(
                                lat = meanLat,
                                lon = meanLon,
                                startTime = sorted[i].timestamp,
                                endTime = sorted[j - 1].timestamp,
                                durationSeconds = deltaT
                            )
                        )
                        i = j
                        found = true
                    } else {
                        i++
                        found = true
                    }
                    break
                }
                j++
            }

            if (!found) {
                // Reached end of trajectory
                val deltaT = sorted.last().timestamp - sorted[i].timestamp
                if (deltaT >= tTh) {
                    val meanLat = sorted.subList(i, sorted.size).map { it.latitude }.average()
                    val meanLon = sorted.subList(i, sorted.size).map { it.longitude }.average()
                    result.add(
                        StayPoint(
                            lat = meanLat,
                            lon = meanLon,
                            startTime = sorted[i].timestamp,
                            endTime = sorted.last().timestamp,
                            durationSeconds = deltaT
                        )
                    )
                }
                break
            }
        }
        return result
    }

    /**
     * Simple greedy clustering of stay points into POIs.
     * Points within clusterRadiusM of each other belong to the same POI.
     */
    fun clusterIntoPOIs(stayPoints: List<StayPoint>, clusterRadiusM: Double = 300.0): List<StayPoint> {
        if (stayPoints.isEmpty()) return emptyList()

        val centroids = mutableListOf<Pair<Double, Double>>() // lat, lon per cluster
        val labeled = stayPoints.map { it.copy() }.toMutableList()

        labeled.forEachIndexed { idx, sp ->
            val existingCluster = centroids.indexOfFirst { (cLat, cLon) ->
                haversineDistance(sp.lat, sp.lon, cLat, cLon) <= clusterRadiusM
            }
            if (existingCluster == -1) {
                centroids.add(Pair(sp.lat, sp.lon))
                labeled[idx] = sp.copy(clusterId = centroids.size - 1)
            } else {
                labeled[idx] = sp.copy(clusterId = existingCluster)
            }
        }
        return labeled
    }
}
