package com.example.focusguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.focusguard.data.model.MonitoredApp
import com.example.focusguard.data.model.AppInfo
import com.example.focusguard.ui.theme.FocusGuardTheme
import kotlinx.coroutines.launch

class AppSelectionActivity : ComponentActivity() {
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var accessTimeManager: AccessTimeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsRepository = SettingsRepository(this)
        accessTimeManager = AccessTimeManager(this)

        setContent {
            FocusGuardTheme {
                AppSelectionScreen(
                    onBack = { finish() }
                )
            }
        }
    }

    @Composable
    private fun AppSelectionScreen(onBack: () -> Unit) {
        var availableApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
        var monitoredApps by remember { mutableStateOf<List<MonitoredApp>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        // Add local state for immediate UI feedback
        var pendingOperations by remember { mutableStateOf<Set<String>>(emptySet()) }

        LaunchedEffect(Unit) {
            try {
                availableApps = accessTimeManager.getInstalledSocialMediaApps()
                settingsRepository.getMonitoredApps().collect { apps ->
                    monitoredApps = apps
                    isLoading = false
                    // Clear pending operations when data is refreshed
                    pendingOperations = emptySet()
                }
            } catch (e: Exception) {
                isLoading = false
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Select Apps to Monitor") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                Text(
                                    text = "Choose Apps to Monitor",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Select social media apps that you want FocusGuard to monitor and limit. You can change these settings anytime.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Available Apps (${availableApps.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    if (availableApps.isEmpty()) {
                        item {
                            Card {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No Social Media Apps Found",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Install some social media apps like Instagram, TikTok, Facebook, Twitter, or YouTube to monitor them with FocusGuard.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    items(availableApps) { appInfo ->
                        val isMonitored = monitoredApps.any { it.packageName == appInfo.packageName }
                        val isPending = pendingOperations.contains(appInfo.packageName)

                        Card(
                            onClick = {
                                // Add to pending operations for immediate UI feedback
                                pendingOperations = pendingOperations + appInfo.packageName

                                lifecycleScope.launch {
                                    try {
                                        if (isMonitored) {
                                            // Properly remove from monitoring by removing the app entirely
                                            settingsRepository.removeMonitoredApp(appInfo.packageName)
                                        } else {
                                            // Add to monitoring with default settings
                                            val monitoredApp = MonitoredApp(
                                                packageName = appInfo.packageName,
                                                appName = appInfo.appName,
                                                dailyLimitMinutes = 60, // Default 1 hour
                                                sessionLimitMinutes = 15, // Default 15 minutes
                                                isEnabled = true
                                            )
                                            settingsRepository.addMonitoredApp(monitoredApp)
                                        }
                                    } catch (e: Exception) {
                                        // Remove from pending if operation failed
                                        pendingOperations = pendingOperations - appInfo.packageName
                                    }
                                }
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isPending -> MaterialTheme.colorScheme.surfaceVariant
                                    isMonitored -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // App icon placeholder with better styling
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            if (isMonitored) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = appInfo.appName.take(2).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMonitored) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = appInfo.appName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMonitored) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.onSurface
                                    )

                                    if (isPending) {
                                        Text(
                                            text = "Updating...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else if (isMonitored) {
                                        val monitoredApp = monitoredApps.find { it.packageName == appInfo.packageName }
                                        if (monitoredApp != null) {
                                            Text(
                                                text = "Daily limit: ${monitoredApp.dailyLimitMinutes} min • Session: ${monitoredApp.sessionLimitMinutes} min",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Tap to monitor this app",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Better visual indicator
                                if (isPending) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isMonitored) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = if (isMonitored) "Monitored" else "Not monitored",
                                        tint = if (isMonitored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Instructions
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
                                    text = "💡 Tips",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "• Start with 2-3 apps that you use most frequently\n" +
                                            "• You can adjust time limits individually for each app\n" +
                                            "• Apps can be enabled/disabled anytime in settings",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
