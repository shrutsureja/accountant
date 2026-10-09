package com.sureja.accountant.domain

import com.sureja.accountant.data.local.PaymentMethod
import com.sureja.accountant.data.local.SyncStatus
import com.sureja.accountant.data.local.TransactionEntity
import com.sureja.accountant.data.local.TransactionSource
import com.sureja.accountant.data.local.TransactionStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DetectedTransactionMatcherTest {
    private val at = Instant.parse("2026-10-08T16:11:17Z")
    private val sms = TransactionEntity(
        id="sms", amountPaise=14000, paidByUserId="user-shrut", paymentMethod=PaymentMethod.UPI,
        accountId="bank", merchant="SHOP NAME LIMITED", occurredAt="2026-10-08T21:41:17+05:30",
        source=TransactionSource.SMS, status=TransactionStatus.DETECTED, sourceReference="123456789012",
        createdByUserId="user-shrut", updatedByUserId="user-shrut", createdAt="now", updatedAt="now",
        syncStatus=SyncStatus.SYNCED,
    )

    @Test fun matchesSmsAndNotificationWithDifferentDescriptionsAndOnlyOneReference() {
        val notification=ParsedTransaction(14000,"SHOP NAME LIMITED MARKET","2307","OTHER")
        assertTrue(DetectedTransactionMatcher.isSamePayment(notification,TransactionSource.NOTIFICATION,at.plusSeconds(5),sms,"2307"))
    }

    @Test fun requiresMatchingAccountAndSpecificMerchantWhenReferenceMissing() {
        val incoming=ParsedTransaction(14000,"SHOP NAME LIMITED","2307","UPI")
        assertFalse(DetectedTransactionMatcher.isSamePayment(incoming,TransactionSource.NOTIFICATION,at.plusSeconds(5),sms,"9999"))
        assertFalse(DetectedTransactionMatcher.isSamePayment(incoming.copy(merchant="OTHER"),TransactionSource.NOTIFICATION,at.plusSeconds(5),sms,"2307"))
        assertFalse(DetectedTransactionMatcher.isSamePayment(incoming,TransactionSource.NOTIFICATION,at.plusSeconds(121),sms,"2307"))
        assertFalse(DetectedTransactionMatcher.isSamePayment(incoming,TransactionSource.SMS,at.plusSeconds(5),sms,"2307"))
    }

    @Test fun distinctReferencesAreNotMerged() {
        val incoming=ParsedTransaction(14000,"SHOP NAME LIMITED","2307","UPI","different")
        assertFalse(DetectedTransactionMatcher.isSamePayment(incoming,TransactionSource.NOTIFICATION,at.plusSeconds(5),sms,"2307"))
    }
}
