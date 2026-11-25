package com.sureja.accountant.ui

import com.sureja.accountant.data.local.PaymentMethod
import com.sureja.accountant.data.local.SyncStatus
import com.sureja.accountant.data.local.TransactionListItem
import com.sureja.accountant.data.local.TransactionSource
import com.sureja.accountant.data.local.TransactionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ReviewInsightsTest {
    private fun item(id: String, time: String, person: String = "shrut", amount: Long = 14000, account: String? = null, merchant: String? = null, category: String? = null) =
        TransactionListItem(id, amount, category, category, person, person, PaymentMethod.UPI, account, null, merchant, null, time,
            TransactionSource.SMS, TransactionStatus.DETECTED, person, SyncStatus.SYNCED, person)

    @Test fun flagsOnlyNearbySamePersonSameAmountWithoutConflictingAccounts() {
        val first = item("a", "2026-11-09T10:05:41+05:30")
        val near = item("b", "2026-11-09T10:07:05+05:30")
        val otherPerson = item("c", "2026-11-09T10:07:05+05:30", person = "alpa")
        val later = item("d", "2026-11-09T10:20:05+05:30")
        val differentAccount = item("e", "2026-11-09T10:06:05+05:30", account = "bank-2")
        val knownAccount = first.copy(accountId = "bank-1")
        val matches = possibleReviewDuplicates(listOf(knownAccount, near, otherPerson, later, differentAccount))
        assertEquals(listOf("b"), matches["a"]?.map { it.id })
        assertFalse(matches.containsKey("c"))
        assertFalse(matches.containsKey("d"))
        assertEquals(listOf("b"), matches["e"]?.map { it.id })
    }

    @Test fun suggestsExactMerchantOnlyWhenHistoryHasClearCategory() {
        val t = "2026-11-09T10:05:41+05:30"
        val history = listOf(
            item("a", t, merchant = "Zerodha Broking", category = "zerodha"),
            item("b", t, merchant = "ZERODHA-BROKING", category = "zerodha"),
            item("c", t, merchant = "Netflix", category = "utilities"),
            item("d", t, merchant = "NETFLIX", category = "entertainment"),
            item("e", t, merchant = "Paytm", category = "shopping"),
        ).map { it.copy(status = TransactionStatus.CONFIRMED) }
        val suggestions = previousMerchantCategories(history)
        assertEquals("zerodha", suggestions[merchantKey("Zerodha Broking")])
        assertNull(suggestions[merchantKey("Netflix")])
        assertNull(merchantKey("Paytm"))
    }
}
