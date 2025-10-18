package com.example.focusguard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.focusguard.ui.theme.FocusGuardTheme
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsRepository = SettingsRepository(this)

        setContent {
            FocusGuardTheme {
                SettingsScreen(
                    onBack = { finish() }
                )
            }
        }
    }

    @Composable
    private fun SettingsScreen(onBack: () -> Unit) {
        val context = LocalContext.current
        val settings by settingsRepository.settings.collectAsState(initial = AppSettings())

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
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
                // Permissions Section
                item {
                    Text(
                        text = "Permissions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    PermissionCard(
                        title = "Accessibility Service",
                        description = "Required to detect app launches and provide blocking",
                        isEnabled = settings.accessibilityServiceEnabled,
                        icon = Icons.Default.Accessibility,
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }
                    )
                }

                item {
                    PermissionCard(
                        title = "Usage Stats Access",
                        description = "Allows tracking app usage time for analytics",
                        isEnabled = settings.usageStatsPermission,
                        icon = Icons.Default.BarChart,
                        onClick = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        }
                    )
                }

                item {
                    PermissionCard(
                        title = "Display Over Other Apps",
                        description = "Enables showing blocking screens over apps",
                        isEnabled = settings.overlayPermission,
                        icon = Icons.Default.DisplaySettings,
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    )
                }

                // Challenge Settings
                item {
                    Text(
                        text = "Challenge Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    ChallengeSettingsCard(
                        difficulty = settings.challengeDifficulty,
                        timeEarned = settings.defaultTimeEarned,
                        onDifficultyChange = { difficulty ->
                            lifecycleScope.launch {
                                settingsRepository.updateChallengeDifficulty(difficulty)
                            }
                        }
                    )
                }

                // App Management
                item {
                    Text(
                        text = "App Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    ActionCard(
                        title = "Select Apps to Monitor",
                        description = "Choose which social media apps to block",
                        icon = Icons.Default.Apps,
                        onClick = {
                            context.startActivity(Intent(context, AppSelectionActivity::class.java))
                        }
                    )
                }

                item {
                    ActionCard(
                        title = "Configure Time Limits",
                        description = "Set daily and session limits for each app",
                        icon = Icons.Default.Timer,
                        onClick = {
                            context.startActivity(Intent(context, TimeLimitsActivity::class.java))
                        }
                    )
                }

                // About Section
                item {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    AboutCard()
                }

                // Reset Option
                item {
                    ResetCard(
                        onReset = {
                            lifecycleScope.launch {
                                // Reset all settings to defaults
                                settingsRepository.updateChallengeDifficulty(2)
                                // Note: In a full implementation, you'd clear the database too
                            }
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun PermissionCard(
        title: String,
        description: String,
        isEnabled: Boolean,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        onClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            colors = CardDefaults.cardColors(
                containerColor = if (isEnabled)
                    Color(0xFF4CAF50).copy(alpha = 0.1f)
                else
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(40.dp),
                    tint = if (isEnabled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Icon(
                    imageVector = if (isEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = if (isEnabled) "Enabled" else "Disabled",
                    tint = if (isEnabled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
            }
        }
    }

    @Composable
    private fun ChallengeSettingsCard(
        difficulty: Int,
        timeEarned: Int,
        onDifficultyChange: (Int) -> Unit
    ) {
        var showDifficultySlider by remember { mutableStateOf(false) }

        Card {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Quiz,
                        contentDescription = "Challenge",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Challenge Difficulty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = getDifficultyText(difficulty),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    TextButton(
                        onClick = { showDifficultySlider = !showDifficultySlider }
                    ) {
                        Text("Adjust")
                    }
                }

                if (showDifficultySlider) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Difficulty Level: ${difficulty}/5",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = difficulty.toFloat(),
                        onValueChange = { /* Live preview */ },
                        onValueChangeFinished = { onDifficultyChange(difficulty) },
                        valueRange = 1f..5f,
                        steps = 3
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Easy", style = MaterialTheme.typography.bodySmall)
                        Text("Hard", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Time earned per correct answer: $timeEarned minutes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun ActionCard(
        title: String,
        description: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        onClick: () -> Unit
    ) {
        Card(onClick = onClick) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Go",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun AboutCard() {
        Card {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "FocusGuard",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Break free from social media addiction with intelligent blocking and cognitive challenges. FocusGuard helps you regain control over your digital habits.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    @Composable
    private fun ResetCard(onReset: () -> Unit) {
        var showConfirmDialog by remember { mutableStateOf(false) }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Reset",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Reset All Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "This will restore all settings to defaults",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                TextButton(
                    onClick = { showConfirmDialog = true }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Reset Settings?") },
                text = { Text("This will reset all your settings and preferences. Your usage history will be preserved.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onReset()
                            showConfirmDialog = false
                        }
                    ) {
                        Text("Reset")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    private fun getDifficultyText(difficulty: Int): String {
        return when (difficulty) {
            1 -> "Easy - Simple addition and subtraction"
            2 -> "Medium - Basic arithmetic with larger numbers"
            3 -> "Hard - Mixed operations and division"
            4 -> "Very Hard - Multi-step problems"
            5 -> "Extreme - Complex calculations and percentages"
            else -> "Medium"
        }
    }
}
