package com.habitminer.engine

import com.google.gson.Gson
import com.habitminer.data.AppUsageEntity
import com.habitminer.data.BaselineEntity
import com.habitminer.data.ContextSnapshotEntity
import com.habitminer.domain.AppIdentityResolver
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class BaselineBuilder
    @Inject
    constructor(
        private val appIdentityResolver: AppIdentityResolver,
    ) {
        fun buildBaseline(
            allUsage: List<AppUsageEntity>,
            snapshots: List<ContextSnapshotEntity>,
        ): List<BaselineEntity> {
            val validUsage = allUsage.filterNot { appIdentityResolver.isLauncher(it.packageName) }
            val gson = Gson()
            val newBaselines = mutableListOf<BaselineEntity>()
            val groupedByBin = validUsage.groupBy { "${it.dayType}_${it.timeSlot}" }

            for ((bin, usagesInBin) in groupedByBin) {
                val groupedByDate =
                    usagesInBin.groupBy {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = it.startTime
                        "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
                    }

                val dataPointCount = groupedByDate.size
                if (dataPointCount < 5) continue

                val dailyDurations = mutableListOf<Long>()
                val dailySessions = mutableListOf<Float>()
                val categoryTotals = mutableMapOf<String, Long>()

                for ((_, dailyUsages) in groupedByDate) {
                    val totalDuration = dailyUsages.sumOf { it.durationMs }
                    dailyDurations.add(totalDuration)
                    dailySessions.add(dailyUsages.size.toFloat())

                    dailyUsages.forEach {
                        categoryTotals[it.appName] = categoryTotals.getOrDefault(it.appName, 0L) + it.durationMs
                    }
                }

                val avgDuration = dailyDurations.average()
                val stdDuration = sqrt(dailyDurations.map { (it - avgDuration) * (it - avgDuration) }.average()).toLong()

                val avgSess = dailySessions.average()
                val stdSess = sqrt(dailySessions.map { (it - avgSess) * (it - avgSess) }.average()).toFloat()

                // Average category times per day
                val avgCategories = categoryTotals.mapValues { it.value / dataPointCount }

                val binParts = bin.split('_', limit = 2)
                val matchingSnapshots =
                    snapshots.filter { snapshot ->
                        if (snapshot.lightLevel < 0f) return@filter false
                        val cal = Calendar.getInstance().apply { timeInMillis = snapshot.timestamp }
                        val day = cal.get(Calendar.DAY_OF_WEEK)
                        val dayType = if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) "WEEKEND" else "WEEKDAY"
                        val hour = cal.get(Calendar.HOUR_OF_DAY)
                        val timeSlot =
                            when (hour) {
                                in 6..11 -> "MORNING"
                                in 12..16 -> "AFTERNOON"
                                in 17..21 -> "EVENING"
                                else -> "NIGHT"
                            }
                        dayType == binParts[0] && timeSlot == binParts[1]
                    }
                val dominantMotion = matchingSnapshots.groupingBy { it.motionState }.eachCount().maxByOrNull { it.value }?.key ?: "UNKNOWN"
                val averageLight = matchingSnapshots.map { it.lightLevel }.average().takeIf { it.isFinite() }?.toFloat() ?: -1f
                val averageUnlocks = matchingSnapshots.map { it.unlockCount.toFloat() }.average().takeIf { it.isFinite() }?.toFloat() ?: 0f

                val newBaseline =
                    BaselineEntity(
                        timeBin = bin,
                        avgScreenTimeMs = avgDuration.toLong(),
                        stdScreenTimeMs = stdDuration,
                        avgSessionCount = avgSess.toFloat(),
                        stdSessionCount = stdSess,
                        avgUnlockCount = averageUnlocks,
                        typicalCategoriesJson = gson.toJson(avgCategories),
                        dominantMotionState = dominantMotion,
                        avgLightLevel = averageLight,
                        updatedAt = System.currentTimeMillis(),
                        dataPointCount = dataPointCount,
                    )

                // Recompute from the complete local history. Blending this value into
                // the previous result on every app launch caused the baseline to drift.
                newBaselines.add(newBaseline)
            }
            return newBaselines
        }

        fun hasEnoughData(allUsage: List<AppUsageEntity>): Boolean {
            return getDaysOfData(allUsage) >= 5
        }

        fun getDaysOfData(allUsage: List<AppUsageEntity>): Int {
            val validUsage = allUsage.filterNot { appIdentityResolver.isLauncher(it.packageName) }
            val days = mutableSetOf<String>()
            val cal = Calendar.getInstance()
            validUsage.forEach {
                cal.timeInMillis = it.startTime
                days.add("${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}")
            }
            return days.size
        }
    }
