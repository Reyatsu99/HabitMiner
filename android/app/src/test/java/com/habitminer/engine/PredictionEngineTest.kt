package com.habitminer.engine

import com.habitminer.data.AppUsageEntity
import com.habitminer.domain.AppIdentityResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class PredictionEngineTest {
    private lateinit var appIdentityResolver: AppIdentityResolver
    private lateinit var predictionEngine: PredictionEngine

    @Before
    fun setup() {
        appIdentityResolver = mock(AppIdentityResolver::class.java)
        // Assume none are launchers for this test
        `when`(appIdentityResolver.isLauncher(org.mockito.ArgumentMatchers.anyString())).thenReturn(false)
        predictionEngine = PredictionEngine(appIdentityResolver)
    }

    @Test
    fun testBuildTransitionMatrix() {
        // Create a sequence: A -> B -> C -> A -> B
        val usage =
            listOf(
                createUsage("A", 1000, 2000),
                // Immediate
                createUsage("B", 2000, 3000),
                // Immediate
                createUsage("C", 3000, 4000),
                // Immediate enough (1000ms gap)
                createUsage("A", 5000, 6000),
                // Immediate
                createUsage("B", 6000, 7000),
            )

        val matrix = predictionEngine.buildTransitionMatrix(usage)

        // Expected for "WEEKDAY_MORNING" (the hardcoded bin in createUsage):
        // A -> B: 2
        // B -> C: 1
        // C -> A: 1
        val binMatrix = matrix["WEEKDAY_MORNING"]
        requireNotNull(binMatrix)

        assertEquals(2, binMatrix["A"]?.get("B"))
        assertEquals(1, binMatrix["B"]?.get("C"))
        assertEquals(1, binMatrix["C"]?.get("A"))
    }

    @Test
    fun testPredictWithHistory() {
        val usage =
            listOf(
                createUsage("A", 1000, 2000),
                createUsage("B", 2000, 3000),
                createUsage("A", 4000, 5000),
                createUsage("B", 5000, 6000),
                createUsage("A", 7000, 8000),
                createUsage("C", 8000, 9000),
            )
        // A transitions to B twice, and to C once. B transitions to A once.

        val predictions = predictionEngine.predict(usage, "A", "MORNING", "WEEKDAY")

        // Top prediction should be B
        assertEquals(2, predictions.size)
        assertEquals("B", predictions[0].appName)
        assertEquals("C", predictions[1].appName)

        // Confidence for B should be higher than C
        assertTrue(predictions[0].confidence > predictions[1].confidence)
    }

    @Test
    fun testPredictFallbackToFrequent() {
        val usage =
            listOf(
                createUsage("D", 1000, 2000),
                // > 15 mins apart, no transition
                createUsage("E", 1000000, 2000000),
                createUsage("D", 3000000, 4000000),
            )

        // Ask for prediction after "X" (which has no history)
        val predictions = predictionEngine.predict(usage, "X", "MORNING", "WEEKDAY")

        // It should fallback to most frequent in bin, which is D (2 times) vs E (1 time)
        assertEquals(2, predictions.size)
        assertEquals("D", predictions[0].appName)
        assertEquals("E", predictions[1].appName)
    }

    private fun createUsage(
        appName: String,
        startTime: Long,
        endTime: Long,
    ): AppUsageEntity {
        return AppUsageEntity(
            id = 0,
            packageName = "com.test.$appName",
            appName = appName,
            appCategory = "TEST",
            startTime = startTime,
            endTime = endTime,
            durationMs = endTime - startTime,
            timeSlot = "MORNING",
            dayType = "WEEKDAY",
        )
    }
}
