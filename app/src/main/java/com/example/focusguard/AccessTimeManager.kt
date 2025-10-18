package com.example.focusguard

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min
import com.example.focusguard.data.model.*

class AccessTimeManager(private val context: Context) {
    private val settingsRepository = SettingsRepository(context)
    private val packageManager = context.packageManager
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val usageStatsHelper = UsageStatsHelper(context)

    // Track current sessions
    private val activeSessions = mutableMapOf<String, SessionInfo>()

    data class SessionInfo(
        val startTime: Long,
        var totalUsed: Int = 0,
        var challengesCompleted: Int = 0,
        var lastChallengeTime: Long = 0
    )

    data class AppAccessResult(
        val canAccess: Boolean,
        val reason: String,
        val timeRemaining: Int = 0,
        val requiresChallenge: Boolean = false,
        val challengeDifficulty: Int = 1
    )

    /**
     * Helper method to check if app usage limit is exceeded
     * Returns true if usage time >= limit + any earned extra time
     */
    suspend fun isAppUsageLimitExceeded(packageName: String): Boolean {
        val monitoredApp = settingsRepository.getMonitoredApp(packageName) ?: return false

        if (!monitoredApp.isEnabled) {
            return false
        }

        val today = dateFormat.format(Date())

        // Get current usage from UsageStatsManager (real-time data)
        val currentUsageMillis = usageStatsHelper.getAppUsageTime(packageName)
        val currentUsageMinutes = (currentUsageMillis / 60000).toInt()

        // Get earned extra time
        val extraTime = settingsRepository.getRemainingExtraTime(packageName, today)

        // Calculate effective limit (base limit + remaining extra time)
        val effectiveLimit = monitoredApp.dailyLimitMinutes + extraTime

        val isExceeded = currentUsageMinutes >= effectiveLimit

        android.util.Log.d(
            "AccessTimeManager",
            "Limit check for $packageName: usage=$currentUsageMinutes, limit=${monitoredApp.dailyLimitMinutes}, " +
                    "extraTime=$extraTime, effectiveLimit=$effectiveLimit, exceeded=$isExceeded"
        )

        return isExceeded
    }

    suspend fun checkAppAccess(packageName: String): AppAccessResult {
        val monitoredApp = settingsRepository.getMonitoredApp(packageName)
            ?: return AppAccessResult(true, "App not monitored")

        if (!monitoredApp.isEnabled) {
            return AppAccessResult(true, "Monitoring disabled")
        }

        val today = dateFormat.format(Date())

        // Use real-time usage from UsageStatsManager
        val currentUsageMillis = usageStatsHelper.getAppUsageTime(packageName)
        val currentUsageMinutes = (currentUsageMillis / 60000).toInt()

        // Get earned extra time
        val extraTime = settingsRepository.getRemainingExtraTime(packageName, today)

        // Calculate effective limit and remaining time
        val effectiveLimit = monitoredApp.dailyLimitMinutes + extraTime
        val dailyRemaining = effectiveLimit - currentUsageMinutes

        android.util.Log.d(
            "AccessTimeManager",
            "Access check for $packageName: usage=$currentUsageMinutes, limit=${monitoredApp.dailyLimitMinutes}, " +
                    "extra=$extraTime, remaining=$dailyRemaining"
        )

        // Check daily limit (including extra time)
        if (dailyRemaining <= 0) {
            return AppAccessResult(
                canAccess = false,
                reason = "Daily limit of ${monitoredApp.dailyLimitMinutes} minutes exceeded",
                timeRemaining = 0,
                requiresChallenge = true,
                challengeDifficulty = calculateChallengeDifficulty(packageName)
            )
        }

        // Check session limit
        val currentSession = activeSessions[packageName]
        if (currentSession != null) {
            val sessionUsage = (System.currentTimeMillis() - currentSession.startTime) / 60000
            if (sessionUsage >= monitoredApp.sessionLimitMinutes) {
                return AppAccessResult(
                    canAccess = false,
                    reason = "Session limit of ${monitoredApp.sessionLimitMinutes} minutes exceeded",
                    timeRemaining = dailyRemaining,
                    requiresChallenge = true,
                    challengeDifficulty = calculateChallengeDifficulty(packageName)
                )
            }
        }

        // Check scheduled blocks
        if (isInScheduledBlock(packageName)) {
            return AppAccessResult(
                canAccess = false,
                reason = "App blocked by schedule",
                timeRemaining = dailyRemaining,
                requiresChallenge = true,
                challengeDifficulty = calculateChallengeDifficulty(packageName)
            )
        }

        return AppAccessResult(
            canAccess = true,
            reason = "Access granted",
            timeRemaining = dailyRemaining
        )
    }

    fun startSession(packageName: String) {
        activeSessions[packageName] = SessionInfo(System.currentTimeMillis())
        android.util.Log.d("AccessTimeManager", "Started session for: $packageName")
    }

    suspend fun endSession(packageName: String) {
        val session = activeSessions.remove(packageName) ?: return
        val durationMinutes = ((System.currentTimeMillis() - session.startTime) / 60000).toInt()

        android.util.Log.d("AccessTimeManager", "Ending session for: $packageName, duration: ${durationMinutes}min")

        if (durationMinutes > 0) {
            val today = dateFormat.format(Date())
            val usageSession = AppUsageSession(
                packageName = packageName,
                startTime = session.startTime,
                endTime = System.currentTimeMillis(),
                durationMinutes = durationMinutes,
                challengesCompleted = session.challengesCompleted,
                date = today
            )
            settingsRepository.recordUsageSession(usageSession)
            android.util.Log.d("AccessTimeManager", "Recorded usage session: $packageName - ${durationMinutes}min")
        }
    }

    suspend fun endAllActiveSessions() {
        android.util.Log.d("AccessTimeManager", "Ending all active sessions (${activeSessions.size} sessions)")

        // Create a copy of the keys to avoid concurrent modification
        val sessionsToEnd = activeSessions.keys.toList()

        for (packageName in sessionsToEnd) {
            endSession(packageName)
        }
    }

    suspend fun grantExtraTime(packageName: String, challenge: ArithmeticChallenge): Int {
        val session = activeSessions[packageName] ?: SessionInfo(System.currentTimeMillis())
        session.challengesCompleted++
        session.lastChallengeTime = System.currentTimeMillis()
        activeSessions[packageName] = session

        settingsRepository.recordChallenge(challenge)

        // Time earned decreases with each challenge in the session
        val baseTime = settingsRepository.settings.first().defaultTimeEarned
        val timeEarned = maxOf(5, baseTime - (session.challengesCompleted - 1) * 2)

        return timeEarned
    }

    private suspend fun calculateChallengeDifficulty(packageName: String): Int {
        val session = activeSessions[packageName]
        val baseDifficulty = settingsRepository.settings.first().challengeDifficulty

        return if (session != null && session.challengesCompleted > 0) {
            // Increase difficulty with each challenge in the session
            min(5, baseDifficulty + session.challengesCompleted)
        } else {
            baseDifficulty
        }
    }

    private suspend fun isInScheduledBlock(packageName: String): Boolean {
        // Get current time
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        val database = settingsRepository.getMonitoredApp(packageName) ?: return false

        // For now, return false - schedule blocking will be implemented in a future update
        // This would check against AppBlockSchedule entries
        return false
    }

    suspend fun getTodayUsage(packageName: String): Int {
        val today = dateFormat.format(Date())
        val todayStats = settingsRepository.getDailyStats(today).first()
            .find { it.packageName == packageName }
        return todayStats?.totalUsageMinutes ?: 0
    }

    suspend fun getRemainingTime(packageName: String): Int {
        val monitoredApp = settingsRepository.getMonitoredApp(packageName) ?: return Int.MAX_VALUE
        val todayUsage = getTodayUsage(packageName)
        return maxOf(0, monitoredApp.dailyLimitMinutes - todayUsage)
    }

    fun getActiveSessionTime(packageName: String): Long {
        val session = activeSessions[packageName] ?: return 0
        return System.currentTimeMillis() - session.startTime
    }

    suspend fun getInstalledSocialMediaApps(): List<AppInfo> {
        val socialMediaPackages = listOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically", // TikTok
            "com.facebook.katana",
            "com.twitter.android",
            "com.snapchat.android",
            "com.reddit.frontpage",
            "com.google.android.youtube",
            "com.facebook.orca", // Messenger
            "com.whatsapp",
            "com.linkedin.android",
            "com.pinterest",
            "com.tumblr",
            "com.discord",
            "com.spotify.music",
            "com.netflix.mediaclient"
        )

        return socialMediaPackages.mapNotNull { packageName ->
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                AppInfo(
                    packageName = packageName,
                    appName = appName,
                    icon = packageManager.getApplicationIcon(appInfo)
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    // Add method to check if any monitored app is currently being used
    fun getCurrentlyUsedApp(): String? {
        return activeSessions.keys.firstOrNull()
    }

    // Add method to get all active sessions for debugging
    fun getActiveSessions(): Map<String, SessionInfo> {
        return activeSessions.toMap()
    }

    // Add method to force refresh usage statistics
    suspend fun refreshUsageStats() {
        // End all sessions and immediately restart them to update stats
        val currentSessions = activeSessions.keys.toList()
        for (packageName in currentSessions) {
            endSession(packageName)
            // Restart session if app is still running
            startSession(packageName)
        }
    }
}
