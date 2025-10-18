package com.example.focusguard.utils

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.text.TextUtils

class PermissionChecker(private val context: Context) {

    /**
     * Check if Usage Stats permission is granted
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Check if System Alert Window (overlay) permission is granted
     */
    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true // Permission not required on older versions
        }
    }

    /**
     * Check if Accessibility Service is enabled with improved detection
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        try {
            // Method 1: Check using the static service flag (most reliable for running state)
            if (com.example.focusguard.FocusGuardAccessibilityService.isServiceRunning) {
                return true
            }

            // Method 2: Check system settings
            val serviceName = "${context.packageName}/com.example.focusguard.FocusGuardAccessibilityService"

            // Get enabled accessibility services from system settings
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""

            // Check if our service is in the enabled services list
            val isServiceListed = enabledServices.split(":").any { service ->
                service.equals(serviceName, ignoreCase = true)
            }

            // Check if accessibility is enabled globally
            val accessibilityEnabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                0
            ) == 1

            // Return true if both conditions are met
            return accessibilityEnabled && isServiceListed

        } catch (e: Exception) {
            // Log the error for debugging
            android.util.Log.e("PermissionChecker", "Error checking accessibility service", e)

            // Final fallback: check if the service is running using the static flag
            return com.example.focusguard.FocusGuardAccessibilityService.isServiceRunning
        }
    }

    /**
     * Get a detailed status of all permissions for debugging
     */
    fun getPermissionStatus(): String {
        return buildString {
            appendLine("=== Permission Status ===")
            appendLine("Accessibility Service: ${isAccessibilityServiceEnabled()}")
            appendLine("Usage Stats: ${hasUsageStatsPermission()}")
            appendLine("Overlay Permission: ${hasOverlayPermission()}")
            appendLine("Service Running Flag: ${com.example.focusguard.FocusGuardAccessibilityService.isServiceRunning}")

            try {
                val enabledServices = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: "null"
                appendLine("Enabled Services: $enabledServices")

                val accessibilityEnabled = Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED,
                    0
                )
                appendLine("Accessibility Globally Enabled: ${accessibilityEnabled == 1}")
            } catch (e: Exception) {
                appendLine("Error getting system settings: ${e.message}")
            }
        }
    }
}
