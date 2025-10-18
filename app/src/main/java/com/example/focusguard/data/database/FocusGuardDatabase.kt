package com.example.focusguard.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.focusguard.data.model.*
import com.example.focusguard.data.dao.*

@Database(
    entities = [
        MonitoredApp::class,
        AppUsageSession::class,
        DailyUsageStats::class,
        ArithmeticChallenge::class,
        AppBlockSchedule::class,
        EarnedExtraTime::class
    ],
    version = 3,
    exportSchema = false
)
abstract class FocusGuardDatabase : RoomDatabase() {
    abstract fun monitoredAppDao(): MonitoredAppDao
    abstract fun appUsageSessionDao(): AppUsageSessionDao
    abstract fun dailyUsageStatsDao(): DailyUsageStatsDao
    abstract fun arithmeticChallengeDao(): ArithmeticChallengeDao
    abstract fun appBlockScheduleDao(): AppBlockScheduleDao
    abstract fun earnedExtraTimeDao(): EarnedExtraTimeDao

    companion object {
        @Volatile
        private var INSTANCE: FocusGuardDatabase? = null

        fun getDatabase(context: Context): FocusGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FocusGuardDatabase::class.java,
                    "focusguard_database"
                )
                .fallbackToDestructiveMigration() // Simplified for development - will recreate DB on schema change
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
