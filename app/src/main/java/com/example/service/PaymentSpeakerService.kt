package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.PreferencesManager
import com.example.tts.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PaymentSpeakerService : Service() {

    companion object {
        private const val TAG = "PaymentSpeakerService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "payvoice_foreground_channel"

        const val ACTION_START_SERVICE = "com.example.action.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_SERVICE"
        const val ACTION_SPEAK = "com.example.action.SPEAK"

        const val EXTRA_SPEECH_TEXT = "extra_speech_text"
        const val EXTRA_PAYMENT_ID = "extra_payment_id"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, PaymentSpeakerService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, PaymentSpeakerService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }

        fun speak(context: Context, text: String, paymentId: Long? = null) {
            val intent = Intent(context, PaymentSpeakerService::class.java).apply {
                action = ACTION_SPEAK
                putExtra(EXTRA_SPEECH_TEXT, text)
                paymentId?.let { putExtra(EXTRA_PAYMENT_ID, it) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private lateinit var ttsManager: TtsManager
    private lateinit var preferencesManager: PreferencesManager
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        ttsManager = TtsManager.getInstance(applicationContext)
        preferencesManager = PreferencesManager(applicationContext)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PayVoice::PaymentAnnouncementWakeLock"
        )

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_SERVICE

        when (action) {
            ACTION_STOP_SERVICE -> {
                _isRunning.value = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_SPEAK -> {
                startAsForeground()
                val textToSpeak = intent?.getStringExtra(EXTRA_SPEECH_TEXT)
                if (!textToSpeak.isNullOrBlank()) {
                    speakWithWakeLock(textToSpeak)
                }
            }

            ACTION_START_SERVICE -> {
                startAsForeground()
            }
        }

        return START_STICKY
    }

    private fun startAsForeground() {
        _isRunning.value = true
        val notification = buildForegroundNotification("Active: Monitoring CBE & Telebirr SMS")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
        }
    }

    private fun speakWithWakeLock(text: String) {
        try {
            wakeLock?.acquire(15000L) // 15-second safety timeout
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock", e)
        }

        val settings = preferencesManager.settings.value
        if (!settings.isServiceEnabled) {
            Log.d(TAG, "Service is muted/disabled in settings, skipping TTS")
            releaseWakeLock()
            return
        }

        updateNotification("Speaking: $text")

        ttsManager.speak(text, settings) {
            updateNotification("Active: Monitoring CBE & Telebirr SMS")
            releaseWakeLock()
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release wake lock", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PayVoice Payment Announcer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Always-on service listening for CBE and Telebirr SMS transactions"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(contentText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PayVoice Cashier Assistant")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.pay_alert_icon_1791456554485)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = buildForegroundNotification(contentText)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        releaseWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
