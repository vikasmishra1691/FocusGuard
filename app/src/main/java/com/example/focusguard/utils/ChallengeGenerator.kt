package com.example.focusguard.utils

import kotlin.random.Random
import com.example.focusguard.data.model.ArithmeticChallenge

class ChallengeGenerator {

    data class Challenge(
        val question: String,
        val answer: Int,
        val difficulty: Int
    )

    fun generateChallenge(difficulty: Int, packageName: String): ArithmeticChallenge {
        val challenge = when (difficulty) {
            1 -> generateEasyChallenge()
            2 -> generateMediumChallenge()
            3 -> generateHardChallenge()
            4 -> generateVeryHardChallenge()
            5 -> generateExtremeChallenge()
            else -> generateMediumChallenge()
        }

        return ArithmeticChallenge(
            packageName = packageName,
            question = challenge.question,
            correctAnswer = challenge.answer,
            userAnswer = null,
            difficultyLevel = difficulty,
            timeToSolveSeconds = 0,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun generateEasyChallenge(): Challenge {
        val operations = listOf("+", "-")
        val operation = operations.random()

        return when (operation) {
            "+" -> {
                val a = Random.nextInt(10, 50)
                val b = Random.nextInt(10, 50)
                Challenge("$a + $b = ?", a + b, 1)
            }
            "-" -> {
                val a = Random.nextInt(20, 100)
                val b = Random.nextInt(10, a - 1)
                Challenge("$a - $b = ?", a - b, 1)
            }
            else -> generateEasyChallenge()
        }
    }

    private fun generateMediumChallenge(): Challenge {
        val operations = listOf("+", "-", "×")
        val operation = operations.random()

        return when (operation) {
            "+" -> {
                val a = Random.nextInt(50, 200)
                val b = Random.nextInt(50, 200)
                Challenge("$a + $b = ?", a + b, 2)
            }
            "-" -> {
                val a = Random.nextInt(100, 500)
                val b = Random.nextInt(50, a - 1)
                Challenge("$a - $b = ?", a - b, 2)
            }
            "×" -> {
                val a = Random.nextInt(5, 15)
                val b = Random.nextInt(5, 15)
                Challenge("$a × $b = ?", a * b, 2)
            }
            else -> generateMediumChallenge()
        }
    }

    private fun generateHardChallenge(): Challenge {
        val operations = listOf("+", "-", "×", "÷")
        val operation = operations.random()

        return when (operation) {
            "+" -> {
                val a = Random.nextInt(200, 1000)
                val b = Random.nextInt(200, 1000)
                Challenge("$a + $b = ?", a + b, 3)
            }
            "-" -> {
                val a = Random.nextInt(500, 2000)
                val b = Random.nextInt(100, a - 1)
                Challenge("$a - $b = ?", a - b, 3)
            }
            "×" -> {
                val a = Random.nextInt(10, 25)
                val b = Random.nextInt(10, 25)
                Challenge("$a × $b = ?", a * b, 3)
            }
            "÷" -> {
                val b = Random.nextInt(5, 20)
                val answer = Random.nextInt(10, 50)
                val a = b * answer
                Challenge("$a ÷ $b = ?", answer, 3)
            }
            else -> generateHardChallenge()
        }
    }

    private fun generateVeryHardChallenge(): Challenge {
        val challengeTypes = listOf("multi_step", "large_multiplication", "complex_division")
        val type = challengeTypes.random()

        return when (type) {
            "multi_step" -> {
                val a = Random.nextInt(10, 50)
                val b = Random.nextInt(10, 50)
                val c = Random.nextInt(5, 20)
                val answer = a + b * c
                Challenge("$a + $b × $c = ?", answer, 4)
            }
            "large_multiplication" -> {
                val a = Random.nextInt(25, 100)
                val b = Random.nextInt(25, 100)
                Challenge("$a × $b = ?", a * b, 4)
            }
            "complex_division" -> {
                val b = Random.nextInt(15, 50)
                val answer = Random.nextInt(20, 100)
                val a = b * answer + Random.nextInt(1, b - 1)
                Challenge("$a ÷ $b = ? (whole number only)", a / b, 4)
            }
            else -> generateVeryHardChallenge()
        }
    }

    private fun generateExtremeChallenge(): Challenge {
        val challengeTypes = listOf("complex_multi_step", "large_division", "percentage")
        val type = challengeTypes.random()

        return when (type) {
            "complex_multi_step" -> {
                val a = Random.nextInt(15, 75)
                val b = Random.nextInt(15, 75)
                val c = Random.nextInt(10, 30)
                val d = Random.nextInt(5, 15)
                val answer = (a + b) * c - d
                Challenge("($a + $b) × $c - $d = ?", answer, 5)
            }
            "large_division" -> {
                val b = Random.nextInt(25, 99)
                val answer = Random.nextInt(50, 200)
                val a = b * answer
                Challenge("$a ÷ $b = ?", answer, 5)
            }
            "percentage" -> {
                val base = Random.nextInt(100, 1000)
                val percentage = listOf(10, 15, 20, 25, 30, 40, 50, 75).random()
                val answer = (base * percentage) / 100
                Challenge("$percentage% of $base = ?", answer, 5)
            }
            else -> generateExtremeChallenge()
        }
    }
}
