package com.example.focusguard

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import com.example.focusguard.data.model.DailyUsageStats
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Background service that tracks app usage using UsageStatsManager
 */
class UsageTrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var usageStatsHelper: UsageStatsHelper
    private lateinit var settingsRepository: SettingsRepository
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    companion object {
        private const val TAG = "UsageTrackingService"
        private const val UPDATE_INTERVAL = 30000L // Update every 30 seconds
    }

    override fun onCreate() {
        super.onCreate()
        usageStatsHelper = UsageStatsHelper(this)
        settingsRepository = SettingsRepository(this)
        Log.d(TAG, "Usage Tracking Service created")

        startUsageTracking()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "Usage Tracking Service destroyed")
    }

    private fun startUsageTracking() {
        serviceScope.launch {
            while (isActive) {
                try {
                    updateUsageStatistics()
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating usage statistics", e)
                }
                delay(UPDATE_INTERVAL)
            }
        }
    }

    private suspend fun updateUsageStatistics() {
        if (!usageStatsHelper.hasUsageStatsPermission()) {
            Log.w(TAG, "Usage stats permission not granted, skipping update")
            return
        }

        // Get all monitored apps - collect once
        val monitoredApps = settingsRepository.getMonitoredApps().first()

        if (monitoredApps.isEmpty()) {
            Log.d(TAG, "No monitored apps, skipping update")
            return
        }

        val packageNames = monitoredApps.map { it.packageName }
        Log.d(TAG, "Updating usage for ${packageNames.size} apps")

        // Get usage stats for all monitored apps
        val usageMap = usageStatsHelper.getAllAppsUsageToday(packageNames)

        val today = dateFormat.format(Date())

        // Update database with real usage data
        usageMap.forEach { (packageName, usageMillis) ->
            val usageMinutes = TimeUnit.MILLISECONDS.toMinutes(usageMillis).toInt()

            if (usageMinutes > 0) {
                Log.d(TAG, "Updating $packageName: $usageMinutes minutes")

                // Create or update daily stats
                val existingStats = settingsRepository.getDailyStatsForPackage(packageName, today)

                if (existingStats != null) {
                    // Update existing stats
                    settingsRepository.updateDailyStats(
                        existingStats.copy(
                            totalUsageMinutes = usageMinutes,
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                } else {
                    // Create new stats entry
                    val newStats = DailyUsageStats(
                        id = "${packageName}_$today",
                        packageName = packageName,
                        date = today,
                        totalUsageMinutes = usageMinutes,
                        blockedAttempts = 0,
                        challengesCompleted = 0,
                        challengesFailed = 0,
                        longestSessionMinutes = 0,
                        sessionsCompleted = 1,
                        lastUpdated = System.currentTimeMillis()
                    )
                    settingsRepository.insertDailyStats(newStats)
                }
            }
        }
    }
}
