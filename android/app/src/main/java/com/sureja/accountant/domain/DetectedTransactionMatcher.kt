package com.sureja.accountant.domain

import com.sureja.accountant.data.local.TransactionEntity
import com.sureja.accountant.data.local.TransactionSource
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Locale

/** Reconcile two capture channels without merging unrelated payments of the same amount. */
object DetectedTransactionMatcher {
    private const val MAX_SECONDS_APART = 120L

    fun isSamePayment(
        incoming: ParsedTransaction,
        incomingSource: TransactionSource,
        incomingAt: Instant,
        existing: TransactionEntity,
        existingAccountLast4: String?,
    ): Boolean {
        if (incomingSource == existing.source || existing.source == TransactionSource.MANUAL) return false
        if (incoming.amountPaise != existing.amountPaise) return false
        val existingAt = runCatching { OffsetDateTime.parse(existing.occurredAt).toInstant() }.getOrNull() ?: return false
        if (kotlin.math.abs(Duration.between(incomingAt, existingAt).seconds) > MAX_SECONDS_APART) return false

        val incomingRef = incoming.sourceReference?.trim()?.uppercase(Locale.ROOT)
        val existingRef = existing.sourceReference?.trim()?.uppercase(Locale.ROOT)
        if (!incomingRef.isNullOrEmpty() && !existingRef.isNullOrEmpty()) return incomingRef == existingRef

        // When one channel omits the bank reference, require both the funding account
        // and a sufficiently specific merchant. Amount and time alone are unsafe.
        if (incoming.accountLast4.isNullOrBlank() || incoming.accountLast4 != existingAccountLast4) return false
        val incomingMerchant = normalizeMerchant(incoming.merchant)
        val existingMerchant = normalizeMerchant(existing.merchant)
        return incomingMerchant.length >= 6 && existingMerchant.length >= 6 &&
            (incomingMerchant.contains(existingMerchant) || existingMerchant.contains(incomingMerchant))
    }

    private fun normalizeMerchant(value: String?): String = value.orEmpty()
        .uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9]"), "")
}
