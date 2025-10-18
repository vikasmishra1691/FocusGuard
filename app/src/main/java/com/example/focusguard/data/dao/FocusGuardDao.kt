package com.example.focusguard.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.focusguard.data.model.*

@Dao
interface MonitoredAppDao {
    @Query("SELECT * FROM monitored_apps WHERE isEnabled = 1")
    fun getEnabledApps(): Flow<List<MonitoredApp>>

    @Query("SELECT * FROM monitored_apps")
    fun getAllApps(): Flow<List<MonitoredApp>>

    @Query("SELECT * FROM monitored_apps WHERE packageName = :packageName")
    suspend fun getApp(packageName: String): MonitoredApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: MonitoredApp)

    @Update
    suspend fun updateApp(app: MonitoredApp)

    @Delete
    suspend fun deleteApp(app: MonitoredApp)

    @Query("UPDATE monitored_apps SET isEnabled = :enabled WHERE packageName = :packageName")
    suspend fun setAppEnabled(packageName: String, enabled: Boolean)
}

@Dao
interface AppUsageSessionDao {
    @Query("SELECT * FROM app_usage_sessions WHERE date = :date ORDER BY startTime DESC")
    fun getSessionsForDate(date: String): Flow<List<AppUsageSession>>

    @Query("SELECT * FROM app_usage_sessions WHERE packageName = :packageName AND date = :date")
    suspend fun getSessionsForAppAndDate(packageName: String, date: String): List<AppUsageSession>

    @Insert
    suspend fun insertSession(session: AppUsageSession)

    @Update
    suspend fun updateSession(session: AppUsageSession)

    @Query("DELETE FROM app_usage_sessions WHERE date < :cutoffDate")
    suspend fun deleteOldSessions(cutoffDate: String)
}

@Dao
interface DailyUsageStatsDao {
    @Query("SELECT * FROM daily_usage_stats WHERE date = :date")
    fun getStatsForDate(date: String): Flow<List<DailyUsageStats>>

    @Query("SELECT * FROM daily_usage_stats WHERE packageName = :packageName ORDER BY date DESC LIMIT 30")
    fun getRecentStatsForApp(packageName: String): Flow<List<DailyUsageStats>>

    @Query("SELECT * FROM daily_usage_stats WHERE date >= :startDate AND date <= :endDate")
    fun getStatsForDateRange(startDate: String, endDate: String): Flow<List<DailyUsageStats>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: DailyUsageStats)

    @Query("DELETE FROM daily_usage_stats WHERE date < :cutoffDate")
    suspend fun deleteOldStats(cutoffDate: String)
}

@Dao
interface ArithmeticChallengeDao {
    @Query("SELECT * FROM arithmetic_challenges WHERE packageName = :packageName ORDER BY timestamp DESC LIMIT 10")
    fun getRecentChallengesForApp(packageName: String): Flow<List<ArithmeticChallenge>>

    @Query("SELECT COUNT(*) FROM arithmetic_challenges WHERE packageName = :packageName AND timestamp >= :startTime AND isCorrect = 1")
    suspend fun getCorrectChallengesCount(packageName: String, startTime: Long): Int

    @Query("SELECT AVG(difficultyLevel) FROM arithmetic_challenges WHERE packageName = :packageName AND timestamp >= :startTime AND isCorrect = 1")
    suspend fun getAverageDifficulty(packageName: String, startTime: Long): Double?

    @Insert
    suspend fun insertChallenge(challenge: ArithmeticChallenge)

    @Query("DELETE FROM arithmetic_challenges WHERE timestamp < :cutoffTime")
    suspend fun deleteOldChallenges(cutoffTime: Long)
}

@Dao
interface AppBlockScheduleDao {
    @Query("SELECT * FROM app_block_schedules WHERE packageName = :packageName AND isEnabled = 1")
    suspend fun getActiveSchedulesForApp(packageName: String): List<AppBlockSchedule>

    @Query("SELECT * FROM app_block_schedules")
    fun getAllSchedules(): Flow<List<AppBlockSchedule>>

    @Insert
    suspend fun insertSchedule(schedule: AppBlockSchedule)

    @Update
    suspend fun updateSchedule(schedule: AppBlockSchedule)

    @Delete
    suspend fun deleteSchedule(schedule: AppBlockSchedule)
}

@Dao
interface EarnedExtraTimeDao {
    @Query("SELECT * FROM earned_extra_time WHERE id = :id")
    suspend fun getExtraTime(id: String): EarnedExtraTime?

    @Query("SELECT * FROM earned_extra_time WHERE packageName = :packageName AND date = :date")
    suspend fun getExtraTimeForDate(packageName: String, date: String): EarnedExtraTime?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtraTime(extraTime: EarnedExtraTime)

    @Update
    suspend fun updateExtraTime(extraTime: EarnedExtraTime)

    @Query("UPDATE earned_extra_time SET extraMinutesEarned = extraMinutesEarned + :minutes WHERE id = :id")
    suspend fun addExtraMinutes(id: String, minutes: Int)

    @Query("UPDATE earned_extra_time SET extraMinutesUsed = extraMinutesUsed + :minutes WHERE id = :id")
    suspend fun useExtraMinutes(id: String, minutes: Int)

    @Query("DELETE FROM earned_extra_time WHERE date < :cutoffDate")
    suspend fun deleteOldRecords(cutoffDate: String)
}
