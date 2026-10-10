package com.example

import android.Manifest
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Looper
import android.provider.Telephony
import androidx.test.core.app.ApplicationProvider
import com.example.service.InboxSms
import com.example.service.SmsInboxMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SmsInboxMonitorTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val provider = InboxProvider()
    private val received = mutableListOf<InboxSms>()

    @Before
    fun setUp() {
        shadowOf(context as Application).grantPermissions(Manifest.permission.READ_SMS)
        ShadowContentResolver.registerProviderInternal("sms", provider)
        provider.rows.add(Row(1, 900, "127", "Old received ETB 10", 800))
    }

    @Test
    fun providerNotificationDetectsNewSmsWithoutABroadcast() = runTest {
        val monitor = SmsInboxMonitor(context, this, { received.add(it) }, { 1000 })
        monitor.start()
        runCurrent()
        assertEquals(0, received.size)
        provider.rows.add(Row(2, 1100, "127", "You have received ETB 10.0", 1050))

        context.contentResolver.notifyChange(Telephony.Sms.Inbox.CONTENT_URI, null)
        shadowOf(Looper.getMainLooper()).idle()
        runCurrent()
        assertEquals(listOf(InboxSms("127", "You have received ETB 10.0", 1050)), received)

        context.contentResolver.notifyChange(Telephony.Sms.CONTENT_URI, null)
        shadowOf(Looper.getMainLooper()).idle()
        runCurrent()
        assertEquals(1, received.size)
        monitor.stop()
    }

    @Test
    fun periodicCheckHandlesMissingProviderNotifications() = runTest {
        val monitor = SmsInboxMonitor(context, this, { received.add(it) }, { 1000 })
        monitor.start()
        runCurrent()
        provider.rows.add(Row(2, 1100, "996", "Your account was credited ETB 20", 1050))
        advanceTimeBy(5000)
        runCurrent()
        assertEquals(1, received.size)
        monitor.stop()
        provider.rows.add(Row(3, 1200, "996", "Your account was credited ETB 30", 1150))
        advanceTimeBy(5000)
        runCurrent()
        assertEquals(1, received.size)
    }

    data class Row(val id: Long, val date: Long, val address: String, val body: String, val sent: Long)

    class InboxProvider : ContentProvider() {
        val rows = mutableListOf<Row>()
        override fun onCreate() = true
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
            check(uri == Telephony.Sms.Inbox.CONTENT_URI)
            val columns = projection!!
            val cursor = MatrixCursor(columns)
            val args = selectionArgs!!
            rows.filter { it.id > args[0].toLong() && it.date >= args[1].toLong() }.sortedBy { it.id }.forEach { row ->
                val values: Map<String, Any> = mapOf(Telephony.Sms._ID to row.id, Telephony.Sms.ADDRESS to row.address,
                    Telephony.Sms.BODY to row.body, Telephony.Sms.DATE_SENT to row.sent)
                cursor.addRow(columns.map { values[it] }.toTypedArray())
            }
            return cursor
        }
        override fun getType(uri: Uri): String = "vnd.android.cursor.dir/sms"
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
    }
}
