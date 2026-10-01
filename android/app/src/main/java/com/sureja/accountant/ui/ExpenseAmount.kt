package com.sureja.accountant.ui

internal fun parseAmountPaise(text: String): Long? = runCatching {
    text.trim().toBigDecimal().movePointRight(2).longValueExact()
}.getOrNull()?.takeIf { it > 0 }
