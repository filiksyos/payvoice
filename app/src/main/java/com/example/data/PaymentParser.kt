package com.example.data

import java.text.NumberFormat
import java.util.Locale
import java.util.regex.Pattern

data class ParsedPayment(
    val serviceType: PaymentServiceType,
    val senderAddress: String,
    val amount: Double,
    val currency: String = "ETB",
    val payerName: String?,
    val payerPhone: String?,
    val accountOrRef: String?,
    val rawBody: String,
    val speechAnnouncement: String
)

object PaymentParser {

    // Common patterns for amount extraction
    // e.g., "ETB 500.00", "500.00 ETB", "Birr 500", "500 Birr", "ETB500.00"
    private val AMOUNT_PATTERN_1 = Pattern.compile(
        "(?:ETB|Birr|ብር)\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )
    private val AMOUNT_PATTERN_2 = Pattern.compile(
        "([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)\\s*(?:ETB|Birr|ብር)",
        Pattern.CASE_INSENSITIVE
    )

    // Transaction ID patterns
    private val REF_PATTERN = Pattern.compile(
        "(?:Transaction\\s*ID|Txn\\s*ID|Ref(?:erence)?|Txn)\\s*[:\\-]?\\s*([A-Za-z0-9_-]+)",
        Pattern.CASE_INSENSITIVE
    )

    fun isPaymentMessage(sender: String, body: String): Boolean {
        val lowerSender = sender.lowercase()
        val lowerBody = body.lowercase()

        val isCbeSender = lowerSender.contains("cbe") || lowerSender.contains("commercial bank") || lowerSender == "996"
        val isTelebirrSender = lowerSender.contains("telebirr") || lowerSender == "127" || lowerSender.contains("ethio")

        val hasPaymentKeywords = lowerBody.contains("received") ||
                lowerBody.contains("credited") ||
                lowerBody.contains("has been credited") ||
                lowerBody.contains("deposited") ||
                lowerBody.contains("transferred to you") ||
                lowerBody.contains("payment for") ||
                lowerBody.contains("ደርሷል") ||
                lowerBody.contains("ገባልዎ")

        val hasCurrency = lowerBody.contains("etb") || lowerBody.contains("birr") || lowerBody.contains("ብር")

        if ((isCbeSender || isTelebirrSender) && hasPaymentKeywords && hasCurrency) {
            return true
        }

        // Even if sender is masked or numeric, if body explicitly mentions CBE or telebirr and credited/received:
        val mentionsCbeOrTelebirr = lowerBody.contains("cbe") || lowerBody.contains("telebirr") || lowerBody.contains("cbebirr")
        return mentionsCbeOrTelebirr && hasPaymentKeywords && hasCurrency
    }

    fun parse(sender: String, body: String): ParsedPayment? {
        val lowerSender = sender.lowercase()
        val lowerBody = body.lowercase()

        val isTelebirr = lowerSender.contains("telebirr") ||
                lowerSender == "127" ||
                lowerBody.contains("telebirr")

        val isCbe = lowerSender.contains("cbe") ||
                lowerSender.contains("commercial bank") ||
                lowerSender == "996" ||
                lowerBody.contains("cbe") ||
                lowerBody.contains("commercial bank of ethiopia")

        val serviceType = when {
            isTelebirr -> PaymentServiceType.TELEBIRR
            isCbe -> PaymentServiceType.CBE
            else -> PaymentServiceType.OTHER
        }

        val amount = extractAmount(body) ?: return null
        val (payerName, payerPhone) = extractPayerInfo(body, serviceType)
        val ref = extractReference(body)

        val speech = generateDefaultSpeech(serviceType, amount, payerName, payerPhone)

        return ParsedPayment(
            serviceType = serviceType,
            senderAddress = sender,
            amount = amount,
            currency = "ETB",
            payerName = payerName,
            payerPhone = payerPhone,
            accountOrRef = ref,
            rawBody = body,
            speechAnnouncement = speech
        )
    }

    private fun extractAmount(body: String): Double? {
        // Try pattern 1: ETB 500.00
        val matcher1 = AMOUNT_PATTERN_1.matcher(body)
        if (matcher1.find()) {
            val amountStr = matcher1.group(1)?.replace(",", "")
            amountStr?.toDoubleOrNull()?.let { return it }
        }

        // Try pattern 2: 500.00 ETB
        val matcher2 = AMOUNT_PATTERN_2.matcher(body)
        if (matcher2.find()) {
            val amountStr = matcher2.group(1)?.replace(",", "")
            amountStr?.toDoubleOrNull()?.let { return it }
        }

        return null
    }

    private fun extractReference(body: String): String? {
        val matcher = REF_PATTERN.matcher(body)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    private fun extractPayerInfo(body: String, serviceType: PaymentServiceType): Pair<String?, String?> {
        var name: String? = null
        var phone: String? = null

        when (serviceType) {
            PaymentServiceType.TELEBIRR -> {
                // e.g.: "you have received 500.00 ETB from John Doe (251911223344)."
                // or "from 0911223344"
                // or "from John Doe."
                val pattern = Pattern.compile(
                    "from\\s+([A-Za-z0-9\\s.'-]+?)(?:\\s*\\(([0-9+]+)\\)|\\.\\s*|\\s+Transaction|\\s+on|\\s+Your)",
                    Pattern.CASE_INSENSITIVE
                )
                val matcher = pattern.matcher(body)
                if (matcher.find()) {
                    val rawName = matcher.group(1)?.trim()
                    val rawPhone = matcher.group(2)?.trim()

                    if (rawPhone != null) {
                        phone = rawPhone
                    }

                    if (rawName != null) {
                        if (rawName.matches(Regex("^[0-9+]+$"))) {
                            phone = rawName
                        } else {
                            name = cleanName(rawName)
                        }
                    }
                }
            }
            PaymentServiceType.CBE -> {
                // e.g.: "credited to your account 1000... from ABEBE KEBEDE on 08/10/2026. Ref: FT..."
                // or "from 251911223344 (Abebe Kebede)"
                val pattern = Pattern.compile(
                    "from\\s+([A-Za-z0-9\\s.'-]+?)(?:\\s*\\(([A-Za-z0-9\\s.'-]+)\\)|\\s+on\\s+|\\.\\s*|\\s+Ref|\\s+Reason|\\s+Current|\\s+Balance)",
                    Pattern.CASE_INSENSITIVE
                )
                val matcher = pattern.matcher(body)
                if (matcher.find()) {
                    val primary = matcher.group(1)?.trim()
                    val secondary = matcher.group(2)?.trim()

                    if (primary != null) {
                        if (primary.matches(Regex("^[0-9+]+$"))) {
                            phone = primary
                            if (secondary != null) {
                                name = cleanName(secondary)
                            }
                        } else {
                            name = cleanName(primary)
                            if (secondary != null && secondary.matches(Regex("^[0-9+]+$"))) {
                                phone = secondary
                            }
                        }
                    }
                }
            }
            PaymentServiceType.OTHER -> {
                val pattern = Pattern.compile("from\\s+([A-Za-z0-9\\s.'-]+?)(?:\\.|\\s+on|\\s+Ref)", Pattern.CASE_INSENSITIVE)
                val matcher = pattern.matcher(body)
                if (matcher.find()) {
                    name = matcher.group(1)?.trim()?.let { cleanName(it) }
                }
            }
        }

        return Pair(name, phone)
    }

    private fun cleanName(raw: String): String {
        return raw.trim()
            .replace(Regex("^(account|acc)\\s+[0-9*]+", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    fun generateDefaultSpeech(
        serviceType: PaymentServiceType,
        amount: Double,
        payerName: String?,
        payerPhone: String?,
        customTemplate: String? = null
    ): String {
        val formattedAmount = formatCurrencyAmount(amount)
        val serviceName = when (serviceType) {
            PaymentServiceType.CBE -> "CBE"
            PaymentServiceType.TELEBIRR -> "Telebirr"
            PaymentServiceType.OTHER -> "Mobile Bank"
        }

        val senderInfo = when {
            !payerName.isNullOrBlank() -> payerName
            !payerPhone.isNullOrBlank() -> "phone ending in ${payerPhone.takeLast(4)}"
            else -> "customer"
        }

        if (!customTemplate.isNullOrBlank()) {
            return customTemplate
                .replace("{amount}", formattedAmount)
                .replace("{sender}", senderInfo)
                .replace("{service}", serviceName)
                .replace("{currency}", "Birr")
        }

        return when (serviceType) {
            PaymentServiceType.TELEBIRR -> {
                "Telebirr alert! Received $formattedAmount Birr from $senderInfo."
            }
            PaymentServiceType.CBE -> {
                "CBE payment received! $formattedAmount Birr from $senderInfo."
            }
            PaymentServiceType.OTHER -> {
                "Payment received! $formattedAmount Birr from $senderInfo via $serviceName."
            }
        }
    }

    fun formatCurrencyAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            NumberFormat.getIntegerInstance(Locale.US).format(amount.toLong())
        } else {
            String.format(Locale.US, "%,.2f", amount)
        }
    }
}
