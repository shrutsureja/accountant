package com.sureja.accountant.ui

import com.sureja.accountant.data.local.TransactionListItem
import com.sureja.accountant.data.local.TransactionStatus
import java.time.Duration
import java.time.OffsetDateTime
import java.util.Locale

/** Advisory only: nearby payments may be separate, so this never changes transaction state. */
internal fun possibleReviewDuplicates(items: List<TransactionListItem>): Map<String, List<TransactionListItem>> {
    val result = mutableMapOf<String, MutableList<TransactionListItem>>()
    val groups = items.groupBy { it.paidByUserId to it.amountPaise }
    groups.values.forEach { group ->
        group.forEachIndexed { index, first ->
            val firstAt = runCatching { OffsetDateTime.parse(first.occurredAt).toInstant() }.getOrNull() ?: return@forEachIndexed
            group.drop(index + 1).forEach { second ->
                if (first.accountId != null && second.accountId != null && first.accountId != second.accountId) return@forEach
                val secondAt = runCatching { OffsetDateTime.parse(second.occurredAt).toInstant() }.getOrNull() ?: return@forEach
                if (kotlin.math.abs(Duration.between(firstAt, secondAt).seconds) <= 300) {
                    result.getOrPut(first.id) { mutableListOf() }.add(second)
                    result.getOrPut(second.id) { mutableListOf() }.add(first)
                }
            }
        }
    }
    return result
}

/** Exact merchant history is a hint, not an automatic category assignment. */
internal fun previousMerchantCategories(confirmed: List<TransactionListItem>): Map<String, String> {
    val grouped = confirmed.filter { it.status == TransactionStatus.CONFIRMED && it.categoryId != null && merchantKey(it.merchant) != null }
        .groupBy { merchantKey(it.merchant)!! }
    return grouped.mapNotNull { (merchant, transactions) ->
        val counts = transactions.groupingBy { it.categoryId!! }.eachCount()
        val highest = counts.maxByOrNull { it.value } ?: return@mapNotNull null
        // Conflicting history should not silently favor one category.
        if (counts.values.count { it == highest.value } > 1) null else merchant to highest.key
    }.toMap()
}

internal fun merchantKey(value: String?): String? {
    val key = value.orEmpty().trim().uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9]"), "")
    return key.takeIf { it.length >= 4 && it !in setOf("OTHER", "PAYTM", "UNKNOWNMERCHANT", "DETECTEDEXPENSE", "YOURBANKACCOUNT") }
}
