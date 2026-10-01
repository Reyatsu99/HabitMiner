package com.habitminer.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val appUsageDao: AppUsageDao,
        private val contextDao: ContextDao,
        private val habitDao: HabitDao,
        private val baselineDao: BaselineDao,
        private val deviationDao: DeviationDao,
    ) {
        suspend fun exportDataToCsv(): String? {
            try {
                val exportDir = File(context.getExternalFilesDir(null), "export")
                if (!exportDir.exists()) {
                    exportDir.mkdirs()
                }

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

                // 1. Export App Usage
                val usageFile = File(exportDir, "app_usage_$timestamp.csv")
                val usages = appUsageDao.getAllUsage().firstOrNull() ?: emptyList()
                FileWriter(usageFile).use { writer ->
                    writer.append("id,packageName,appName,appCategory,startTime,endTime,")
                    writer.append("durationMs,timeSlot,dayType,previousPackageName,isHistorical\n")
                    usages.forEach {
                        writer.append(
                            "${it.id},${escapeCsv(it.packageName)},${escapeCsv(it.appName)},${escapeCsv(it.appCategory)},${it.startTime},",
                        )
                        writer.append("${it.endTime},${it.durationMs},${it.timeSlot},${it.dayType},")
                        writer.append("${escapeCsv(it.previousPackageName ?: "")},${it.isHistorical}\n")
                    }
                }

                // 2. Export Context Snapshots
                val contextFile = File(exportDir, "context_snapshots_$timestamp.csv")
                val contexts = contextDao.getAllSnapshots().firstOrNull() ?: emptyList()
                FileWriter(contextFile).use { writer ->
                    writer.append("id,timestamp,accelMean,accelVariance,accelStd,accelMin,accelMax,accelEnergy,")
                    writer.append("gyroMean,gyroVariance,gyroStd,gyroMin,gyroMax,gyroEnergy,lightLux,proximityNear,")
                    writer.append("stepsSinceLastSnapshot,batteryLevel,isCharging,isScreenOn,unlockCount,notificationsLastHour\n")
                    contexts.forEach {
                        writer.append("${it.id},${it.timestamp},${it.accelMean},${it.accelVariance},${it.accelStd},${it.accelMin},")
                        writer.append("${it.accelMax},${it.accelEnergy},${it.gyroMean},${it.gyroVariance},${it.gyroStd},${it.gyroMin},")
                        writer.append("${it.gyroMax},${it.gyroEnergy},${it.lightLux},${it.proximityNear ?: ""},")
                        writer.append("${it.stepsSinceLastSnapshot},${it.batteryLevel},${it.isCharging},${it.isScreenOn},")
                        writer.append("${it.unlockCount},${it.notificationsLastHour}\n")
                    }
                }

                // 3. Export Habits
                val habitsFile = File(exportDir, "habits_$timestamp.csv")
                val habits = habitDao.getAllHabits().firstOrNull() ?: emptyList()
                FileWriter(habitsFile).use { writer ->
                    writer.append("id,habitName,patternDescription,appSequence,confidence,")
                    writer.append("occurrenceCount,timeSlot,dayType,discoveredAt,lastSeenAt\n")
                    habits.forEach {
                        writer.append("${it.id},${escapeCsv(it.habitName)},${escapeCsv(it.patternDescription)},")
                        writer.append("${escapeCsv(it.appSequence)},${it.confidence},")
                        writer.append("${it.occurrenceCount},${it.timeSlot},${it.dayType},${it.discoveredAt},${it.lastSeenAt}\n")
                    }
                }

                // 4. Export Baselines
                val baselinesFile = File(exportDir, "baselines_$timestamp.csv")
                val baselines = baselineDao.getAllBaselines().firstOrNull() ?: emptyList()
                FileWriter(baselinesFile).use { writer ->
                    writer.append("timeBin,avgScreenTimeMs,stdScreenTimeMs,avgSessionCount,stdSessionCount,")
                    writer.append("avgUnlockCount,typicalCategoriesJson,avgAccelEnergy,avgLightLux,updatedAt,dataPointCount\n")
                    baselines.forEach {
                        writer.append(
                            "${escapeCsv(
                                it.timeBin,
                            )},${it.avgScreenTimeMs},${it.stdScreenTimeMs},${it.avgSessionCount},${it.stdSessionCount},",
                        )
                        writer.append(
                            "${it.avgUnlockCount},${escapeCsv(
                                it.typicalCategoriesJson,
                            )},${it.avgAccelEnergy},${it.avgLightLux},${it.updatedAt},${it.dataPointCount}\n",
                        )
                    }
                }

                // 5. Export Deviations
                val deviationsFile = File(exportDir, "deviations_$timestamp.csv")
                val deviations = deviationDao.getRecentDeviations(Int.MAX_VALUE).firstOrNull() ?: emptyList()
                FileWriter(deviationsFile).use { writer ->
                    writer.append("id,timestamp,timeBin,deviationType,description,zScore,normalizedScore,affectedCategory\n")
                    deviations.forEach {
                        writer.append("${it.id},${it.timestamp},${escapeCsv(it.timeBin)},${escapeCsv(it.deviationType)},")
                        writer.append("${escapeCsv(it.description)},${it.zScore},${it.normalizedScore},${escapeCsv(it.affectedCategory)}\n")
                    }
                }

                return exportDir.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        private fun escapeCsv(value: String): String {
            if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                return "\"" + value.replace("\"", "\"\"") + "\""
            }
            return value
        }
    }
