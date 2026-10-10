package com.example.data

import android.content.Context
import android.util.Log
import com.example.service.PaymentSpeakerService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.util.Locale

class PaymentSmsProcessor(
    private val context: Context,
    private val database: PaymentDatabase = PaymentDatabase.getDatabase(context),
    private val announce: (String, Long) -> Unit = { text, id -> PaymentSpeakerService.speak(context, text, id) }
) {
    companion object {
        private const val TAG = "PayVoice_SmsProcessor"
        // Both detection paths must check and insert under the same lock.
        private val processingMutex = Mutex()
    }

    suspend fun process(sender: String, body: String, sentAt: Long, source: String) {
        processingMutex.withLock {
            if (!PaymentParser.isPaymentMessage(sender, body)) {
                Log.i(TAG, "$source SMS ignored: sender or payment/currency keywords did not match")
                return@withLock
            }
            val parsed = PaymentParser.parse(sender, body) ?: run {
                Log.w(TAG, "$source payment SMS ignored: could not extract an amount")
                return@withLock
            }
            val preferences = PreferencesManager(context).settings.value
            if ((parsed.serviceType == PaymentServiceType.CBE && !preferences.cbeEnabled) ||
                (parsed.serviceType == PaymentServiceType.TELEBIRR && !preferences.telebirrEnabled)) {
                Log.i(TAG, "$source payment ignored: ${parsed.serviceType} disabled")
                return@withLock
            }

            val normalizedSender = sender.trim().lowercase(Locale.ROOT)
            val identityTime = if (sentAt > 0) sentAt else System.currentTimeMillis()
            val keyInput = "$normalizedSender\n${body.trim()}\n$identityTime"
            val hash = MessageDigest.getInstance("SHA-256").digest(keyInput.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            val messageKey = (if (sentAt > 0) "sent:" else "unknown:") + hash
            val dao = database.paymentDao()
            // Some SMS apps omit DATE_SENT. In that case, match the other path's
            // same sender/body for a short period rather than announce twice.
            if (dao.hasSmsPayment(messageKey, sender.trim(), body, System.currentTimeMillis() - 30_000, sentAt <= 0)) {
                Log.i(TAG, "$source duplicate payment SMS ignored")
                return@withLock
            }

            val speech = PaymentParser.generateDefaultSpeech(
                parsed.serviceType, parsed.amount, parsed.payerName, parsed.payerPhone, preferences.customTemplate
            )
            val id = dao.insertPayment(PaymentRecord(
                serviceType = parsed.serviceType,
                senderAddress = sender.trim(),
                amount = parsed.amount,
                currency = parsed.currency,
                payerName = parsed.payerName,
                payerPhone = parsed.payerPhone,
                accountOrRef = parsed.accountOrRef,
                rawBody = parsed.rawBody,
                announcedText = speech,
                isAnnounced = preferences.isServiceEnabled,
                smsMessageKey = messageKey
            ))
            Log.i(TAG, "$source payment saved with id: $id, type: ${parsed.serviceType}")
            if (preferences.isServiceEnabled) {
                Log.i(TAG, "Requesting voice announcement for payment $id")
                announce(speech, id)
            } else {
                Log.i(TAG, "Payment stored without speech: announcements disabled")
            }
        }
    }
}
