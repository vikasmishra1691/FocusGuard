package com.example.focusguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.focusguard.data.model.DailyUsageStats
import com.example.focusguard.ui.theme.FocusGuardTheme
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsActivity : ComponentActivity() {
    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsRepository = SettingsRepository(this)

        setContent {
            FocusGuardTheme {
                AnalyticsScreen(
                    onBack = { finish() }
                )
            }
        }
    }

    @Composable
    private fun AnalyticsScreen(onBack: () -> Unit) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayStats by settingsRepository.getDailyStats(today).collectAsState(initial = emptyList())
        val monitoredApps by settingsRepository.getEnabledApps().collectAsState(initial = emptyList())

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Analytics") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overall Stats Card
                item {
                    OverallStatsCard(todayStats)
                }

                // Today's Usage by App
                item {
                    Text(
                        text = "Today's Usage by App",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (todayStats.isEmpty()) {
                    item {
                        Card {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = "No data",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No Usage Data Yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Start using monitored apps to see analytics here.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    items(todayStats.sortedByDescending { it.totalUsageMinutes }) { stats ->
                        val app = monitoredApps.find { it.packageName == stats.packageName }
                        if (app != null) {
                            AppUsageCard(stats, app)
                        }
                    }
                }

                // Weekly Summary (placeholder)
                item {
                    WeeklySummaryCard(todayStats)
                }

                // Achievement Badge (placeholder)
                item {
                    AchievementCard(todayStats)
                }
            }
        }
    }

    @Composable
    private fun OverallStatsCard(todayStats: List<DailyUsageStats>) {
        val totalUsage = todayStats.sumOf { it.totalUsageMinutes }
        val totalBlocks = todayStats.sumOf { it.blockedAttempts }
        val totalChallenges = todayStats.sumOf { it.challengesCompleted }
        val totalFailed = todayStats.sumOf { it.challengesFailed }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Analytics",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Today's Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatColumn("Total Usage", "${totalUsage}m", Icons.Default.Timer)
                    StatColumn("Blocks", "$totalBlocks", Icons.Default.Block)
                    StatColumn("Challenges", "$totalChallenges", Icons.Default.Quiz)
                }

                if (totalChallenges > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val successRate = ((totalChallenges.toFloat() / (totalChallenges + totalFailed)) * 100).toInt()
                    Text(
                        text = "Challenge Success Rate: $successRate%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (successRate >= 70) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    @Composable
    private fun StatColumn(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    @Composable
    private fun AppUsageCard(
        stats: DailyUsageStats,
        app: com.example.focusguard.data.model.MonitoredApp
    ) {
        val usagePercentage = (stats.totalUsageMinutes.toFloat() / app.dailyLimitMinutes.toFloat()).coerceAtMost(1f)

        Card {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App icon placeholder
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = app.appName.take(2).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${stats.totalUsageMinutes}/${app.dailyLimitMinutes} minutes",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Text(
                        text = "${(usagePercentage * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (usagePercentage >= 0.9f) MaterialTheme.colorScheme.error
                               else if (usagePercentage >= 0.7f) Color(0xFFFF9800)
                               else Color(0xFF4CAF50)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = usagePercentage,
                    modifier = Modifier.fillMaxWidth(),
                    color = if (usagePercentage >= 0.9f) MaterialTheme.colorScheme.error
                           else if (usagePercentage >= 0.7f) Color(0xFFFF9800)
                           else Color(0xFF4CAF50)
                )

                if (stats.blockedAttempts > 0 || stats.challengesCompleted > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (stats.blockedAttempts > 0) {
                            Text(
                                text = "${stats.blockedAttempts} blocks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (stats.challengesCompleted > 0) {
                            Text(
                                text = "${stats.challengesCompleted} challenges solved",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun WeeklySummaryCard(todayStats: List<DailyUsageStats>) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Weekly Trends",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "📈 Weekly analytics coming soon! Track your progress over time, identify patterns, and see your improvement streaks.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    @Composable
    private fun AchievementCard(todayStats: List<DailyUsageStats>) {
        val totalBlocks = todayStats.sumOf { it.blockedAttempts }
        val totalChallenges = todayStats.sumOf { it.challengesCompleted }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (totalBlocks >= 5) Color(0xFF4CAF50).copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (totalBlocks >= 5) "🏆" else "🎯",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (totalBlocks >= 5) "Achievement Unlocked!" else "Daily Goal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (totalBlocks >= 5)
                        "Great self-control! You've successfully resisted $totalBlocks distractions today."
                    else
                        "Block 5 app launches today to earn your first achievement!",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
