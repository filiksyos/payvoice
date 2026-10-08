package com.example

import com.example.data.PaymentParser
import com.example.data.PaymentServiceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentParserTest {

    @Test
    fun testTelebirrPaymentParsing() {
        val sender = "telebirr"
        val body = "Dear Customer, you have received 500.00 ETB from Abebe Kebede (251911223344). Transaction ID: TB9928341. Your current balance is 2,450.00 ETB."

        assertTrue(PaymentParser.isPaymentMessage(sender, body))

        val parsed = PaymentParser.parse(sender, body)
        assertNotNull(parsed)
        assertEquals(PaymentServiceType.TELEBIRR, parsed!!.serviceType)
        assertEquals(500.0, parsed.amount, 0.001)
        assertEquals("Abebe Kebede", parsed.payerName)
        assertEquals("251911223344", parsed.payerPhone)
        assertEquals("TB9928341", parsed.accountOrRef)
        assertTrue(parsed.speechAnnouncement.contains("500 Birr"))
        assertTrue(parsed.speechAnnouncement.contains("Abebe Kebede"))
    }

    @Test
    fun testCbeAccountCreditParsing() {
        val sender = "CBE"
        val body = "Dear Customer, ETB 1,250.00 has been credited to your account 100023456789 from KEBEDE WORKU on 08/10/2026. Ref: FT260981234. Current balance is ETB 15,200.00."

        assertTrue(PaymentParser.isPaymentMessage(sender, body))

        val parsed = PaymentParser.parse(sender, body)
        assertNotNull(parsed)
        assertEquals(PaymentServiceType.CBE, parsed!!.serviceType)
        assertEquals(1250.0, parsed.amount, 0.001)
        assertEquals("KEBEDE WORKU", parsed.payerName)
        assertEquals("FT260981234", parsed.accountOrRef)
        assertTrue(parsed.speechAnnouncement.contains("1,250 Birr"))
        assertTrue(parsed.speechAnnouncement.contains("KEBEDE WORKU"))
    }

    @Test
    fun testCbeBirrWalletParsing() {
        val sender = "CBEBirr"
        val body = "You have received ETB 200.00 from 251912345678 (Tadesse Alemu). Transaction ID: 987654321. Your new balance is ETB 500.00."

        assertTrue(PaymentParser.isPaymentMessage(sender, body))

        val parsed = PaymentParser.parse(sender, body)
        assertNotNull(parsed)
        assertEquals(PaymentServiceType.CBE, parsed!!.serviceType)
        assertEquals(200.0, parsed.amount, 0.001)
        assertEquals("Tadesse Alemu", parsed.payerName)
        assertEquals("251912345678", parsed.payerPhone)
        assertEquals("987654321", parsed.accountOrRef)
    }

    @Test
    fun testNonPaymentSpamIgnored() {
        val sender = "telebirr"
        val spamBody = "Enjoy 100% weekend bonus package on all recharges! Dial *127# to subscribe."

        assertFalse(PaymentParser.isPaymentMessage(sender, spamBody))
    }
}
