package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class PaymentRepository(context: Context) {
    private val db = PaymentDatabase.getDatabase(context)
    private val dao = db.paymentDao()
    val preferencesManager = PreferencesManager(context)

    val allPayments: Flow<List<PaymentRecord>> = dao.getAllPayments()
    val latestPayment: Flow<PaymentRecord?> = dao.getLatestPayment()

    private fun getTodayStartTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getTodayPaymentCount(): Flow<Int> {
        return dao.getPaymentCountSince(getTodayStartTimestamp())
    }

    fun getTodayTotalAmount(): Flow<Double> {
        return dao.getTotalAmountSince(getTodayStartTimestamp())
    }

    fun getPaymentsByType(type: PaymentServiceType): Flow<List<PaymentRecord>> {
        return dao.getPaymentsByType(type)
    }

    suspend fun insertPayment(record: PaymentRecord): Long {
        return dao.insertPayment(record)
    }

    suspend fun deletePayment(record: PaymentRecord) {
        dao.deletePayment(record)
    }

    suspend fun clearAll() {
        dao.clearAllPayments()
    }
}
