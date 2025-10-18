package com.example.focusguard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.focusguard.ui.theme.*
import com.example.focusguard.ui.components.*
import com.example.focusguard.utils.PermissionChecker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var accessTimeManager: AccessTimeManager
    private lateinit var permissionChecker: PermissionChecker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsRepository = SettingsRepository(this)
        accessTimeManager = AccessTimeManager(this)
        permissionChecker = PermissionChecker(this)

        setContent {
            FocusGuardTheme {
                val settings by settingsRepository.settings.collectAsState(
                    initial = AppSettings()
                )

                @OptIn(ExperimentalAnimationApi::class)
                AnimatedContent(
                    targetState = settings.isFirstTimeSetup,
                    transitionSpec = {
                        slideInHorizontally { it } + fadeIn() with
                                slideOutHorizontally { -it } + fadeOut()
                    },
                    label = "main_content"
                ) { isFirstTime ->
                    if (isFirstTime) {
                        ModernSetupScreen(
                            onSetupComplete = {
                                lifecycleScope.launch {
                                    settingsRepository.setFirstTimeSetupComplete()
                                }
                            }
                        )
                    } else {
                        ModernMainScreen(
                            settings = settings,
                            onNavigateToAppSelection = {
                                startActivity(Intent(this@MainActivity, AppSelectionActivity::class.java))
                            },
                            onNavigateToTimeLimits = {
                                startActivity(Intent(this@MainActivity, TimeLimitsActivity::class.java))
                            },
                            onNavigateToAnalytics = {
                                startActivity(Intent(this@MainActivity, AnalyticsActivity::class.java))
                            },
                            onNavigateToSettings = {
                                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ModernSetupScreen(onSetupComplete: () -> Unit) {
        var currentStep by remember { mutableStateOf(0) }
        val totalSteps = 3
        val stepTitles = listOf("Accessibility", "Usage Stats", "Overlay")

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(40.dp))

                    Text(
                        text = "Welcome to",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "FocusGuard",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Your digital wellness companion",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Progress Section
                GradientCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Step ${currentStep + 1} of $totalSteps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        CircularProgress(
                            progress = (currentStep + 1).toFloat() / totalSteps,
                            size = 40.dp,
                            strokeWidth = 4.dp,
                            label = "${currentStep + 1}"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = (currentStep + 1).toFloat() / totalSteps,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(stepTitles.size) { index ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (index <= currentStep)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = stepTitles[index],
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (index <= currentStep)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Step Content
                @OptIn(ExperimentalAnimationApi::class)
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        slideInHorizontally { it } + fadeIn() with
                                slideOutHorizontally { -it } + fadeOut()
                    },
                    label = "step_content"
                ) { step ->
                    when (step) {
                        0 -> ModernSetupStep(
                            icon = Icons.Default.Accessibility,
                            title = "Enable Accessibility Service",
                            description = "FocusGuard needs accessibility access to detect app usage and provide smart blocking features.",
                            buttonText = "Open Accessibility Settings",
                            onAction = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                startActivity(intent)
                            },
                            onNext = { currentStep = 1 }
                        )
                        1 -> ModernSetupStep(
                            icon = Icons.Default.Analytics,
                            title = "Enable Usage Statistics",
                            description = "This allows FocusGuard to track your app usage patterns and provide detailed analytics.",
                            buttonText = "Open Usage Access Settings",
                            onAction = {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                startActivity(intent)
                            },
                            onNext = { currentStep = 2 }
                        )
                        2 -> ModernSetupStep(
                            icon = Icons.Default.Layers,
                            title = "Enable Display Overlay",
                            description = "This permission allows FocusGuard to show blocking screens and helpful reminders over other apps.",
                            buttonText = "Open Overlay Settings",
                            onAction = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                                startActivity(intent)
                            },
                            onNext = onSetupComplete,
                            isLastStep = true
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ModernSetupStep(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        title: String,
        description: String,
        buttonText: String,
        onAction: () -> Unit,
        onNext: () -> Unit,
        isLastStep: Boolean = false
    ) {
        Column {
            GradientCard {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        GradientStart.copy(alpha = 0.2f),
                                        GradientEnd.copy(alpha = 0.1f)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ActionButton(
                        text = buttonText,
                        icon = Icons.Default.Settings,
                        onClick = onAction,
                        variant = ButtonVariant.Gradient
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ActionButton(
                        text = if (isLastStep) "Complete Setup" else "Continue",
                        icon = if (isLastStep) Icons.Default.Check else Icons.Default.ArrowForward,
                        onClick = onNext,
                        variant = ButtonVariant.Secondary
                    )
                }
            }
        }
    }

    @Composable
    private fun ModernMainScreen(
        settings: AppSettings,
        onNavigateToAppSelection: () -> Unit,
        onNavigateToTimeLimits: () -> Unit,
        onNavigateToAnalytics: () -> Unit,
        onNavigateToSettings: () -> Unit
    ) {
        val context = LocalContext.current
        val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

        // Real-time state management
        val todayStats by settingsRepository.getDailyStats(today).collectAsState(initial = emptyList())
        val monitoredApps by settingsRepository.getEnabledApps().collectAsState(initial = emptyList())

        // Permission states - check in real-time
        var accessibilityEnabled by remember { mutableStateOf(false) }
        var usageStatsEnabled by remember { mutableStateOf(false) }
        var overlayEnabled by remember { mutableStateOf(false) }

        // Auto-refresh mechanism - check permissions every 2 seconds
        LaunchedEffect(Unit) {
            while (true) {
                accessibilityEnabled = permissionChecker.isAccessibilityServiceEnabled()
                usageStatsEnabled = permissionChecker.hasUsageStatsPermission()
                overlayEnabled = permissionChecker.hasOverlayPermission()
                delay(2000) // Check every 2 seconds
            }
        }

        // Force UI refresh every 30 seconds to update usage stats
        var refreshTrigger by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(30000) // Refresh every 30 seconds
                refreshTrigger++
                android.util.Log.d("MainActivity", "Auto-refresh triggered: $refreshTrigger")
            }
        }

        // Trigger manual refresh when returning to app
        DisposableEffect(Unit) {
            android.util.Log.d("MainActivity", "MainActivity composed - checking for updates")
            onDispose { }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column {
                ModernTopBar(
                    title = "FocusGuard",
                    subtitle = "Stay focused, stay productive",
                    onSettingsClick = {
                        android.util.Log.d("MainActivity", "Settings clicked from top bar")
                        onNavigateToSettings()
                    }
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 20.dp)
                ) {
                    // Status Card - shows real-time permission status
                    item {
                        ModernStatusCard(
                            accessibilityEnabled = accessibilityEnabled,
                            usageStatsEnabled = usageStatsEnabled,
                            overlayEnabled = overlayEnabled
                        )
                    }

                    // Quick Stats - now with real-time updates
                    item {
                        QuickStatsSection(refreshTrigger = refreshTrigger)
                    }

                    // Quick Actions
                    item {
                        Text(
                            text = "Quick Actions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ActionButton(
                                    text = "Select Apps",
                                    icon = Icons.Default.Apps,
                                    onClick = onNavigateToAppSelection,
                                    modifier = Modifier.weight(1f),
                                    variant = ButtonVariant.Primary
                                )

                                ActionButton(
                                    text = "Set Limits",
                                    icon = Icons.Default.Timer,
                                    onClick = onNavigateToTimeLimits,
                                    modifier = Modifier.weight(1f),
                                    variant = ButtonVariant.Secondary
                                )
                            }

                            ActionButton(
                                text = "View Analytics",
                                icon = Icons.Default.Analytics,
                                onClick = onNavigateToAnalytics,
                                variant = ButtonVariant.Gradient
                            )
                        }
                    }

                    // Monitored Apps Section
                    if (monitoredApps.isNotEmpty()) {
                        item {
                            Text(
                                text = "Monitored Apps (${monitoredApps.size})",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        items(monitoredApps) { app ->
                            ModernAppCard(
                                app = app,
                                stats = todayStats.find { it.packageName == app.packageName }
                            )
                        }
                    } else {
                        item {
                            EmptyStateCard()
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ModernStatusCard(
        accessibilityEnabled: Boolean,
        usageStatsEnabled: Boolean,
        overlayEnabled: Boolean
    ) {
        val allEnabled = accessibilityEnabled && usageStatsEnabled && overlayEnabled
        val enabledCount = listOf(accessibilityEnabled, usageStatsEnabled, overlayEnabled).count { it }

        GradientCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (allEnabled) Icons.Default.Shield else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (allEnabled) Success else Warning,
                            modifier = Modifier.size(24.dp)
                        )

                        Text(
                            text = if (allEnabled) "Protection Active" else "Setup Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (allEnabled)
                            "All permissions granted. FocusGuard is ready to protect your focus."
                        else
                            "$enabledCount of 3 permissions granted. Complete setup for full protection.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                CircularProgress(
                    progress = enabledCount / 3f,
                    size = 60.dp,
                    strokeWidth = 6.dp,
                    label = "$enabledCount/3"
                )
            }

            if (!allEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusIndicator(
                        isActive = accessibilityEnabled,
                        activeText = "Accessibility Service",
                        inactiveText = "Accessibility Service Required"
                    )

                    StatusIndicator(
                        isActive = usageStatsEnabled,
                        activeText = "Usage Stats Access",
                        inactiveText = "Usage Stats Access Required"
                    )

                    StatusIndicator(
                        isActive = overlayEnabled,
                        activeText = "Display Overlay",
                        inactiveText = "Display Overlay Required"
                    )
                }
            }
        }
    }

    @Composable
    private fun QuickStatsSection(refreshTrigger: Int) {
        val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
        val todayStats by settingsRepository.getDailyStats(today).collectAsState(initial = emptyList())
        val enabledApps by settingsRepository.getEnabledApps().collectAsState(initial = emptyList())

        // Add debugging
        LaunchedEffect(todayStats) {
            android.util.Log.d("MainActivity", "Today stats updated: ${todayStats.size} entries")
            todayStats.forEach { stat ->
                android.util.Log.d("MainActivity", "Stat: ${stat.packageName} - ${stat.totalUsageMinutes}min")
            }
        }

        val totalUsage = todayStats.sumOf { it.totalUsageMinutes }
        val totalBlocks = todayStats.sumOf { it.blockedAttempts }
        val savedTime = enabledApps.sumOf { app ->
            val usage = todayStats.find { it.packageName == app.packageName }?.totalUsageMinutes ?: 0
            maxOf(0, usage - app.dailyLimitMinutes)
        }

        GradientCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Add refresh button for debugging
                IconButton(
                    onClick = {
                        android.util.Log.d("MainActivity", "Manual refresh requested")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatsCard(
                    title = "Total Usage",
                    value = "${totalUsage}min",
                    subtitle = if (todayStats.isEmpty()) "No data yet" else "across ${todayStats.size} apps",
                    icon = Icons.Default.AccessTime,
                    modifier = Modifier.weight(1f)
                )

                StatsCard(
                    title = "Blocks",
                    value = "$totalBlocks",
                    subtitle = "${savedTime}min saved",
                    icon = Icons.Default.Block,
                    modifier = Modifier.weight(1f),
                    gradient = true
                )
            }

            // Add debug information
            if (todayStats.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💡 No usage data yet. Open a monitored app to start tracking!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }

    @Composable
    private fun ModernAppCard(
        app: com.example.focusguard.data.model.MonitoredApp,
        stats: com.example.focusguard.data.model.DailyUsageStats?
    ) {
        GradientCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Time Limit: ${app.dailyLimitMinutes}min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (stats != null) {
                        Text(
                            text = "Used: ${stats.totalUsageMinutes}min | Blocks: ${stats.blockedAttempts}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (stats != null) {
                    val progress = (stats.totalUsageMinutes.toFloat() / app.dailyLimitMinutes).coerceAtMost(1f)
                    CircularProgress(
                        progress = progress,
                        size = 50.dp,
                        strokeWidth = 4.dp,
                        label = "${(progress * 100).toInt()}%"
                    )
                }
            }
        }
    }

    @Composable
    private fun EmptyStateCard() {
        GradientCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "No Apps Selected Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Start by selecting apps you want to monitor and set time limits for better focus.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
