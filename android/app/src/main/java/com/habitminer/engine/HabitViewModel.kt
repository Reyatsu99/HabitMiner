package com.habitminer.engine

import android.app.AppOpsManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import androidx.compose.runtime.Immutable
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

@Immutable
data class HabitUiState(
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val retentionDays: Int = 90,
    val daysOfData: Int = 0,
    val todayScreenTimeMs: Long = 0L,
    val todayUnlocks: Int = 0,
    val todayTopApp: String = "",
    val todayUsageByApp: ImmutableMap<String, Long> = persistentMapOf(),
    val discoveredHabits: ImmutableList<DiscoveredHabitEntity> = persistentListOf(),
    val todayDeviations: ImmutableList<DeviationEntity> = persistentListOf(),
    val overallDeviationScore: Float = 0f,
    val predictions: ImmutableList<PredictionEngine.Prediction> = persistentListOf(),
    val predictabilityScore: Float = 0f,
    val baselineStatus: String = "Building baseline...",
    val hasEnoughData: Boolean = false,
    val latestContext: ContextSnapshotEntity? = null,
    val hasUsagePermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val hasRuntimePermissions: Boolean = false,
    val expectedScreenTimeMs: Long = 0L,
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
        private val syncMutex = Mutex()

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
            val retentionDays =
                application.getSharedPreferences("habitminer_model", Context.MODE_PRIVATE)
                    .getInt("retention_days", 90).coerceIn(30, 180)
            val collectionEnabled =
                application.getSharedPreferences("habitminer_model", Context.MODE_PRIVATE)
                    .getBoolean("collection_enabled", true)

            _uiState.update {
                it.copy(
                    hasUsagePermission = hasUsage,
                    hasNotificationPermission = hasNotif,
                    // Neither raw motion sensor access nor usage history requires a
                    // runtime permission. Notification access remains optional.
                    hasRuntimePermissions = true,
                    retentionDays = retentionDays,
                )
            }

            if (hasUsage && collectionEnabled && !initialCollectionStarted) {
                initialCollectionStarted = true
                synchronizeUsageAndModel()
            }
        }

        fun loadHistoricalData() {
            if (!_uiState.value.hasUsagePermission) return
            val application = getApplication<Application>()
            application.getSharedPreferences("habitminer_model", Context.MODE_PRIVATE)
                .edit().putBoolean("collection_enabled", true).apply()
            synchronizeUsageAndModel()
        }

        private fun synchronizeUsageAndModel() {
            viewModelScope.launch(Dispatchers.IO) {
                if (!syncMutex.tryLock()) return@launch
                _uiState.update { it.copy(isLoading = true, isSyncing = true) }
                try {
                    val application = getApplication<Application>()
                    val count = contextRepository.getUsageCount()
                    val lastTimestamp = contextRepository.getLastInsertedUsageTimestamp()
                    val usages =
                        when {
                            count == 0 -> usageDataCollector.collectLast14Days()
                            else -> {
                                val launcherPackages = appIdentityResolver.getLauncherPackages()
                                val prevPkg = contextRepository.getLastUsedNonLauncherPackage(launcherPackages)
                                usageDataCollector.collectUsageSince(
                                    lastTimestamp ?: System.currentTimeMillis() - 24 * 60 * 60 * 1000L,
                                    prevPkg,
                                )
                            }
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

                    val revision = contextRepository.getModelRevision(getStartOfDay())
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

                    // Trigger immediate collection for fresh sensor context
                    DataCollectionWorker.runOnce(application)
                } catch (error: Exception) {
                    android.util.Log.e("HabitMiner", "Could not sync usage or update the model", error)
                } finally {
                    syncMutex.unlock()
                    _uiState.update { it.copy(isLoading = false, isSyncing = false) }
                }
            }
        }

        fun setRetentionDays(days: Int) {
            val normalizedDays = days.coerceIn(30, 180)
            val application = getApplication<Application>()
            application.getSharedPreferences("habitminer_model", Context.MODE_PRIVATE)
                .edit().putInt("retention_days", normalizedDays).apply()
            _uiState.update { it.copy(retentionDays = normalizedDays) }
            DataCollectionWorker.runOnce(application)
        }

        fun clearCollectedData() {
            viewModelScope.launch(Dispatchers.IO) {
                contextRepository.clearCollectedData()
                habitRepository.clearModelData()
                val application = getApplication<Application>()
                application.getSharedPreferences("habitminer_model", Context.MODE_PRIVATE).edit()
                    .remove("source_revision")
                    .remove("days_of_data")
                    .remove("predictability_score")
                    .remove("stored_labels_resolved")
                    .putBoolean("collection_enabled", false)
                    .apply()
                _uiState.update {
                    HabitUiState(
                        hasUsagePermission = true,
                        hasRuntimePermissions = true,
                        retentionDays = it.retentionDays,
                        baselineStatus = "Data cleared. Tap Sync Usage to resume collection.",
                    )
                }
            }
        }

        private suspend fun refreshHabits() {
            val startOfDay = getStartOfDay()
            val allUsage = contextRepository.getAllUsage().first()
            val todayUsage = contextRepository.getTodayUsage(startOfDay).first()
            val historicalUsage = allUsage.filter { it.startTime < startOfDay }

            // Baseline
            val contextWindowStart = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
            val snapshots = contextRepository.getSnapshotsSince(contextWindowStart)
            val newBaselines = baselineBuilder.buildBaseline(historicalUsage, snapshots.filter { it.timestamp < startOfDay })
            newBaselines.forEach { habitRepository.insertBaseline(it) }

            // Habits
            val habits = habitEngine.discoverHabits(historicalUsage)
            habitRepository.deleteAllHabits()
            habits.forEach { habitRepository.insertHabit(it) }

            // Predictability
            val predictability = habitEngine.computePredictabilityScore(historicalUsage)

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
                val startOfDayFlow =
                    flow {
                        while (currentCoroutineContext().isActive) {
                            emit(getStartOfDay())
                            // Calculate ms until next midnight
                            val now = Calendar.getInstance()
                            val nextMidnight =
                                Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, 1)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                            val delayMs = nextMidnight.timeInMillis - now.timeInMillis
                            delay(delayMs)
                        }
                    }.distinctUntilChanged()

                launch {
                    startOfDayFlow.collectLatest { startOfDay ->
                        contextRepository.getTodayUsage(startOfDay).collect { usage ->
                            val (validUsage, totalTime, categories) =
                                withContext(Dispatchers.Default) {
                                    val valid = usage.filterNot { appIdentityResolver.isLauncher(it.packageName) }
                                    val byApp =
                                        valid.groupBy { appIdentityResolver.getAppName(it.packageName) }
                                            .mapValues { entry -> entry.value.sumOf { item -> item.durationMs } }
                                    Triple(valid, valid.sumOf { it.durationMs }, byApp)
                                }
                            val topCategory = categories.maxByOrNull { it.value }?.key.orEmpty()

                            _uiState.update {
                                it.copy(
                                    todayScreenTimeMs = totalTime,
                                    todayUsageByApp = categories.toImmutableMap(),
                                    todayTopApp = topCategory,
                                )
                            }

                            if (validUsage.isNotEmpty()) {
                                val latest = validUsage.first()
                                val allUsage = contextRepository.getAllUsage().first()
                                val predictions =
                                    withContext(Dispatchers.Default) {
                                        predictionEngine.predict(
                                            allUsage,
                                            appIdentityResolver.getAppName(latest.packageName),
                                            latest.timeSlot,
                                            latest.dayType,
                                        )
                                    }
                                _uiState.update { it.copy(predictions = predictions.toImmutableList()) }
                            }
                        }
                    }
                }

                launch {
                    habitRepository.getAllHabits().collect { habits ->
                        _uiState.update { it.copy(discoveredHabits = habits.toImmutableList()) }
                    }
                }

                launch {
                    habitRepository.getAllBaselines().collect { baselines ->
                        val cal = Calendar.getInstance()
                        val day = cal.get(Calendar.DAY_OF_WEEK)
                        val dayType = if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) "WEEKEND" else "WEEKDAY"
                        val expected = baselines.filter { it.timeBin.startsWith(dayType) }.sumOf { it.avgScreenTimeMs }
                        _uiState.update { it.copy(expectedScreenTimeMs = expected) }
                    }
                }

                launch {
                    startOfDayFlow.collectLatest { startOfDay ->
                        habitRepository.getTodayDeviations(startOfDay).collect { devs ->
                            val overallScore = devs.maxOfOrNull { it.normalizedScore } ?: 0f
                            _uiState.update { it.copy(todayDeviations = devs.toImmutableList(), overallDeviationScore = overallScore) }
                        }
                    }
                }

                launch {
                    contextRepository.getLatestSnapshot().collect { snapshot ->
                        _uiState.update {
                            it.copy(
                                latestContext = snapshot,
                                todayUnlocks = snapshot?.unlockCount ?: 0,
                            )
                        }
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
