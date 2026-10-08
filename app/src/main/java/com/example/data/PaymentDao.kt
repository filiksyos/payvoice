package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY timestamp DESC")
    fun getAllPayments(): Flow<List<PaymentRecord>>

    @Query("SELECT * FROM payments WHERE serviceType = :type ORDER BY timestamp DESC")
    fun getPaymentsByType(type: PaymentServiceType): Flow<List<PaymentRecord>>

    @Query("SELECT * FROM payments ORDER BY timestamp DESC LIMIT 1")
    fun getLatestPayment(): Flow<PaymentRecord?>

    @Query("SELECT COUNT(*) FROM payments WHERE timestamp >= :sinceTimestamp")
    fun getPaymentCountSince(sinceTimestamp: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE timestamp >= :sinceTimestamp")
    fun getTotalAmountSince(sinceTimestamp: Long): Flow<Double>

    @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
    suspend fun getPaymentById(id: Long): PaymentRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentRecord): Long

    @Delete
    suspend fun deletePayment(payment: PaymentRecord)

    @Query("DELETE FROM payments")
    suspend fun clearAllPayments()
}
