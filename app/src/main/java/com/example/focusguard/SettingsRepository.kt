package com.example.focusguard

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.focusguard.data.database.FocusGuardDatabase
import com.example.focusguard.data.model.*
import com.example.focusguard.utils.PermissionChecker
import kotlinx.coroutines.flow.first

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val database = FocusGuardDatabase.getDatabase(context)
    private val permissionChecker = PermissionChecker(context)

    companion object {
        val CHALLENGE_DIFFICULTY = intPreferencesKey("challenge_difficulty")
        val DEFAULT_TIME_EARNED = intPreferencesKey("default_time_earned")
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val FIRST_TIME_SETUP = booleanPreferencesKey("first_time_setup")
    }

    // Settings Flow - now checks real permissions
    val settings: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            accessibilityServiceEnabled = permissionChecker.isAccessibilityServiceEnabled(),
            usageStatsPermission = permissionChecker.hasUsageStatsPermission(),
            overlayPermission = permissionChecker.hasOverlayPermission(),
            challengeDifficulty = preferences[CHALLENGE_DIFFICULTY] ?: 2,
            defaultTimeEarned = preferences[DEFAULT_TIME_EARNED] ?: 10,
            notificationEnabled = preferences[NOTIFICATION_ENABLED] ?: true,
            isFirstTimeSetup = preferences[FIRST_TIME_SETUP] ?: true
        )
    }

    suspend fun setFirstTimeSetupComplete() {
        context.dataStore.edit { preferences ->
            preferences[FIRST_TIME_SETUP] = false
        }
    }

    suspend fun updateChallengeDifficulty(difficulty: Int) {
        context.dataStore.edit { preferences ->
            preferences[CHALLENGE_DIFFICULTY] = difficulty
        }
    }

    // Monitored Apps
    fun getMonitoredApps(): Flow<List<MonitoredApp>> = database.monitoredAppDao().getAllApps()

    fun getEnabledApps(): Flow<List<MonitoredApp>> = database.monitoredAppDao().getEnabledApps()

    suspend fun addMonitoredApp(app: MonitoredApp) {
        database.monitoredAppDao().insertApp(app)
    }

    suspend fun updateMonitoredApp(app: MonitoredApp) {
        database.monitoredAppDao().updateApp(app)
    }

    suspend fun removeMonitoredApp(packageName: String) {
        val app = database.monitoredAppDao().getApp(packageName)
        if (app != null) {
            database.monitoredAppDao().deleteApp(app)
        }
    }

    suspend fun setAppEnabled(packageName: String, enabled: Boolean) {
        database.monitoredAppDao().setAppEnabled(packageName, enabled)
    }

    suspend fun getMonitoredApp(packageName: String): MonitoredApp? {
        return database.monitoredAppDao().getApp(packageName)
    }

    // Usage Statistics
    suspend fun recordUsageSession(session: AppUsageSession) {
        database.appUsageSessionDao().insertSession(session)
        updateDailyStats(session)
    }

    private suspend fun updateDailyStats(session: AppUsageSession) {
        val statsId = "${session.packageName}_${session.date}"
        val existingStats = database.dailyUsageStatsDao().getStatsForDate(session.date).first()
            .find { it.packageName == session.packageName }

        if (existingStats != null) {
            val updatedStats = existingStats.copy(
                totalUsageMinutes = existingStats.totalUsageMinutes + session.durationMinutes,
                challengesCompleted = existingStats.challengesCompleted + session.challengesCompleted,
                longestSessionMinutes = maxOf(existingStats.longestSessionMinutes, session.durationMinutes),
                blockedAttempts = if (session.wasBlocked) existingStats.blockedAttempts + 1 else existingStats.blockedAttempts
            )
            database.dailyUsageStatsDao().insertStats(updatedStats)
        } else {
            val newStats = DailyUsageStats(
                id = statsId,
                packageName = session.packageName,
                date = session.date,
                totalUsageMinutes = session.durationMinutes,
                blockedAttempts = if (session.wasBlocked) 1 else 0,
                challengesCompleted = session.challengesCompleted,
                challengesFailed = 0,
                longestSessionMinutes = session.durationMinutes
            )
            database.dailyUsageStatsDao().insertStats(newStats)
        }
    }

    fun getDailyStats(date: String): Flow<List<DailyUsageStats>> {
        return database.dailyUsageStatsDao().getStatsForDate(date)
    }

    suspend fun getDailyStatsForPackage(packageName: String, date: String): DailyUsageStats? {
        return database.dailyUsageStatsDao().getStatsForDate(date).first()
            .find { it.packageName == packageName }
    }

    suspend fun updateDailyStats(stats: DailyUsageStats) {
        database.dailyUsageStatsDao().insertStats(stats)
    }

    suspend fun insertDailyStats(stats: DailyUsageStats) {
        database.dailyUsageStatsDao().insertStats(stats)
    }

    // Challenge Management
    suspend fun recordChallenge(challenge: ArithmeticChallenge) {
        database.arithmeticChallengeDao().insertChallenge(challenge)
    }

    suspend fun getRecentChallengesForApp(packageName: String): List<ArithmeticChallenge> {
        return database.arithmeticChallengeDao().getRecentChallengesForApp(packageName).first()
    }

    // Earned Extra Time Management
    suspend fun getEarnedExtraTime(packageName: String, date: String): EarnedExtraTime? {
        return database.earnedExtraTimeDao().getExtraTimeForDate(packageName, date)
    }

    suspend fun addEarnedExtraTime(packageName: String, date: String, minutes: Int) {
        val id = "${packageName}_$date"
        val existing = database.earnedExtraTimeDao().getExtraTime(id)

        if (existing != null) {
            database.earnedExtraTimeDao().addExtraMinutes(id, minutes)
        } else {
            database.earnedExtraTimeDao().insertExtraTime(
                EarnedExtraTime(
                    id = id,
                    packageName = packageName,
                    date = date,
                    extraMinutesEarned = minutes
                )
            )
        }
    }

    suspend fun useExtraTime(packageName: String, date: String, minutes: Int) {
        val id = "${packageName}_$date"
        database.earnedExtraTimeDao().useExtraMinutes(id, minutes)
    }

    suspend fun getRemainingExtraTime(packageName: String, date: String): Int {
        val extraTime = database.earnedExtraTimeDao().getExtraTimeForDate(packageName, date)
        return extraTime?.getRemainingExtraTime() ?: 0
    }
}

data class AppSettings(
    val accessibilityServiceEnabled: Boolean = false,
    val usageStatsPermission: Boolean = false,
    val overlayPermission: Boolean = false,
    val challengeDifficulty: Int = 2,
    val defaultTimeEarned: Int = 10,
    val notificationEnabled: Boolean = true,
    val isFirstTimeSetup: Boolean = true
)
