package com.habitminer.repository

import com.habitminer.data.BaselineDao
import com.habitminer.data.BaselineEntity
import com.habitminer.data.DeviationDao
import com.habitminer.data.DeviationEntity
import com.habitminer.data.DiscoveredHabitEntity
import com.habitminer.data.HabitDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepository
    @Inject
    constructor(
        private val habitDao: HabitDao,
        private val baselineDao: BaselineDao,
        private val deviationDao: DeviationDao,
    ) {
        fun getAllHabits(): Flow<List<DiscoveredHabitEntity>> = habitDao.getAllHabits()

        suspend fun getHabitsForTimeBin(
            dayType: String,
            timeSlot: String,
        ): List<DiscoveredHabitEntity> {
            return habitDao.getHabitsForTimeBin(dayType, timeSlot)
        }

        fun getRecentDeviations(limit: Int = 20): Flow<List<DeviationEntity>> = deviationDao.getRecentDeviations(limit)

        fun getTodayDeviations(startOfDayMs: Long): Flow<List<DeviationEntity>> = deviationDao.getTodayDeviations(startOfDayMs)

        suspend fun getBaseline(timeBin: String): BaselineEntity? = baselineDao.getBaseline(timeBin)

        fun getAllBaselines(): Flow<List<BaselineEntity>> = baselineDao.getAllBaselines()

        suspend fun insertHabit(habit: DiscoveredHabitEntity) = habitDao.insertOrUpdate(habit)

        suspend fun deleteAllHabits() = habitDao.deleteAll()

        suspend fun insertBaseline(baseline: BaselineEntity) = baselineDao.insertOrUpdate(baseline)

        suspend fun insertDeviation(deviation: DeviationEntity) = deviationDao.insert(deviation)

        suspend fun deleteDeviationsSince(startOfDayMs: Long) = deviationDao.deleteSince(startOfDayMs)
    }
