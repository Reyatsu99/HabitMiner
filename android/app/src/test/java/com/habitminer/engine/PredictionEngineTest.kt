package com.habitminer.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PredictionEngineTest {

    @Test
    fun testBuildPOISummaries() {
        val sp1 = StayPoint(37.7749, -122.4194, 1000L, 2000L, 1200L, clusterId = 0)
        val sp2 = StayPoint(37.7750, -122.4195, 3000L, 4000L, 1800L, clusterId = 0)
        val sp3 = StayPoint(37.8044, -122.2712, 5000L, 6000L, 600L, clusterId = 1)

        val summaries = PredictionEngine.buildPOISummaries(listOf(sp1, sp2, sp3))
        assertEquals(2, summaries.size)

        val homeSummary = summaries.first { it.clusterId == 0 }
        assertEquals(2, homeSummary.visitCount)
        assertEquals(50L, homeSummary.totalDurationMin) // (1200 + 1800) / 60
    }

    @Test
    fun testPredictabilityScoreSingleCluster() {
        val summaries = listOf(
            POISummary(0, "Home", 10, 500L, 37.7749, -122.4194)
        )
        val score = PredictionEngine.predictabilityScore(summaries)
        assertEquals(100, score)
    }

    @Test
    fun testPredictabilityScoreEqualVisits() {
        val summaries = listOf(
            POISummary(0, "Home", 10, 500L, 37.7749, -122.4194),
            POISummary(1, "Work", 10, 500L, 37.8044, -122.2712)
        )
        val score = PredictionEngine.predictabilityScore(summaries)
        assertEquals(0, score)
    }

    @Test
    fun testPredictNextLocation() {
        val summaries = listOf(
            POISummary(0, "Home", 10, 500L, 37.7749, -122.4194),
            POISummary(1, "Work / College", 15, 600L, 37.8044, -122.2712)
        )

        val prediction = PredictionEngine.predict(summaries, currentClusterId = 0)
        assertNotNull(prediction)
        assertEquals("Work / College", prediction?.label)
        assertTrue(prediction!!.confidence in 40..95)
    }
}
