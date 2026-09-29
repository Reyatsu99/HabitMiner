package com.habitminer.engine

import android.app.AppOpsManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.habitminer.collection.DataCollectionWorker
import com.habitminer.collection.HabitNotificationListener
import com.habitminer.collection.UsageDataCollector
import com.habitminer.data.ContextSnapshotEntity
import com.habitminer.data.DeviationEntity
import com.habitminer.data.DiscoveredHabitEntity
import com.habitminer.domain.AppIdentityResolver
import com.habitminer.repository.ContextRepository
import com.habitminer.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

data class HabitUiState(
    val isLoading: Boolean = true,
    val daysOfData: Int = 0,
    val todayScreenTimeMs: Long = 0L,
    val todayUnlocks: Int = 0,
    val todayTopApp: String = "",
    val todayUsageByApp: Map<String, Long> = emptyMap(),
    val discoveredHabits: List<DiscoveredHabitEntity> = emptyList(),
    val todayDeviations: List<DeviationEntity> = emptyList(),
    val overallDeviationScore: Float = 0f,
    val predictions: List<PredictionEngine.Prediction> = emptyList(),
    val predictabilityScore: Float = 0f,
    val baselineStatus: String = "Building baseline...",
    val hasEnoughData: Boolean = false,
    val latestContext: ContextSnapshotEntity? = null,
    val hasUsagePermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val hasRuntimePermissions: Boolean = false,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class HabitViewModel
    @Inject
    constructor(
        application: Application,
        private val contextRepository: ContextRepository,
        private val habitRepository: HabitRepository,
        private val habitEngine: HabitEngine,
        private val predictionEngine: PredictionEngine,
        private val baselineBuilder: BaselineBuilder,
        private val deviationDetector: DeviationDetector,
        private val usageDataCollector: UsageDataCollector,
        private val appIdentityResolver: AppIdentityResolver,
    ) : AndroidViewModel(application) {
        private val _uiState = MutableStateFlow(HabitUiState())
        val uiState: StateFlow<HabitUiState> = _uiState.asStateFlow()
        private var initialCollectionStarted = false

        init {
            checkPermissions()
            observeData()
        }

        fun checkPermissions() {
            val application = getApplication<Application>()
            val appOps = application.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOps.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(),
                        application.packageName,
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(),
                        application.packageName,
                    )
                }
            val hasUsage = mode == AppOpsManager.MODE_ALLOWED
            val hasNotif = HabitNotificationListener.isEnabled(application)

            _uiState.update {
                it.copy(
                    hasUsagePermission = hasUsage,
                    hasNotificationPermission = hasNotif,
                    // Neither raw motion sensor access nor usage history requires a
                    // runtime permission. Notification access remains optional.
                    hasRuntimePermissions = true,
                )
            }

            if (hasUsage && !initialCollectionStarted) {
                initialCollectionStarted = true
                synchronizeUsageAndModel()
            }
        }

        fun loadHistoricalData() {
            if (!_uiState.value.hasUsagePermission) return
            synchronizeUsageAndModel()
        }

        private fun synchronizeUsageAndModel() {
            viewModelScope.launch(Dispatchers.IO) {
                _uiState.update { it.copy(isLoading = true) }
                try {
                    val application = getApplication<Application>()
                    val count = contextRepository.getUsageCount()
                    val lastTimestamp = contextRepository.getLastInsertedUsageTimestamp()
                    val usages =
                        when {
                            count == 0 -> usageDataCollector.collectLast14Days()
                            else -> usageDataCollector.collectUsageSince(
                                lastTimestamp ?: System.currentTimeMillis() - 24 * 60 * 60 * 1000L,
                            )
                        }
                    if (usages.isNotEmpty()) contextRepository.insertAllAppUsage(usages)
                    DataCollectionWorker.schedulePeriodicWork(application)

                    val preferences = application.getSharedPreferences("habitminer_model", Context.MODE_PRIVATE)
                    var labelsChanged = false
                    if (!preferences.getBoolean("stored_labels_resolved", false)) {
                        contextRepository.getPackagesWithFallbackNames().forEach { packageName ->
                            val appName = appIdentityResolver.getAppName(packageName)
                            if (appName != packageName) {
                                contextRepository.updateFallbackAppName(packageName, appName)
                                labelsChanged = true
                            }
                        }
                        preferences.edit().putBoolean("stored_labels_resolved", true).apply()
                    }

                    val revision = "${contextRepository.getUsageRevision()}|${contextRepository.getSnapshotRevision()}"
                    if (labelsChanged || preferences.getString("source_revision", null) != revision) {
                        refreshHabits()
                        preferences.edit()
                            .putString("source_revision", revision)
                            .putInt("days_of_data", _uiState.value.daysOfData)
                            .putFloat("predictability_score", _uiState.value.predictabilityScore)
                            .apply()
                    } else {
                        val days = preferences.getInt("days_of_data", 0)
                        _uiState.update {
                            it.copy(
                                daysOfData = days,
                                hasEnoughData = days >= 5,
                                baselineStatus = if (days >= 5) "Model up to date · $days days" else "Building baseline: $days/5 days",
                                predictabilityScore = preferences.getFloat("predictability_score", 0f),
                            )
                        }
                    }
                } catch (error: Exception) {
                    android.util.Log.e("HabitMiner", "Could not sync usage or update the model", error)
                } finally {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }

        private suspend fun refreshHabits() {
            val startOfDay = getStartOfDay()
            val allUsage = contextRepository.getAllUsage().first()
            val todayUsage = contextRepository.getTodayUsage(startOfDay).first()

            // Baseline
            val contextWindowStart = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
            val snapshots = contextRepository.getSnapshotsSince(contextWindowStart)
            val newBaselines = baselineBuilder.buildBaseline(allUsage, snapshots)
            newBaselines.forEach { habitRepository.insertBaseline(it) }

            // Habits
            val habits = habitEngine.discoverHabits(allUsage)
            habitRepository.deleteAllHabits()
            habits.forEach { habitRepository.insertHabit(it) }

            // Predictability
            val predictability = habitEngine.computePredictabilityScore(allUsage)

            // Deviations
            val deviationsResult = deviationDetector.detectDeviations(todayUsage, newBaselines)
            habitRepository.deleteDeviationsSince(startOfDay)
            deviationsResult.forEach { dev ->
                habitRepository.insertDeviation(
                    DeviationEntity(
                        timestamp = System.currentTimeMillis(),
                        timeBin = dev.timeBin,
                        deviationType = dev.deviationType,
                        description = dev.description,
                        zScore = dev.zScore,
                        normalizedScore = dev.normalizedScore,
                        affectedCategory = dev.affectedCategory,
                    ),
                )
            }

            val daysOfData = baselineBuilder.getDaysOfData(allUsage)
            val hasEnoughData = daysOfData >= 5
            val baselineStatus = if (hasEnoughData) "Baseline built from $daysOfData days" else "Building baseline: $daysOfData/5 days"

            _uiState.update { current ->
                current.copy(
                    daysOfData = daysOfData,
                    hasEnoughData = hasEnoughData,
                    baselineStatus = baselineStatus,
                    predictabilityScore = predictability,
                )
            }
        }

        private fun observeData() {
            viewModelScope.launch {
                val startOfDay = getStartOfDay()

                launch {
                    contextRepository.getTodayUsage(startOfDay).collect { usage ->
                        val (validUsage, totalTime, categories) = withContext(Dispatchers.Default) {
                            val valid = usage.filterNot { appIdentityResolver.isLauncher(it.packageName) }
                            val byApp = valid.groupBy { appIdentityResolver.getAppName(it.packageName) }
                                .mapValues { entry -> entry.value.sumOf { item -> item.durationMs } }
                            Triple(valid, valid.sumOf { it.durationMs }, byApp)
                        }
                        val topCategory = categories.maxByOrNull { it.value }?.key.orEmpty()

                        _uiState.update {
                            it.copy(
                                todayScreenTimeMs = totalTime,
                                todayUsageByApp = categories,
                                todayTopApp = topCategory,
                            )
                        }

                        if (validUsage.isNotEmpty()) {
                            val latest = validUsage.first()
                            val allUsage = contextRepository.getAllUsage().first()
                            val predictions = withContext(Dispatchers.Default) {
                                predictionEngine.predict(allUsage, appIdentityResolver.getAppName(latest.packageName), latest.timeSlot, latest.dayType)
                            }
                            _uiState.update { it.copy(predictions = predictions) }
                        }
                    }
                }

                launch {
                    habitRepository.getAllHabits().collect { habits ->
                        _uiState.update { it.copy(discoveredHabits = habits) }
                    }
                }

                launch {
                    habitRepository.getTodayDeviations(startOfDay).collect { devs ->
                        val overallScore = devs.maxOfOrNull { it.normalizedScore } ?: 0f
                        _uiState.update { it.copy(todayDeviations = devs, overallDeviationScore = overallScore) }
                    }
                }

                launch {
                    contextRepository.getLatestSnapshot().collect { snapshot ->
                        _uiState.update { it.copy(latestContext = snapshot) }
                    }
                }

                launch {
                    contextRepository.getAllUsage().debounce(300).collect { usage ->
                        val days = withContext(Dispatchers.Default) { baselineBuilder.getDaysOfData(usage) }
                        _uiState.update { it.copy(daysOfData = days) }
                    }
                }
            }
        }

        private fun getStartOfDay(): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }
