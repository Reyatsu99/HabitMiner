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
        val usages =
            (1..7).map { day ->
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
        val snapshots =
            usages.map { usage ->
                ContextSnapshotEntity(
                    timestamp = usage.startTime,
                    accelMean = 0.5f,
                    accelVariance = 0.1f,
                    accelStd = 0.3f,
                    accelMin = 0.1f,
                    accelMax = 1.0f,
                    accelEnergy = 0.25f,
                    gyroMean = 0.1f,
                    gyroVariance = 0.01f,
                    gyroStd = 0.1f,
                    gyroMin = 0.0f,
                    gyroMax = 0.2f,
                    gyroEnergy = 0.05f,
                    lightLux = 40f,
                    proximityNear = false,
                    stepsSinceLastSnapshot = 10,
                    batteryLevel = 80,
                    isCharging = false,
                    isScreenOn = true,
                    unlockCount = 3,
                    notificationsLastHour = 1,
                )
            }

        val baseline = builder.buildBaseline(usages, snapshots).single()

        assertEquals("WEEKDAY_MORNING", baseline.timeBin)
        assertTrue(baseline.dataPointCount >= 5)
        assertEquals(0.25f, baseline.avgAccelEnergy, 0.001f)
        assertEquals(40f, baseline.avgLightLux, 0.001f)
        assertEquals(3f, baseline.avgUnlockCount, 0.001f)
    }

    @Test
    fun `does not build a baseline from fewer than five days`() {
        val usages =
            (1..2).map { day ->
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
