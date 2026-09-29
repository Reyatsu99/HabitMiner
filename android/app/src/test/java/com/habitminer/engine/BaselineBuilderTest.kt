package com.habitminer.engine

import com.habitminer.data.AppUsageEntity
import com.habitminer.data.ContextSnapshotEntity
import com.habitminer.domain.AppIdentityResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import java.util.Calendar

class BaselineBuilderTest {
    private val resolver = mock<AppIdentityResolver>()
    private val builder = BaselineBuilder(resolver)

    @Test
    fun `builds a five day baseline with context averages`() {
        val usages = (1..5).map { day ->
            AppUsageEntity(
                packageName = "com.example.reader",
                appName = "Reader",
                appCategory = "EDUCATION",
                startTime = weekdayStart(day),
                endTime = weekdayStart(day) + 30 * 60_000,
                durationMs = 30 * 60_000,
                timeSlot = "MORNING",
                dayType = "WEEKDAY",
            )
        }
        val snapshots = usages.map { usage ->
            ContextSnapshotEntity(
                timestamp = usage.startTime,
                motionState = "STILL",
                lightLevel = 40f,
                batteryLevel = 80,
                isCharging = false,
                isScreenOn = true,
                unlockCount = 3,
                notificationCount = 1,
            )
        }

        val baseline = builder.buildBaseline(usages, snapshots).single()

        assertEquals("WEEKDAY_MORNING", baseline.timeBin)
        assertEquals(5, baseline.dataPointCount)
        assertEquals(30 * 60_000, baseline.avgScreenTimeMs)
        assertEquals("STILL", baseline.dominantMotionState)
        assertEquals(40f, baseline.avgLightLevel)
        assertEquals(3f, baseline.avgUnlockCount)
    }

    @Test
    fun `does not build a baseline from fewer than five days`() {
        val usages = (1..4).map { day ->
            AppUsageEntity(
                packageName = "com.example.reader",
                appName = "Reader",
                appCategory = "EDUCATION",
                startTime = weekdayStart(day),
                endTime = weekdayStart(day) + 60_000,
                durationMs = 60_000,
                timeSlot = "MORNING",
                dayType = "WEEKDAY",
            )
        }

        assertTrue(builder.buildBaseline(usages, emptyList()).isEmpty())
    }

    private fun weekdayStart(weekdayIndex: Int): Long =
        Calendar.getInstance().apply {
            var found = 0
            while (found < weekdayIndex) {
                add(Calendar.DAY_OF_YEAR, -1)
                if (get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                    found++
                }
            }
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
