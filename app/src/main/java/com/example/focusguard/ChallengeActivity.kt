package com.example.focusguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

class ChallengeActivity : ComponentActivity() {
    private lateinit var challengeGenerator: ChallengeGenerator
    private lateinit var accessTimeManager: AccessTimeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        challengeGenerator = ChallengeGenerator()
        accessTimeManager = AccessTimeManager(this)

        val packageName = intent.getStringExtra("package_name") ?: ""
        val difficulty = intent.getIntExtra("difficulty", 2)

        setContent {
            FocusGuardTheme {
                ChallengeScreen(
                    packageName = packageName,
                    difficulty = difficulty,
                    onChallengeCompleted = { timeEarned ->
                        // Return result and finish
                        setResult(RESULT_OK)
                        finish()
                    },
                    onChallengeFailed = {
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }

    @Composable
    private fun ChallengeScreen(
        packageName: String,
        difficulty: Int,
        onChallengeCompleted: (Int) -> Unit,
        onChallengeFailed: () -> Unit
    ) {
        var currentChallenge by remember {
            mutableStateOf(challengeGenerator.generateChallenge(difficulty, packageName))
        }
        var userInput by remember { mutableStateOf("") }
        var showResult by remember { mutableStateOf(false) }
        var isCorrect by remember { mutableStateOf(false) }
        var timeEarned by remember { mutableStateOf(0) }
        var startTime by remember { mutableStateOf(System.currentTimeMillis()) }

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
                Icon(
                    imageVector = Icons.Default.Quiz,
                    contentDescription = "Challenge",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (!showResult) {
                    Text(
                        text = "Solve this challenge to continue:",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = currentChallenge.question,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            OutlinedTextField(
                                value = userInput,
                                onValueChange = { userInput = it },
                                label = { Text("Your answer") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onChallengeFailed,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Give Up")
                                }

                                Button(
                                    onClick = {
                                        val userAnswer = userInput.toIntOrNull()
                                        if (userAnswer != null) {
                                            val timeToSolve = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                                            val challenge = currentChallenge.copy(
                                                userAnswer = userAnswer,
                                                isCorrect = userAnswer == currentChallenge.correctAnswer,
                                                timeToSolveSeconds = timeToSolve
                                            )

                                            lifecycleScope.launch {
                                                val earned = if (challenge.isCorrect) {
                                                    accessTimeManager.grantExtraTime(packageName, challenge)
                                                } else {
                                                    0
                                                }

                                                timeEarned = earned
                                                isCorrect = challenge.isCorrect
                                                showResult = true
                                            }
                                        }
                                    },
                                    enabled = userInput.isNotBlank(),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Submit")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Difficulty: ${getDifficultyName(difficulty)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                } else {
                    // Show result
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
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isCorrect) "Correct! ✓" else "Incorrect ✗",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCorrect) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (isCorrect) {
                                Text(
                                    text = "Great job! You earned $timeEarned minutes of app time.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = { onChallengeCompleted(timeEarned) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Continue to App")
                                }
                            } else {
                                Text(
                                    text = "The correct answer was: ${currentChallenge.correctAnswer}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onChallengeFailed,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Give Up")
                                    }

                                    Button(
                                        onClick = {
                                            // Generate new challenge
                                            currentChallenge = challengeGenerator.generateChallenge(difficulty, packageName)
                                            userInput = ""
                                            showResult = false
                                            startTime = System.currentTimeMillis()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Try Again")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getDifficultyName(difficulty: Int): String {
        return when (difficulty) {
            1 -> "Easy"
            2 -> "Medium"
            3 -> "Hard"
            4 -> "Very Hard"
            5 -> "Extreme"
            else -> "Medium"
        }
    }
}
