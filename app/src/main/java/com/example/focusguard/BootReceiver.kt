package com.example.focusguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == Intent.ACTION_PACKAGE_REPLACED) {

            Log.d("BootReceiver", "Boot completed or package replaced")

            // Check if accessibility service was enabled before reboot
            // In a full implementation, you might want to show a notification
            // reminding the user to re-enable the accessibility service if needed
        }
    }
}
