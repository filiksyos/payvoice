package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.PaymentDatabase
import com.example.data.PaymentSmsProcessor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PaymentSmsProcessorTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: PaymentDatabase
    private val announcements = mutableListOf<String>()
    private val body = "Dear, You have received ETB 10.0 from Commercial Bank of Ethiopia. Thank you for using telebirr Ethio telecom."

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, PaymentDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() { database.close() }

    private fun processor() = PaymentSmsProcessor(context, database) { text, _ -> announcements.add(text) }

    @Test
    fun broadcastAndInboxOnlySaveAndAnnounceOnce() = runBlocking {
        processor().process("127", body, 1000, "Broadcast")
        processor().process("127", body, 1000, "Inbox")
        assertEquals(1, database.paymentDao().getPaymentCountSince(0).first())
        assertEquals(1, announcements.size)
    }

    @Test
    fun differentSmsTimestampsAreSeparatePayments() = runBlocking {
        processor().process("127", body, 1000, "Inbox")
        processor().process("127", body, 2000, "Inbox")
        assertEquals(2, database.paymentDao().getPaymentCountSince(0).first())
        assertEquals(2, announcements.size)
    }

    @Test
    fun inboxWithoutSentTimestampMatchesTheBroadcast() = runBlocking {
        processor().process("127", body, 0, "Inbox")
        processor().process("127", body, 1000, "Broadcast")
        assertEquals(1, database.paymentDao().getPaymentCountSince(0).first())
        assertEquals(1, announcements.size)
    }

    @Test
    fun migrationPreservesExistingHistory() = runBlocking {
        database.close()
        val name = "migration-test.db"
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            old.execSQL("""CREATE TABLE payments (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                serviceType TEXT NOT NULL, senderAddress TEXT NOT NULL, amount REAL NOT NULL,
                currency TEXT NOT NULL, payerName TEXT, payerPhone TEXT, accountOrRef TEXT,
                rawBody TEXT NOT NULL, timestamp INTEGER NOT NULL, announcedText TEXT NOT NULL,
                isAnnounced INTEGER NOT NULL)""")
            old.execSQL("""INSERT INTO payments (id, serviceType, senderAddress, amount, currency,
                rawBody, timestamp, announcedText, isAnnounced)
                VALUES (7, 'TELEBIRR', '127', 10.0, 'ETB', 'old payment', 1000, 'Received 10 Birr', 1)""")
            old.version = 1
        }
        database = Room.databaseBuilder(context, PaymentDatabase::class.java, name)
            .addMigrations(PaymentDatabase.MIGRATION_1_2).allowMainThreadQueries().build()
        val record = database.paymentDao().getPaymentById(7)!!
        assertEquals(10.0, record.amount, 0.001)
        assertNull(record.smsMessageKey)
        assertEquals(1, database.paymentDao().getPaymentCountSince(0).first())
    }
}
