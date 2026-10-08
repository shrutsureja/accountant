package com.sureja.accountant.domain

import com.sureja.accountant.data.local.AccountEntity
import com.sureja.accountant.data.local.PaymentMethod
import java.nio.charset.StandardCharsets
import java.util.UUID

/** Match the funding account, regardless of whether this debit used UPI or a card. */
fun matchDetectedAccount(accounts: List<AccountEntity>, last4: String?, memberId: String, payment: PaymentMethod, bankName: String? = null): String? {
    if (last4 == null || !last4.matches(Regex("\\d{4}")) || payment == PaymentMethod.CASH) return null
    return accounts.filter {
        it.active && it.deletedAt == null && it.last4 == last4 && it.paymentMethod != PaymentMethod.CASH &&
            (bankName == null || it.bankName == null || it.bankName.equals(bankName, ignoreCase = true)) &&
            (it.ownerUserId == null || it.ownerUserId == memberId)
    }.singleOrNull()?.id
}

private val recognizedBanks = setOf("HDFC", "SBI", "ICICI", "Bank of Baroda", "Kotak")

/** Unknown bank, ambiguous suffix, or an archived matching account stays for manual review. */
fun canAutoCreateDetectedAccount(accounts: List<AccountEntity>, last4: String?, bankName: String?, memberId: String, payment: PaymentMethod, debitSource: Boolean): Boolean {
    if (!debitSource || payment == PaymentMethod.CASH || last4?.matches(Regex("\\d{4}")) != true || bankName !in recognizedBanks) return false
    return accounts.none {
        it.last4 == last4 && (it.ownerUserId == null || it.ownerUserId == memberId) &&
            (it.bankName == null || it.bankName.equals(bankName, ignoreCase = true))
    }
}

fun detectedAccountId(memberId: String, bankName: String, last4: String): String = UUID.nameUUIDFromBytes(
    "detected-account-v1|$memberId|${bankName.lowercase()}|$last4".toByteArray(StandardCharsets.UTF_8),
).toString()
