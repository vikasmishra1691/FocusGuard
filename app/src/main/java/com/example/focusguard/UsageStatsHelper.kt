package com.example.focusguard

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Helper class for tracking app usage using Android's UsageStatsManager API
 */
class UsageStatsHelper(private val context: Context) {

    companion object {
        private const val TAG = "UsageStatsHelper"
    }

    private val usageStatsManager: UsageStatsManager? by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    }

    /**
     * Check if the app has Usage Access permission
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps?.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps?.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }

        val hasPermission = mode == AppOpsManager.MODE_ALLOWED
        Log.d(TAG, "Usage stats permission status: $hasPermission")
        return hasPermission
    }

    /**
     * Open the Usage Access settings screen
     */
    fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
        Log.d(TAG, "Opened Usage Access Settings")
    }

    /**
     * Get the total usage time for a specific app today (in milliseconds)
     */
    fun getAppUsageTime(packageName: String): Long {
        if (!hasUsageStatsPermission()) {
            Log.w(TAG, "Usage stats permission not granted")
            return 0L
        }

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        Log.d(TAG, "Querying usage stats for $packageName from $startTime to $endTime")

        val usageStats = usageStatsManager?.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        if (usageStats.isNullOrEmpty()) {
            Log.w(TAG, "No usage stats available. User may need to grant permission.")
            return 0L
        }

        val appUsage = usageStats.find { it.packageName == packageName }
        val totalTime = appUsage?.totalTimeInForeground ?: 0L

        Log.d(TAG, "Usage for $packageName: ${TimeUnit.MILLISECONDS.toMinutes(totalTime)} minutes (${totalTime}ms)")

        return totalTime
    }

    /**
     * Get usage time for a specific app in a custom time range (in milliseconds)
     */
    fun getAppUsageTime(packageName: String, startTime: Long, endTime: Long): Long {
        if (!hasUsageStatsPermission()) {
            Log.w(TAG, "Usage stats permission not granted")
            return 0L
        }

        Log.d(TAG, "Querying usage stats for $packageName from $startTime to $endTime")

        val usageStats = usageStatsManager?.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        if (usageStats.isNullOrEmpty()) {
            Log.w(TAG, "No usage stats available")
            return 0L
        }

        val totalTime = usageStats
            .filter { it.packageName == packageName }
            .sumOf { it.totalTimeInForeground }

        Log.d(TAG, "Usage for $packageName: ${TimeUnit.MILLISECONDS.toMinutes(totalTime)} minutes")

        return totalTime
    }

    /**
     * Get usage stats for all monitored apps today
     */
    fun getAllAppsUsageToday(packageNames: List<String>): Map<String, Long> {
        if (!hasUsageStatsPermission()) {
            Log.w(TAG, "Usage stats permission not granted")
            return emptyMap()
        }

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        Log.d(TAG, "Querying usage stats for ${packageNames.size} apps")

        val usageStats = usageStatsManager?.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: emptyList()

        if (usageStats.isEmpty()) {
            Log.w(TAG, "No usage stats available")
            return emptyMap()
        }

        val result = packageNames.associateWith { packageName ->
            val usage = usageStats.find { it.packageName == packageName }?.totalTimeInForeground ?: 0L
            Log.d(TAG, "$packageName: ${TimeUnit.MILLISECONDS.toMinutes(usage)} minutes")
            usage
        }

        return result
    }

    /**
     * Get the currently running foreground app
     */
    fun getCurrentForegroundApp(): String? {
        if (!hasUsageStatsPermission()) {
            return null
        }

        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.SECONDS.toMillis(10) // Last 10 seconds

        val usageStats = usageStatsManager?.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        if (usageStats.isNullOrEmpty()) {
            return null
        }

        // Find the app with the most recent timestamp
        val currentApp = usageStats.maxByOrNull { it.lastTimeUsed }

        Log.d(TAG, "Current foreground app: ${currentApp?.packageName}")
        return currentApp?.packageName
    }

    /**
     * Get detailed usage breakdown for debugging
     */
    fun getDetailedUsageStats(): List<UsageStatDetail> {
        if (!hasUsageStatsPermission()) {
            Log.w(TAG, "Usage stats permission not granted")
            return emptyList()
        }

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageStats = usageStatsManager?.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: return emptyList()

        Log.d(TAG, "Total apps with usage data: ${usageStats.size}")

        return usageStats
            .filter { it.totalTimeInForeground > 0 }
            .map { stats ->
                UsageStatDetail(
                    packageName = stats.packageName,
                    totalTimeInForeground = stats.totalTimeInForeground,
                    lastTimeUsed = stats.lastTimeUsed,
                    firstTimeStamp = stats.firstTimeStamp
                ).also {
                    Log.d(TAG, "App: ${it.packageName}, Time: ${TimeUnit.MILLISECONDS.toMinutes(it.totalTimeInForeground)}min")
                }
            }
            .sortedByDescending { it.totalTimeInForeground }
    }

    data class UsageStatDetail(
        val packageName: String,
        val totalTimeInForeground: Long,
        val lastTimeUsed: Long,
        val firstTimeStamp: Long
    )
}

