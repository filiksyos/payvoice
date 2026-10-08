package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.PaymentDatabase
import com.example.data.PaymentParser
import com.example.data.PaymentRecord
import com.example.data.PaymentServiceType
import com.example.data.PreferencesManager
import com.example.service.PaymentSpeakerService
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
        if (messages.isNullOrEmpty()) {
            return
        }

        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                // Group messages by originating address in case of multi-part SMS
                val grouped = messages.groupBy { it.displayOriginatingAddress ?: it.originatingAddress ?: "Unknown" }

                for ((sender, parts) in grouped) {
                    val fullBody = parts.joinToString("") { it.displayMessageBody ?: it.messageBody ?: "" }
                    Log.d(TAG, "Incoming SMS from '$sender': $fullBody")

                    if (!PaymentParser.isPaymentMessage(sender, fullBody)) {
                        continue
                    }

                    val preferences = PreferencesManager(context).settings.value
                    val parsed = PaymentParser.parse(sender, fullBody) ?: continue

                    // Check user preferences filters
                    val isCbeBlocked = parsed.serviceType == PaymentServiceType.CBE && !preferences.cbeEnabled
                    val isTelebirrBlocked = parsed.serviceType == PaymentServiceType.TELEBIRR && !preferences.telebirrEnabled

                    if (isCbeBlocked || isTelebirrBlocked) {
                        Log.d(TAG, "Payment received but service type is disabled in user preferences")
                        continue
                    }

                    // Customize announcement if user has a custom speech template
                    val speechAnnouncement = PaymentParser.generateDefaultSpeech(
                        serviceType = parsed.serviceType,
                        amount = parsed.amount,
                        payerName = parsed.payerName,
                        payerPhone = parsed.payerPhone,
                        customTemplate = preferences.customTemplate
                    )

                    // Persist to Room Database
                    val db = PaymentDatabase.getDatabase(context)
                    val record = PaymentRecord(
                        serviceType = parsed.serviceType,
                        senderAddress = parsed.senderAddress,
                        amount = parsed.amount,
                        currency = parsed.currency,
                        payerName = parsed.payerName,
                        payerPhone = parsed.payerPhone,
                        accountOrRef = parsed.accountOrRef,
                        rawBody = parsed.rawBody,
                        timestamp = System.currentTimeMillis(),
                        announcedText = speechAnnouncement,
                        isAnnounced = preferences.isServiceEnabled
                    )
                    val insertedId = db.paymentDao().insertPayment(record)
                    Log.d(TAG, "Payment saved to database with id: $insertedId")

                    // Trigger Voice Announcement via Foreground Service
                    if (preferences.isServiceEnabled) {
                        PaymentSpeakerService.speak(context, speechAnnouncement, insertedId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling incoming SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
