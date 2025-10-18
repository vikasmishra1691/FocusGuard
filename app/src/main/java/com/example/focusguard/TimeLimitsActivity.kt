package com.example.focusguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.focusguard.data.model.MonitoredApp
import com.example.focusguard.ui.theme.FocusGuardTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class TimeLimitsActivity : ComponentActivity() {
    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsRepository = SettingsRepository(this)

        setContent {
            FocusGuardTheme {
                TimeLimitsScreen(
                    onBack = { finish() }
                )
            }
        }
    }

    @Composable
    private fun TimeLimitsScreen(onBack: () -> Unit) {
        val monitoredApps by settingsRepository.getMonitoredApps().collectAsState(initial = emptyList())
        val enabledApps = monitoredApps.filter { it.isEnabled }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Time Limits") },
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
                item {
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
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Timer",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Configure Time Limits",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Set daily and session limits for each monitored app. Daily limits control total usage per day, while session limits control continuous usage time.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                if (enabledApps.isEmpty()) {
                    item {
                        Card {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No Apps Selected",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Go to 'Select Apps' to choose which social media apps you want to monitor first.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    items(enabledApps) { app ->
                        AppTimeLimitCard(
                            app = app,
                            onUpdateApp = { updatedApp ->
                                lifecycleScope.launch {
                                    settingsRepository.updateMonitoredApp(updatedApp)
                                }
                            }
                        )
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "💡 Recommendations",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "• Start with 30-60 minutes daily limits\n" +
                                            "• Keep session limits at 10-15 minutes\n" +
                                            "• Adjust based on your usage patterns\n" +
                                            "• Lower limits = more challenge prompts",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AppTimeLimitCard(
        app: MonitoredApp,
        onUpdateApp: (MonitoredApp) -> Unit
    ) {
        var dailyLimit by remember { mutableFloatStateOf(app.dailyLimitMinutes.toFloat()) }
        var sessionLimit by remember { mutableFloatStateOf(app.sessionLimitMinutes.toFloat()) }
        var showDailySlider by remember { mutableStateOf(false) }
        var showSessionSlider by remember { mutableStateOf(false) }

        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // App Header
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
                            text = "Tap to adjust limits",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = app.isEnabled,
                        onCheckedChange = { enabled ->
                            onUpdateApp(app.copy(isEnabled = enabled))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Daily Limit Section
                Card(
                    onClick = { showDailySlider = !showDailySlider },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daily Limit",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${dailyLimit.roundToInt()} minutes",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (showDailySlider) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(
                                value = dailyLimit,
                                onValueChange = { dailyLimit = it },
                                onValueChangeFinished = {
                                    onUpdateApp(app.copy(dailyLimitMinutes = dailyLimit.roundToInt()))
                                },
                                valueRange = 15f..240f, // 15 minutes to 4 hours
                                steps = 44 // 5-minute increments
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "15m",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "4h",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Session Limit Section
                Card(
                    onClick = { showSessionSlider = !showSessionSlider },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Session Limit",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${sessionLimit.roundToInt()} minutes",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (showSessionSlider) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(
                                value = sessionLimit,
                                onValueChange = { sessionLimit = it },
                                onValueChangeFinished = {
                                    onUpdateApp(app.copy(sessionLimitMinutes = sessionLimit.roundToInt()))
                                },
                                valueRange = 5f..60f, // 5 minutes to 1 hour
                                steps = 10 // 5-minute increments
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "5m",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "1h",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Quick preset buttons
                if (!showDailySlider && !showSessionSlider) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onUpdateApp(app.copy(dailyLimitMinutes = 30, sessionLimitMinutes = 10))
                                dailyLimit = 30f
                                sessionLimit = 10f
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Strict", style = MaterialTheme.typography.bodySmall)
                        }

                        OutlinedButton(
                            onClick = {
                                onUpdateApp(app.copy(dailyLimitMinutes = 60, sessionLimitMinutes = 15))
                                dailyLimit = 60f
                                sessionLimit = 15f
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Moderate", style = MaterialTheme.typography.bodySmall)
                        }

                        OutlinedButton(
                            onClick = {
                                onUpdateApp(app.copy(dailyLimitMinutes = 120, sessionLimitMinutes = 30))
                                dailyLimit = 120f
                                sessionLimit = 30f
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Relaxed", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
