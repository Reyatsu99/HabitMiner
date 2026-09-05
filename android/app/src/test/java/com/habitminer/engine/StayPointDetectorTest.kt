package com.habitminer.engine

import com.habitminer.data.RawGpsEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StayPointDetectorTest {

    @Test
    fun testHaversineDistanceZeroForSamePoint() {
        val dist = StayPointDetector.haversineDistance(37.7749, -122.4194, 37.7749, -122.4194)
        assertEquals(0.0, dist, 0.001)
    }

    @Test
    fun testHaversineDistanceKnownPoints() {
        // Distance between SF (37.7749, -122.4194) and Oakland (37.8044, -122.2712) ~ 13km
        val dist = StayPointDetector.haversineDistance(37.7749, -122.4194, 37.8044, -122.2712)
        assertTrue(dist in 12000.0..14000.0)
    }

    @Test
    fun testExtractStayPointsEmptyList() {
        val stayPoints = StayPointDetector.extract(emptyList())
        assertTrue(stayPoints.isEmpty())
    }

    @Test
    fun testExtractStayPointsValidCluster() {
        // 3 GPS points within 50m over 20 minutes (1200 seconds)
        val startTime = 1700000000L
        val points = listOf(
            RawGpsEntity(1, 37.7749, -122.4194, startTime, 5.0f, "STATIONARY"),
            RawGpsEntity(2, 37.7750, -122.4195, startTime + 600, 5.0f, "STATIONARY"),
            RawGpsEntity(3, 37.7751, -122.4194, startTime + 1200, 5.0f, "STATIONARY"),
            // Point far away
            RawGpsEntity(4, 37.8044, -122.2712, startTime + 1800, 5.0f, "IN_VEHICLE")
        )

        val stayPoints = StayPointDetector.extract(points, dTh = 200.0, tTh = 900L)
        assertEquals(1, stayPoints.size)
        assertEquals(1200L, stayPoints[0].durationSeconds)
    }

    @Test
    fun testClusterIntoPOIs() {
        val sp1 = StayPoint(37.7749, -122.4194, 1000L, 2000L, 1000L)
        val sp2 = StayPoint(37.7750, -122.4195, 3000L, 4000L, 1000L) // Close to sp1
        val sp3 = StayPoint(37.8044, -122.2712, 5000L, 6000L, 1000L) // Far away

        val clustered = StayPointDetector.clusterIntoPOIs(listOf(sp1, sp2, sp3), clusterRadiusM = 300.0)
        assertEquals(3, clustered.size)
        assertEquals(0, clustered[0].clusterId)
        assertEquals(0, clustered[1].clusterId)
        assertEquals(1, clustered[2].clusterId)
    }
}
