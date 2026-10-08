package com.sureja.accountant.domain

import com.sureja.accountant.data.local.AccountEntity
import com.sureja.accountant.data.local.PaymentMethod
import org.junit.Assert.*
import org.junit.Test

class DetectedAccountMatcherTest {
    private val account = AccountEntity("bank-1", "Bank", last4 = "2307", ownerUserId = "member-1", paymentMethod = PaymentMethod.UPI, createdAt = "", updatedAt = "")
    @Test fun selectsUniqueMatchingAccount() {
        assertEquals("bank-1", matchDetectedAccount(listOf(account), "2307", "member-1", PaymentMethod.UPI))
    }
    @Test fun doesNotGuessBetweenDuplicateSuffixes() {
        assertNull(matchDetectedAccount(listOf(account, account.copy(id = "bank-2")), "2307", "member-1", PaymentMethod.UPI))
    }
    @Test fun excludesOtherMembersAndInactiveAccounts() {
        assertNull(matchDetectedAccount(listOf(account), "2307", "member-2", PaymentMethod.UPI))
        assertNull(matchDetectedAccount(listOf(account.copy(active = false)), "2307", "member-1", PaymentMethod.UPI))
        assertNull(matchDetectedAccount(listOf(account.copy(deletedAt = "deleted")), "2307", "member-1", PaymentMethod.UPI))
    }
    @Test fun requiresValidSuffixButMatchesTheSameBankAcrossDigitalPaymentMethods() {
        assertNull(matchDetectedAccount(listOf(account), null, "member-1", PaymentMethod.UPI))
        assertNull(matchDetectedAccount(listOf(account), "307", "member-1", PaymentMethod.UPI))
        assertEquals("bank-1", matchDetectedAccount(listOf(account), "2307", "member-1", PaymentMethod.OTHER))
        assertNull(matchDetectedAccount(listOf(account), "2307", "member-1", PaymentMethod.CASH))
    }
    @Test fun permitsUnambiguousSharedAccount() {
        assertEquals("bank-1", matchDetectedAccount(listOf(account.copy(ownerUserId = null)), "2307", "member-2", PaymentMethod.UPI))
    }
    @Test fun bankNameDisambiguatesMatchingSuffixes() {
        val kotak = account.copy(bankName = "Kotak")
        val sbi = account.copy(id = "bank-2", bankName = "SBI")
        assertEquals("bank-1", matchDetectedAccount(listOf(kotak,sbi), "2307", "member-1", PaymentMethod.OTHER, "Kotak"))
        assertNull(matchDetectedAccount(listOf(kotak,sbi), "2307", "member-1", PaymentMethod.OTHER))
    }
    @Test fun createsOnlyConfidentNewFundingAccounts() {
        assertTrue(canAutoCreateDetectedAccount(emptyList(), "5678", "Kotak", "member-1", PaymentMethod.OTHER, true))
        assertFalse(canAutoCreateDetectedAccount(emptyList(), "5678", null, "member-1", PaymentMethod.UPI, true))
        assertFalse(canAutoCreateDetectedAccount(emptyList(), "5678", "Kotak", "member-1", PaymentMethod.UPI, false))
        assertFalse(canAutoCreateDetectedAccount(listOf(account.copy(bankName = "Kotak", active = false)), "2307", "Kotak", "member-1", PaymentMethod.UPI, true))
        assertEquals(detectedAccountId("member-1", "Kotak", "5678"), detectedAccountId("member-1", "Kotak", "5678"))
        assertNotEquals(detectedAccountId("member-1", "Kotak", "5678"), detectedAccountId("member-2", "Kotak", "5678"))
    }
}
