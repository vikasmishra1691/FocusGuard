package com.example.focusguard

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import com.example.focusguard.data.model.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class FocusGuardAccessibilityService : AccessibilityService() {
    private var accessTimeManager: AccessTimeManager? = null
    private var settingsRepository: SettingsRepository? = null
    private var usageStatsHelper: UsageStatsHelper? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var lastPackageName: String? = null
    private var lastEventTime: Long = 0

    companion object {
        private const val TAG = "FocusGuardAccessibility"
        private const val EVENT_DEBOUNCE_TIME = 1000L // 1 second
        private const val USAGE_SYNC_INTERVAL = 30000L // 30 seconds

        var isServiceRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        accessTimeManager = AccessTimeManager(this)
        settingsRepository = SettingsRepository(this)
        usageStatsHelper = UsageStatsHelper(this)
        isServiceRunning = true
        Log.d(TAG, "FocusGuard Accessibility Service created")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        serviceScope.cancel()
        Log.d(TAG, "FocusGuard Accessibility Service destroyed")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        // Handle both window state changes and window content changes
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString() ?: return
        val currentTime = System.currentTimeMillis()

        // Skip system packages and launcher
        if (packageName.startsWith("com.android") ||
            packageName.startsWith("android") ||
            packageName.contains("launcher")) {

            // If we switched to home/system, end all active sessions
            if (packageName.contains("launcher") || packageName == "com.android.systemui") {
                serviceScope.launch {
                    endAllActiveSessions()
                }
            }
            return
        }

        // Debounce rapid events from the same package
        if (packageName == lastPackageName &&
            currentTime - lastEventTime < EVENT_DEBOUNCE_TIME) {
            return
        }

        // If switching to a different app, end previous session
        if (lastPackageName != null && lastPackageName != packageName) {
            serviceScope.launch {
                accessTimeManager?.endSession(lastPackageName!!)
            }
        }

        lastPackageName = packageName
        lastEventTime = currentTime

        Log.d(TAG, "App launched: $packageName")

        // Check if this is a monitored app
        serviceScope.launch {
            try {
                handleAppLaunch(packageName)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling app launch", e)
            }
        }
    }

    private suspend fun handleAppLaunch(packageName: String) {
        val accessTimeManager = this.accessTimeManager ?: return
        val settingsRepository = this.settingsRepository ?: return

        // Check if app is monitored
        val monitoredApp = settingsRepository.getMonitoredApp(packageName)
        if (monitoredApp == null || !monitoredApp.isEnabled) {
            return
        }

        Log.d(TAG, "Monitoring app launch: ${monitoredApp.appName}")

        // First check if usage limit is already exceeded
        val limitExceeded = accessTimeManager.isAppUsageLimitExceeded(packageName)

        if (limitExceeded) {
            Log.d(TAG, "Usage limit exceeded for ${monitoredApp.appName}, blocking immediately")

            // Increment blocked attempts counter
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val existingStats = settingsRepository.getDailyStatsForPackage(packageName, today)
            if (existingStats != null) {
                settingsRepository.updateDailyStats(
                    existingStats.copy(blockedAttempts = existingStats.blockedAttempts + 1)
                )
            }

            // Block the app immediately
            val accessResult = AccessTimeManager.AppAccessResult(
                canAccess = false,
                reason = "You've reached your limit for ${monitoredApp.appName}! Solve a quick math question to earn 5 more minutes.",
                timeRemaining = 0,
                requiresChallenge = true,
                challengeDifficulty = 2
            )
            blockApp(packageName, accessResult)
            return
        }

        // Check other access permissions (session limit, schedule, etc.)
        val accessResult = accessTimeManager.checkAppAccess(packageName)

        if (!accessResult.canAccess) {
            Log.d(TAG, "Blocking app: ${accessResult.reason}")
            blockApp(packageName, accessResult)
        } else {
            Log.d(TAG, "Allowing app access: ${accessResult.reason}")
            accessTimeManager.startSession(packageName)
        }
    }

    private fun blockApp(packageName: String, accessResult: AccessTimeManager.AppAccessResult) {
        val intent = Intent(this, BlockerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                   Intent.FLAG_ACTIVITY_CLEAR_TOP or
                   Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("package_name", packageName)
            putExtra("block_reason", accessResult.reason)
            putExtra("time_remaining", accessResult.timeRemaining)
            putExtra("requires_challenge", accessResult.requiresChallenge)
            putExtra("challenge_difficulty", accessResult.challengeDifficulty)
        }

        startActivity(intent)

        // Force the blocked app to background
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")

        // Start periodic monitoring when service connects
        startPeriodicSessionMonitoring()
        startPeriodicUsageSync()
    }

    private fun startPeriodicUsageSync() {
        serviceScope.launch {
            while (isActive) {
                try {
                    syncUsageStatistics()
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing usage statistics", e)
                }
                delay(USAGE_SYNC_INTERVAL)
            }
        }
    }

    private suspend fun syncUsageStatistics() {
        val usageHelper = usageStatsHelper ?: return
        val settingsRepo = settingsRepository ?: return

        if (!usageHelper.hasUsageStatsPermission()) {
            Log.w(TAG, "Usage stats permission not available")
            return
        }

        // Get all monitored apps
        val monitoredApps = settingsRepo.getMonitoredApps().first()

        monitoredApps.forEach { app ->
            val usageMillis = usageHelper.getAppUsageTime(app.packageName)
            val usageMinutes = TimeUnit.MILLISECONDS.toMinutes(usageMillis).toInt()

            if (usageMinutes > 0) {
                Log.d(TAG, "Syncing ${app.appName}: $usageMinutes minutes")

                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val existingStats = settingsRepo.getDailyStatsForPackage(app.packageName, today)

                if (existingStats != null && existingStats.totalUsageMinutes != usageMinutes) {
                    // Update with real usage data from system
                    settingsRepo.updateDailyStats(
                        existingStats.copy(
                            totalUsageMinutes = usageMinutes,
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                } else if (existingStats == null) {
                    // Create new stats entry
                    settingsRepo.insertDailyStats(
                        DailyUsageStats(
                            id = "${app.packageName}_$today",
                            packageName = app.packageName,
                            date = today,
                            totalUsageMinutes = usageMinutes,
                            blockedAttempts = 0,
                            challengesCompleted = 0,
                            challengesFailed = 0,
                            longestSessionMinutes = 0,
                            sessionsCompleted = 1,
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    fun onAppClosed(packageName: String) {
        serviceScope.launch {
            try {
                accessTimeManager?.endSession(packageName)
                Log.d(TAG, "Session ended for: $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Error ending session for $packageName", e)
            }
        }
    }

    private suspend fun endAllActiveSessions() {
        try {
            accessTimeManager?.endAllActiveSessions()
            Log.d(TAG, "All active sessions ended")
        } catch (e: Exception) {
            Log.e(TAG, "Error ending all sessions", e)
        }
    }

    // Method to manually refresh accessibility service status
    fun updateAccessibilityServiceStatus() {
        // This method is called from PermissionChecker to update service status
        Log.d(TAG, "Accessibility service status updated")
    }

    // Method to check if service is properly configured
    fun isProperlyConfigured(): Boolean {
        return accessTimeManager != null && settingsRepository != null
    }

    // Add periodic session monitoring to enforce limits even during continuous usage
    private fun startPeriodicSessionMonitoring() {
        serviceScope.launch {
            while (isActive) {
                try {
                    monitorActiveSessions()
                } catch (e: Exception) {
                    Log.e(TAG, "Error monitoring sessions", e)
                }
                delay(5000) // Check every 5 seconds
            }
        }
    }

    private suspend fun monitorActiveSessions() {
        val accessTimeManager = this.accessTimeManager ?: return
        val settingsRepository = this.settingsRepository ?: return

        // Get all active sessions
        val activeSessions = accessTimeManager.getActiveSessions()

        for ((packageName, sessionInfo) in activeSessions) {
            // Check if this app has exceeded its limit during the session
            val limitExceeded = accessTimeManager.isAppUsageLimitExceeded(packageName)

            if (limitExceeded) {
                Log.d(TAG, "App $packageName exceeded limit during session, ending session")
                accessTimeManager.endSession(packageName)

                // Note: We can't block here because we don't know if the app is still in foreground
                // The next launch attempt will trigger the block
            }
        }
    }
}
