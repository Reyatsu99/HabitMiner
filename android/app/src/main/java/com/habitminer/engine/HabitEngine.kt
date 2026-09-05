package com.habitminer.engine

import com.habitminer.data.RawGpsEntity
import java.util.Calendar

data class ContextSummary(
    val avgAudioDb: Float,
    val avgLightLux: Float,
    val screenOnRatio: Float // 0.0 to 1.0
)

enum class HabitType {
    SLEEPING,
    DEEP_FOCUS,
    SOCIALIZING_OR_ACTIVE,
    CASUAL_PHONE_USAGE,
    COMMUTING,
    UNKNOWN
}

data class HabitEvent(
    val habitType: HabitType,
    val startTime: Long,
    val durationSeconds: Long,
    val clusterId: Int // which POI this happened at, if applicable
)

object HabitEngine {

    /**
     * Extracts a Behavioral Timeline (HabitEvents) from raw GPS + context points.
     */
    fun extractHabits(points: List<RawGpsEntity>, stayPoints: List<StayPoint>): List<HabitEvent> {
        val events = mutableListOf<HabitEvent>()
        
        // Process stay points to find stationary habits (Sleep, Focus, Social)
        for (sp in stayPoints) {
            val ptsInStay = points.filter { it.timestamp in sp.startTime..sp.endTime }
            if (ptsInStay.isEmpty()) continue

            val avgAudio = ptsInStay.map { it.audioLevel }.average().toFloat()
            val avgLight = ptsInStay.map { it.lightLevel }.average().toFloat()
            val screenRatio = ptsInStay.count { it.isScreenOn }.toFloat() / ptsInStay.size.toFloat()

            // Time context
            val cal = Calendar.getInstance().apply { timeInMillis = sp.startTime * 1000 }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val isNight = hour >= 22 || hour < 7

            val habitType = when {
                isNight && avgLight < 15f && avgAudio < 45f && screenRatio < 0.1f -> HabitType.SLEEPING
                avgAudio < 50f && screenRatio < 0.2f -> HabitType.DEEP_FOCUS
                avgAudio > 65f -> HabitType.SOCIALIZING_OR_ACTIVE
                screenRatio > 0.6f -> HabitType.CASUAL_PHONE_USAGE
                else -> HabitType.UNKNOWN
            }

            events.add(
                HabitEvent(
                    habitType = habitType,
                    startTime = sp.startTime,
                    durationSeconds = sp.durationSeconds,
                    clusterId = sp.clusterId
                )
            )
        }

        // To find Commuting, we look at the gaps between stay points
        val sortedStays = stayPoints.sortedBy { it.startTime }
        for (i in 0 until sortedStays.size - 1) {
            val curr = sortedStays[i]
            val next = sortedStays[i + 1]
            val gap = next.startTime - curr.endTime
            
            // If the gap is longer than 10 mins and they changed clusters, it's likely a commute
            if (gap > 600 && curr.clusterId != next.clusterId) {
                events.add(
                    HabitEvent(
                        habitType = HabitType.COMMUTING,
                        startTime = curr.endTime,
                        durationSeconds = gap,
                        clusterId = -1 // Moving
                    )
                )
            }
        }

        return events.sortedBy { it.startTime }
    }
    
    fun getHabitEmoji(type: HabitType): String = when(type) {
        HabitType.SLEEPING -> "😴"
        HabitType.DEEP_FOCUS -> "📚"
        HabitType.SOCIALIZING_OR_ACTIVE -> "🍻"
        HabitType.CASUAL_PHONE_USAGE -> "📱"
        HabitType.COMMUTING -> "🚌"
        HabitType.UNKNOWN -> "🤔"
    }
    
    fun getHabitName(type: HabitType): String = when(type) {
        HabitType.SLEEPING -> "Sleeping"
        HabitType.DEEP_FOCUS -> "Deep Focus"
        HabitType.SOCIALIZING_OR_ACTIVE -> "Socializing / Active"
        HabitType.CASUAL_PHONE_USAGE -> "Phone Usage"
        HabitType.COMMUTING -> "Commuting"
        HabitType.UNKNOWN -> "Other Activity"
    }
}
