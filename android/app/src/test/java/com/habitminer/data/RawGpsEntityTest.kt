package com.habitminer.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RawGpsEntityTest {

    @Test
    fun testEntityCreationAndFields() {
        val entity = RawGpsEntity(
            id = 1L,
            latitude = 37.7749,
            longitude = -122.4194,
            timestamp = 1700000000L,
            accuracy = 4.5f,
            activityState = "WALKING"
        )

        assertEquals(1L, entity.id)
        assertEquals(37.7749, entity.latitude, 0.0001)
        assertEquals(-122.4194, entity.longitude, 0.0001)
        assertEquals(1700000000L, entity.timestamp)
        assertEquals(4.5f, entity.accuracy, 0.001f)
        assertEquals("WALKING", entity.activityState)
    }
}
