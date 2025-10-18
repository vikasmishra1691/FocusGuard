package com.example.focusguard.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitored_apps")
data class MonitoredApp(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int = 60, // Default 1 hour
    val sessionLimitMinutes: Int = 15, // Default 15 minutes
    val isEnabled: Boolean = true,
    val iconPath: String? = null
)

@Entity(tableName = "app_usage_sessions")
data class AppUsageSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val wasBlocked: Boolean = false,
    val challengesCompleted: Int = 0,
    val date: String // Format: YYYY-MM-DD
)

@Entity(tableName = "daily_usage_stats")
data class DailyUsageStats(
    @PrimaryKey
    val id: String, // Format: packageName_YYYY-MM-DD
    val packageName: String,
    val date: String,
    val totalUsageMinutes: Int,
    val blockedAttempts: Int,
    val challengesCompleted: Int,
    val challengesFailed: Int,
    val longestSessionMinutes: Int,
    val sessionsCompleted: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "arithmetic_challenges")
data class ArithmeticChallenge(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val question: String,
    val correctAnswer: Int,
    val userAnswer: Int?,
    val isCorrect: Boolean = false,
    val difficultyLevel: Int, // 1-5
    val timeToSolveSeconds: Int,
    val timestamp: Long,
    val timeEarnedMinutes: Int = 0
)

@Entity(tableName = "app_block_schedules")
data class AppBlockSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val startHour: Int, // 0-23
    val startMinute: Int, // 0-59
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeek: String, // Comma separated: "1,2,3,4,5" for weekdays
    val isEnabled: Boolean = true
)

// Add the missing AppInfo class for app selection and management
data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable
)

// New entity to track earned extra time from challenges
@Entity(tableName = "earned_extra_time")
data class EarnedExtraTime(
    @PrimaryKey
    val id: String, // Format: packageName_YYYY-MM-DD
    val packageName: String,
    val date: String,
    val extraMinutesEarned: Int = 0,
    val extraMinutesUsed: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun getRemainingExtraTime(): Int = maxOf(0, extraMinutesEarned - extraMinutesUsed)
}
