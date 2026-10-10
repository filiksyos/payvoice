package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.PaymentSmsProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PayVoice_SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        Log.i(TAG, "SMS broadcast received: ${messages?.size ?: 0} message parts")
        if (messages.isNullOrEmpty()) {
            return
        }

        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                // Group messages by originating address in case of multi-part SMS
                val grouped = messages.groupBy { it.displayOriginatingAddress ?: it.originatingAddress ?: "Unknown" }
                val processor = PaymentSmsProcessor(context)

                for ((sender, parts) in grouped) {
                    val fullBody = parts.joinToString("") { it.displayMessageBody ?: it.messageBody ?: "" }
                    Log.i(TAG, "Incoming SMS from '$sender': ${parts.size} parts, ${fullBody.length} characters")

                    processor.process(sender, fullBody, parts.first().timestampMillis, "Broadcast")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling incoming SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
