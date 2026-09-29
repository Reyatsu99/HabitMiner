package com.habitminer.engine

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.habitminer.data.AppUsageEntity
import com.habitminer.data.BaselineEntity
import com.habitminer.domain.AppIdentityResolver
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviationDetector
    @Inject
    constructor(
        private val appIdentityResolver: AppIdentityResolver,
    ) {
        data class DeviationResult(
            val timeBin: String,
            val deviationType: String,
            val description: String,
            val zScore: Float,
            val normalizedScore: Float,
            val affectedCategory: String,
        )

        fun detectDeviations(
            todayUsage: List<AppUsageEntity>,
            baseline: List<BaselineEntity>,
        ): List<DeviationResult> {
            val validTodayUsage = todayUsage.filterNot { appIdentityResolver.isLauncher(it.packageName) }
            val results = mutableListOf<DeviationResult>()
            val baselineMap = baseline.associateBy { it.timeBin }
            val gson = Gson()
            val type = object : TypeToken<Map<String, Long>>() {}.type

            val currentDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
            val todayDayType =
                if (currentDay == java.util.Calendar.SATURDAY || currentDay == java.util.Calendar.SUNDAY) {
                    "WEEKEND"
                } else {
                    "WEEKDAY"
                }
            val groupedToday = validTodayUsage.groupBy { "${it.dayType}_${it.timeSlot}" }
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val slotProgress =
                mapOf(
                    "MORNING" to
                        when {
                            hour < 6 -> 0f
                            hour >= 12 -> 1f
                            else -> (hour - 6) / 6f
                        },
                    "AFTERNOON" to
                        when {
                            hour < 12 -> 0f
                            hour >= 17 -> 1f
                            else -> (hour - 12) / 5f
                        },
                    "EVENING" to
                        when {
                            hour < 17 -> 0f
                            hour >= 22 -> 1f
                            else -> (hour - 17) / 5f
                        },
                )

            for ((bin, base) in baselineMap) {
                if (!bin.startsWith("${todayDayType}_")) continue
                val timeSlot = bin.substringAfter('_')
                // NIGHT spans midnight; suppress it until aggregation is day-boundary aware.
                val progress = slotProgress[timeSlot] ?: continue
                if (progress <= 0f) continue
                val usages = groupedToday[bin].orEmpty()

                val todayDuration = usages.sumOf { it.durationMs }
                val expectedDuration = (base.avgScreenTimeMs * progress).toLong()

                // 1. Check for excess duration
                if (base.stdScreenTimeMs > 0) {
                    val zScore = (todayDuration - expectedDuration).toFloat() / base.stdScreenTimeMs
                    if (zScore > 1.5f) {
                        val norm = (1f / (1f + kotlin.math.exp(-zScore))).coerceIn(0f, 1f)
                        val cat = usages.maxByOrNull { it.durationMs }?.appName ?: "Unknown App"
                        results.add(
                            DeviationResult(
                                timeBin = bin,
                                deviationType = "EXCESS_DURATION",
                                description =
                                    "Your total screen time was ${formatDuration(todayDuration)} " +
                                        "in the $timeSlot, versus your usual " +
                                        "${formatDuration(expectedDuration)} by now. The top app was $cat.",
                                zScore = zScore,
                                normalizedScore = norm,
                                affectedCategory = cat,
                            ),
                        )
                    }
                }

                // 2. Check for new behavior (categories)
                val todayCategories = usages.groupBy { it.appName }.mapValues { it.value.sumOf { u -> u.durationMs } }
                val baseCategories: Map<String, Long> =
                    runCatching {
                        gson.fromJson<Map<String, Long>>(base.typicalCategoriesJson, type)
                    }.getOrNull() ?: emptyMap()

                for ((cat, duration) in todayCategories) {
                    if (duration > 5 * 60 * 1000 && !baseCategories.containsKey(cat)) {
                        results.add(
                            DeviationResult(
                                timeBin = bin,
                                deviationType = "NEW_BEHAVIOR",
                                description =
                                    "$cat was used for ${formatDuration(duration)} " +
                                        "in the $timeSlot, but it is not part of your usual " +
                                        "routine for this time.",
                                zScore = 2.0f,
                                normalizedScore = 0.8f,
                                affectedCategory = cat,
                            ),
                        )
                    }
                }

                // Simplified version: if total duration < 20% of avg and avg > 30 mins
                if (progress >= 1f && base.avgScreenTimeMs > 30 * 60 * 1000 && todayDuration < base.avgScreenTimeMs * 0.2) {
                    results.add(
                        DeviationResult(
                            timeBin = bin,
                            deviationType = "MISSING_ROUTINE",
                            description =
                                "Your usual $timeSlot activity of about " +
                                    "${formatDuration(base.avgScreenTimeMs)} has not appeared today.",
                            zScore = -2.0f,
                            normalizedScore = 0.7f,
                            affectedCategory = "ALL",
                        ),
                    )
                }
            }

            return results
        }

        fun generateDescription(
            deviationType: String,
            category: String,
            zScore: Float,
            timeBin: String,
        ): String {
            val parts = timeBin.split("_")
            val timeLabel = if (parts.size > 1) parts[1].lowercase() else "time"

            return when (deviationType) {
                "EXCESS_DURATION" -> "$category took substantially more of your $timeLabel than your usual routine."
                "NEW_BEHAVIOR" -> "$category is new in your typical $timeLabel routine today."
                "MISSING_ROUTINE" -> "Your usual $timeLabel activity has not appeared today."
                else -> "Deviation detected"
            }
        }

        fun getOverallDeviationScore(deviations: List<DeviationResult>): Float {
            return deviations.maxOfOrNull { it.normalizedScore }?.coerceIn(0f, 1f) ?: 0f
        }

        private fun formatDuration(milliseconds: Long): String {
            val minutes = (milliseconds / 60_000).coerceAtLeast(1)
            return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
        }
    }
