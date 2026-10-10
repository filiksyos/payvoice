package com.example.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext

data class InboxSms(val sender: String, val body: String, val sentAt: Long)

class SmsInboxMonitor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onSms: suspend (InboxSms) -> Unit,
    private val now: () -> Long = System::currentTimeMillis
) {
    companion object { private const val TAG = "PayVoice_InboxMonitor" }
    private val scanMutex = Mutex()
    private var startedAt = 0L
    private var lastSeenId = -1L
    @Volatile private var running = false
    private var pollingJob: Job? = null
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            Log.d(TAG, "Inbox content changed; running=$running")
            if (running) scope.launch { scanInbox() }
        }
    }

    fun start() {
        if (running) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Inbox monitoring unavailable: SMS read permission not granted")
            return
        }
        startedAt = now()
        lastSeenId = -1L
        try {
            context.contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, observer)
            running = true
            Log.i(TAG, "Inbox monitoring started; watching messages received since $startedAt")
            // The observer reacts immediately. A small periodic check also covers
            // SMS apps/OEMs that suppress or omit content-change notifications.
            pollingJob = scope.launch {
                Log.d(TAG, "Inbox polling coroutine started")
                while (isActive && running) {
                    scanInbox()
                    delay(5_000)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not start inbox monitoring", e)
        }
    }

    private suspend fun scanInbox() {
        scanMutex.withLock {
            if (!running) return@withLock
            val scanStarted = SystemClock.elapsedRealtime()
            try {
                Log.d(TAG, "Inbox query starting: lastSeenId=$lastSeenId, receivedSince=$startedAt, now=${now()}")
                val columns = arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE_SENT)
                val result = context.contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI, columns,
                    "${Telephony.Sms._ID} > ? AND ${Telephony.Sms.DATE} >= ?",
                    arrayOf(lastSeenId.toString(), startedAt.toString()), "${Telephony.Sms._ID} ASC"
                )
                Log.d(TAG, "Inbox query returned after ${SystemClock.elapsedRealtime() - scanStarted} ms; cursorAvailable=${result != null}")
                if (result == null) Log.w(TAG, "SMS provider returned no cursor")
                result?.use { cursor ->
                    var rows = 0
                    while (running && cursor.moveToNext()) {
                        rows++
                        coroutineContext.ensureActive()
                        val id = cursor.getLong(0)
                        val sender = cursor.getString(1)
                        val body = cursor.getString(2)
                        val sentAt = cursor.getLong(3)
                        if (!sender.isNullOrBlank() && !body.isNullOrBlank()) {
                            Log.i(TAG, "New inbox SMS: row $id, sender '$sender', ${body.length} characters")
                            onSms(InboxSms(sender, body, sentAt))
                        }
                        lastSeenId = id
                    }
                    Log.d(TAG, "Inbox scan completed: rows=$rows, lastSeenId=$lastSeenId, elapsedMs=${SystemClock.elapsedRealtime() - scanStarted}")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                Log.e(TAG, "Inbox monitoring lost SMS read permission", e)
                stop()
            } catch (e: Exception) {
                Log.e(TAG, "Could not process inbox changes", e)
            }
        }
    }

    fun stop() {
        if (!running) return
        running = false
        pollingJob?.cancel()
        pollingJob = null
        context.contentResolver.unregisterContentObserver(observer)
        Log.i(TAG, "Inbox monitoring stopped")
    }
}
