package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.PreferencesManager
import com.example.service.PaymentSpeakerService

class BootCompletedReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "PayVoice_BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d(TAG, "Device booted or app updated, checking auto-start preference")
            val preferences = PreferencesManager(context).settings.value
            if (preferences.isServiceEnabled) {
                Log.d(TAG, "Starting PaymentSpeakerService on boot")
                PaymentSpeakerService.start(context)
            }
        }
    }
}
