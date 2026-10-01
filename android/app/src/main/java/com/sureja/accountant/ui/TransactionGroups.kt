package com.sureja.accountant.ui

import com.sureja.accountant.data.local.TransactionListItem
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class TransactionGroup(val date: LocalDate, val label: String, val items: List<TransactionListItem>)

fun groupTransactions(items: List<TransactionListItem>, today: LocalDate = LocalDate.now()): List<TransactionGroup> =
    items.groupBy { transaction ->
        runCatching { OffsetDateTime.parse(transaction.occurredAt).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate() }
            .recoverCatching { LocalDate.parse(transaction.occurredAt.take(10)) }
            .getOrDefault(LocalDate.MIN)
    }
        .toSortedMap(reverseOrder())
        .map { (date, dayItems) ->
            val label = when (date) {
                today -> "TODAY"
                today.minusDays(1) -> "YESTERDAY"
                else -> date.format(DateTimeFormatter.ofPattern("d MMM yyyy")).uppercase()
            }
            TransactionGroup(date, label, dayItems)
        }
