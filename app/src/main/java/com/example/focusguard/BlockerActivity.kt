package com.example.focusguard

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.focusguard.data.model.ArithmeticChallenge
import com.example.focusguard.ui.theme.FocusGuardTheme
import com.example.focusguard.utils.ChallengeGenerator
import kotlinx.coroutines.launch

class BlockerActivity : ComponentActivity() {
    private lateinit var accessTimeManager: AccessTimeManager
    private lateinit var challengeGenerator: ChallengeGenerator
    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        accessTimeManager = AccessTimeManager(this)
        challengeGenerator = ChallengeGenerator()
        settingsRepository = SettingsRepository(this)

        val packageName = intent.getStringExtra("package_name") ?: ""
        val blockReason = intent.getStringExtra("block_reason") ?: "App blocked"
        val timeRemaining = intent.getIntExtra("time_remaining", 0)
        val requiresChallenge = intent.getBooleanExtra("requires_challenge", false)
        val challengeDifficulty = intent.getIntExtra("challenge_difficulty", 2)

        // ✅ New back-press handling (for both gestures + button)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Prevent going back to the blocked app
                goHome()
            }
        })

        setContent {
            FocusGuardTheme {
                BlockerScreen(
                    packageName = packageName,
                    blockReason = blockReason,
                    timeRemaining = timeRemaining,
                    requiresChallenge = requiresChallenge,
                    challengeDifficulty = challengeDifficulty,
                    onChallengeCompleted = { timeEarned ->
                        handleChallengeCompleted(timeEarned)
                    },
                    onGoHome = {
                        goHome()
                    }
                )
            }
        }
    }

    private fun handleChallengeCompleted(timeEarned: Int) {
        // Start a timer for the earned time, then return to blocking
        lifecycleScope.launch {
            kotlinx.coroutines.delay((timeEarned * 60 * 1000).toLong()) // Convert minutes to milliseconds
            // After time expires, show blocker again or go home
            goHome()
        }

        // For now, just close the blocker
        finish()
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    @Composable
    private fun BlockerScreen(
        packageName: String,
        blockReason: String,
        timeRemaining: Int,
        requiresChallenge: Boolean,
        challengeDifficulty: Int,
        onChallengeCompleted: (Int) -> Unit,
        onGoHome: () -> Unit
    ) {
        var currentChallenge by remember {
            mutableStateOf(
                if (requiresChallenge) challengeGenerator.generateChallenge(challengeDifficulty, packageName)
                else null
            )
        }
        var userInput by remember { mutableStateOf("") }
        var showResult by remember { mutableStateOf(false) }
        var isCorrect by remember { mutableStateOf(false) }
        var timeEarned by remember { mutableStateOf(0) }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Block Icon
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Blocked",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // App Name
                Text(
                    text = getAppName(packageName),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Block Reason
                Text(
                    text = blockReason,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )

                if (timeRemaining > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Time remaining",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$timeRemaining minutes remaining today",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (requiresChallenge && currentChallenge != null && !showResult) {
                    // Challenge Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Solve this to continue:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = currentChallenge!!.question,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = userInput,
                                onValueChange = { userInput = it },
                                label = { Text("Your answer") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val userAnswer = userInput.toIntOrNull()
                                    if (userAnswer != null) {
                                        val startTime = System.currentTimeMillis()
                                        val challenge = currentChallenge!!.copy(
                                            userAnswer = userAnswer,
                                            isCorrect = userAnswer == currentChallenge!!.correctAnswer,
                                            timeToSolveSeconds = 0
                                        )

                                        lifecycleScope.launch {
                                            if (challenge.isCorrect) {
                                                // Grant extra time and store it
                                                val earned = accessTimeManager.grantExtraTime(packageName, challenge)

                                                // Store the earned extra time in database
                                                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                                                settingsRepository.addEarnedExtraTime(packageName, today, earned)

                                                android.util.Log.d("BlockerActivity",
                                                    "Challenge completed! Earned $earned minutes for $packageName")

                                                // Update challenge record with earned time
                                                settingsRepository.recordChallenge(
                                                    challenge.copy(timeEarnedMinutes = earned)
                                                )

                                                timeEarned = earned
                                                isCorrect = true
                                            } else {
                                                // Record failed challenge
                                                settingsRepository.recordChallenge(challenge)
                                                android.util.Log.d("BlockerActivity",
                                                    "Challenge failed for $packageName")
                                                isCorrect = false
                                            }
                                            showResult = true
                                        }
                                    }
                                },
                                enabled = userInput.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Submit Answer")
                            }
                        }
                    }
                } else if (showResult) {
                    // Result Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCorrect)
                                Color(0xFF4CAF50).copy(alpha = 0.1f)
                            else
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isCorrect) "Correct! ✓" else "Incorrect ✗",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isCorrect) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                            )

                            if (isCorrect) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "You earned $timeEarned minutes!",
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(onClick = { onChallengeCompleted(timeEarned) }) {
                                    Text("Continue to App")
                                }
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "The correct answer was ${currentChallenge?.correctAnswer}",
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        currentChallenge = challengeGenerator.generateChallenge(challengeDifficulty, packageName)
                                        userInput = ""
                                        showResult = false
                                    }
                                ) {
                                    Text("Try Again")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Home Button
                OutlinedButton(
                    onClick = onGoHome,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go to Home Screen")
                }
            }
        }
    }

    private fun getAppName(packageName: String): String {
        return when (packageName) {
            "com.instagram.android" -> "Instagram"
            "com.zhiliaoapp.musically" -> "TikTok"
            "com.facebook.katana" -> "Facebook"
            "com.twitter.android" -> "Twitter"
            "com.snapchat.android" -> "Snapchat"
            "com.reddit.frontpage" -> "Reddit"
            "com.google.android.youtube" -> "YouTube"
            else -> "Blocked App"
        }
    }
}
