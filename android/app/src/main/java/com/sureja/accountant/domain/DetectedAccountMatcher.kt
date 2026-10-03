package com.sureja.accountant.domain

import com.sureja.accountant.data.local.AccountEntity
import com.sureja.accountant.data.local.PaymentMethod

/** Never guess between accounts sharing the same suffix or another member's account. */
fun matchDetectedAccount(accounts: List<AccountEntity>, last4: String?, memberId: String, payment: PaymentMethod): String? {
    if (last4 == null || !last4.matches(Regex("\\d{4}")) || payment == PaymentMethod.CASH) return null
    return accounts.filter {
        it.active && it.deletedAt == null && it.last4 == last4 && it.paymentMethod == payment &&
            (it.ownerUserId == null || it.ownerUserId == memberId)
    }.singleOrNull()?.id
}
