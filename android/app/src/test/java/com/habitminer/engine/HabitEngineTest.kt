package com.habitminer.engine

import android.content.Context
import com.habitminer.data.AppUsageEntity
import com.habitminer.domain.AppIdentityResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class HabitEngineTest {
    private val mockContext = mock<Context>()
    private val mockResolver = mock<AppIdentityResolver>()
    private val habitEngine = HabitEngine(mockResolver)

    @Test
    fun testLauncherFilteringInSequenceMining() {
        whenever(mockResolver.isLauncher("com.matrixx.home")).thenReturn(true)
        whenever(mockResolver.isLauncher("com.whatsapp")).thenReturn(false)
        whenever(mockResolver.isLauncher("com.instagram.android")).thenReturn(false)

        val now = System.currentTimeMillis()

        // Create 3 days of activity interspersed with Home launcher
        val usages = mutableListOf<AppUsageEntity>()
        for (day in 0..3) {
            val baseTime = now - (day * 24 * 60 * 60 * 1000L)
            usages.add(
                AppUsageEntity(
                    packageName = "com.matrixx.home",
                    appName = "Matrixx Home",
                    appCategory = "OTHER",
                    startTime = baseTime + 1000,
                    endTime = baseTime + 5000,
                    durationMs = 4000,
                    timeSlot = "MORNING",
                    dayType = "WEEKDAY",
                ),
            )
            usages.add(
                AppUsageEntity(
                    packageName = "com.whatsapp",
                    appName = "WhatsApp",
                    appCategory = "COMMUNICATION",
                    startTime = baseTime + 6000,
                    endTime = baseTime + 60000,
                    durationMs = 54000,
                    timeSlot = "MORNING",
                    dayType = "WEEKDAY",
                ),
            )
            usages.add(
                AppUsageEntity(
                    packageName = "com.matrixx.home",
                    appName = "Matrixx Home",
                    appCategory = "OTHER",
                    startTime = baseTime + 61000,
                    endTime = baseTime + 65000,
                    durationMs = 4000,
                    timeSlot = "MORNING",
                    dayType = "WEEKDAY",
                ),
            )
            usages.add(
                AppUsageEntity(
                    packageName = "com.instagram.android",
                    appName = "Instagram",
                    appCategory = "SOCIAL",
                    startTime = baseTime + 66000,
                    endTime = baseTime + 120000,
                    durationMs = 54000,
                    timeSlot = "MORNING",
                    dayType = "WEEKDAY",
                ),
            )
        }

        val habits = habitEngine.discoverHabits(usages)

        assertFalse(habits.isEmpty())
        val habit = habits.first()
        assertEquals("WhatsApp → Instagram", habit.patternDescription)
        assertFalse(habit.patternDescription.contains("Matrixx Home"))
    }
}
