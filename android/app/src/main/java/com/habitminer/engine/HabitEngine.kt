package com.habitminer.engine

import com.google.gson.Gson
import com.habitminer.data.AppUsageEntity
import com.habitminer.data.DiscoveredHabitEntity
import com.habitminer.domain.AppIdentityResolver
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln

@Singleton
class HabitEngine
    @Inject
    constructor(
        private val appIdentityResolver: AppIdentityResolver,
    ) {
        private data class UsageItem(val packageName: String, val appName: String)

        fun discoverHabits(allUsage: List<AppUsageEntity>): List<DiscoveredHabitEntity> {
            val validUsage = allUsage.filterNot { appIdentityResolver.isLauncher(it.packageName) }
            if (validUsage.isEmpty()) return emptyList()

            val gson = Gson()
            val habits = mutableListOf<DiscoveredHabitEntity>()

            // Group by dayType and timeSlot
            val groupedByBin = validUsage.groupBy { "${it.dayType}_${it.timeSlot}" }

            for ((bin, usagesInBin) in groupedByBin) {
                val parts = bin.split("_")
                val dayType = parts[0]
                val timeSlot = parts[1]

                // Group by date (calendar day)
                val groupedByDate =
                    usagesInBin.groupBy {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = it.startTime
                        "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
                    }

                val totalDays = groupedByDate.size
                if (totalDays < 3) continue // need at least 3 days for a pattern

                // Get sequence of usage items per day
                val sequencesPerDay =
                    groupedByDate.values.map { dayUsages ->
                        dayUsages.sortedBy { it.startTime }.map { UsageItem(it.packageName, it.appName) }
                    }

                // Extract n-grams (size 2 and 3)
                val patternCounts = mutableMapOf<List<UsageItem>, Int>()

                for (seq in sequencesPerDay) {
                    // simple deduplication of adjacent identical apps
                    val cleanSeq = seq.filterIndexed { index, item -> index == 0 || item.packageName != seq[index - 1].packageName }

                    val seenToday = mutableSetOf<List<UsageItem>>()

                    for (size in 2..3) {
                        if (cleanSeq.size < size) continue
                        for (i in 0..cleanSeq.size - size) {
                            val pattern = cleanSeq.subList(i, i + size)
                            if (seenToday.add(pattern)) {
                                patternCounts[pattern] = patternCounts.getOrDefault(pattern, 0) + 1
                            }
                        }
                    }
                }

                for ((pattern, count) in patternCounts) {
                    val confidence = count.toFloat() / totalDays
                    if (count >= 3 && confidence >= 0.4f) {
                        val dominantApp =
                            pattern.map { it.appName }
                                .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "App"

                        val habitName = labelHabit(dominantApp, timeSlot, dayType)
                        val description = pattern.joinToString(" → ") { it.appName }
                        val packageSequence = pattern.map { it.packageName }

                        habits.add(
                            DiscoveredHabitEntity(
                                habitName = habitName,
                                patternDescription = description,
                                appSequence = gson.toJson(packageSequence),
                                confidence = confidence,
                                occurrenceCount = count,
                                timeSlot = timeSlot,
                                dayType = dayType,
                                discoveredAt = System.currentTimeMillis(),
                                lastSeenAt = System.currentTimeMillis(),
                            ),
                        )
                    }
                }
            }

            // Return deduplicated top patterns
            return habits.sortedByDescending { it.confidence }
                .distinctBy { it.patternDescription + it.timeSlot + it.dayType }
        }

        fun labelHabit(
            dominantApp: String,
            timeSlot: String,
            dayType: String,
        ): String {
            val timeLabel =
                when (timeSlot) {
                    "MORNING" -> "🌅 Morning"
                    "AFTERNOON" -> "☀️ Afternoon"
                    "EVENING" -> "🌆 Evening"
                    "NIGHT" -> "🌙 Night"
                    else -> ""
                }

            return "$timeLabel $dominantApp Routine"
        }

        fun computePredictabilityScore(allUsage: List<AppUsageEntity>): Float {
            val validUsage = allUsage.filterNot { appIdentityResolver.isLauncher(it.packageName) }
            if (validUsage.isEmpty()) return 0f

            // compute transitions
            val transitions = mutableMapOf<String, Int>()
            val sorted = validUsage.sortedBy { it.startTime }

            for (i in 0 until sorted.size - 1) {
                val from = sorted[i].appName
                val to = sorted[i + 1].appName
                val key = "$from->$to"
                transitions[key] = transitions.getOrDefault(key, 0) + 1
            }

            val total = transitions.values.sum().toFloat()
            if (total == 0f) return 0f

            var entropy = 0f
            for (count in transitions.values) {
                val p = count / total
                entropy -= (p * ln(p))
            }

            val maxEntropy = ln(transitions.size.coerceAtLeast(1).toFloat())
            if (maxEntropy == 0f) return 100f

            val normalized = 1f - (entropy / maxEntropy)
            return (normalized * 100).coerceIn(0f, 100f)
        }
    }
