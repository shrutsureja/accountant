package com.sureja.accountant.ui

import com.sureja.accountant.data.local.PaymentMethod
import com.sureja.accountant.data.local.SyncStatus
import com.sureja.accountant.data.local.TransactionListItem
import com.sureja.accountant.data.local.TransactionSource
import com.sureja.accountant.data.local.TransactionStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TransactionGroupsTest {
    private fun item(id: String, date: String) = TransactionListItem(
        id = id, amountPaise = 100, categoryId = null, categoryName = null,
        paidByUserId = "user", memberName = "Member", paymentMethod = PaymentMethod.CASH,
        accountId = null, accountName = null, merchant = null, note = null, occurredAt = date,
        source = TransactionSource.MANUAL, status = TransactionStatus.CONFIRMED,
        createdByUserId = "user", updatedByUserId = "user", syncStatus = SyncStatus.SYNCED,
    )

    @Test fun groupsNewestDayFirstAndLabelsRelativeDays() {
        val today = LocalDate.of(2026, 10, 1)
        val groups = groupTransactions(
            listOf(item("old", "2026-09-30T12:00:00+05:30"), item("new", "2026-10-01T09:00:00+05:30")),
            today,
        )
        assertEquals(listOf("TODAY", "YESTERDAY"), groups.map { it.label })
        assertEquals(listOf("new", "old"), groups.flatMap { it.items }.map { it.id })
    }
}
