package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PaymentServiceType {
    CBE,
    TELEBIRR,
    OTHER
}

@Entity(tableName = "payments")
data class PaymentRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serviceType: PaymentServiceType,
    val senderAddress: String,
    val amount: Double,
    val currency: String = "ETB",
    val payerName: String?,
    val payerPhone: String?,
    val accountOrRef: String?,
    val rawBody: String,
    val timestamp: Long = System.currentTimeMillis(),
    val announcedText: String,
    val isAnnounced: Boolean = true
)
