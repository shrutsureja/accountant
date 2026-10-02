package com.sureja.accountant.domain

import org.junit.Assert.*
import org.junit.Test

class NotificationTransactionParserTest {
    private val kotak = "com.kotak.bank.mobile"
    @Test fun smallKotakPaymentUsesTitleAndMaskedAccountBody() {
        val result = NotificationTransactionParser.parse(kotak, "₹20.00 paid to DEMO STORE", "Amount debited from XX1234. Check out details.")!!
        assertEquals(2000L, result.amountPaise)
        assertEquals("DEMO STORE", result.merchant)
        assertEquals("1234", result.accountLast4)
        assertEquals("OTHER", result.paymentMethod) // Bank app alone does not prove UPI.
    }
    @Test fun allSupportedAppsCaptureSmallDebits() {
        NotificationTransactionParser.supportedPackages.forEach { app ->
            assertEquals(app, 2000L, NotificationTransactionParser.parse(app, "Payment", "Rs 20 paid to DEMO via UPI")!!.amountPaise)
        }
    }
    @Test fun bhimAndGooglePaySupplyUpiContext() {
        listOf("in.org.npci.upiapp", "com.google.android.apps.nbu.paisa.user").forEach { app ->
            assertEquals("UPI", NotificationTransactionParser.parse(app, "You paid ₹20 to DEMO", null)!!.paymentMethod)
        }
    }
    @Test fun expandedNotificationAndRepeatedBodyAreHandled() {
        val result = NotificationTransactionParser.parse(kotak, "Payment", "Rs 20 paid to DEMO via UPI", "Rs 20 paid to DEMO via UPI")!!
        assertEquals("DEMO", result.merchant)
        assertEquals(2000L, result.amountPaise)
    }
    @Test fun unrelatedAppsAndBankMarketingAreIgnored() {
        assertNull(NotificationTransactionParser.parse("com.example.untrusted", "Rs 20 paid to DEMO", null))
        assertNull(NotificationTransactionParser.parse("com.bankofbaroda.mconnect", "Financial Protection for Your Family", "Protect your loved ones with Term Life Insurance"))
        assertNull(NotificationTransactionParser.parse(kotak, "Get Rs 20 cashback", "You paid with UPI"))
    }
    @Test fun creditsRequestsFailuresAndPendingPaymentsAreIgnoredEvenIfTitleLooksSuccessful() {
        listOf("Payment failed", "Payment pending", "Payment reversed", "Collect request", "Amount received", "Refund credited").forEach { body ->
            assertNull(body, NotificationTransactionParser.parse("in.org.npci.upiapp", "₹20 paid to DEMO", body))
        }
    }
    @Test fun noMinimumSpendButZeroIsRejected() {
        assertEquals(1L, NotificationTransactionParser.parse(kotak, "Rs 0.01 paid to DEMO", null)!!.amountPaise)
        assertNull(NotificationTransactionParser.parse(kotak, "Rs 0 paid to DEMO", null))
    }
}
