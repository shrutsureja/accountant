package com.sureja.accountant.domain

import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale

data class ParsedTransaction(
    val amountPaise: Long,
    val merchant: String?,
    val accountLast4: String?,
    val paymentMethod: String,
    val sourceReference: String? = null,
)

interface TransactionMessageParser {
    fun canParse(message: String): Boolean
    fun parse(message: String): ParsedTransaction?
}

class GenericDebitParser : TransactionMessageParser {
    private val debit = Regex("(?i)\\b(debited|spent|paid|purchase)\\b")
    private val credit = Regex("(?i)\\b(credited|received|refund(?:ed)?)\\b")
    private val amount = Regex("(?i)(?:INR|Rs\\.?|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
    private val account = Regex("(?i)(?:a/c|acct|account)(?:\\s+(?:ending|no\\.?))?\\s*(?:xx|x|\\*)*([0-9]{4})")
    private val merchant = Regex("(?i)(?:at|to|towards)\\s+([A-Z0-9][A-Z0-9 .&_-]{1,40}?)(?:\\s+(?:on|via|ref|upi)|[.,]|$)")
    private val reference = Regex("(?i)(?:ref(?:erence)?|utr|txn(?: id)?)[: #.-]*([A-Z0-9]{6,30})")
    override fun canParse(message: String) = debit.containsMatchIn(message) && !credit.containsMatchIn(message)
    override fun parse(message: String): ParsedTransaction? {
        if (!canParse(message)) return null
        val raw = amount.find(message)?.groupValues?.get(1)?.replace(",", "")?.toBigDecimalOrNull() ?: return null
        return ParsedTransaction(
            amountPaise = raw.movePointRight(2).longValueExact(),
            merchant = merchant.find(message)?.groupValues?.get(1)?.trim(),
            accountLast4 = account.find(message)?.groupValues?.get(1),
            paymentMethod = if (message.contains("UPI", true)) "UPI" else "OTHER",
            sourceReference = reference.find(message)?.groupValues?.get(1),
        )
    }
}

class HdfcSmsParser(private val fallback: GenericDebitParser = GenericDebitParser()) : TransactionMessageParser {
    override fun canParse(message: String) = message.contains("HDFC", true) && fallback.canParse(message)
    override fun parse(message: String) = if (canParse(message)) fallback.parse(message) else null
}
class SbiSmsParser(private val fallback: GenericDebitParser = GenericDebitParser()) : TransactionMessageParser {
    override fun canParse(message: String) = (message.contains("SBI", true) || message.contains("State Bank", true)) && fallback.canParse(message)
    override fun parse(message: String) = if (canParse(message)) fallback.parse(message) else null
}
class IciciSmsParser(private val fallback: GenericDebitParser = GenericDebitParser()) : TransactionMessageParser {
    override fun canParse(message: String) = message.contains("ICICI", true) && fallback.canParse(message)
    override fun parse(message: String) = if (canParse(message)) fallback.parse(message) else null
}

class ParserRegistry(private val parsers: List<TransactionMessageParser> = listOf(HdfcSmsParser(),SbiSmsParser(),IciciSmsParser(),GenericDebitParser())) {
    fun parse(message: String): ParsedTransaction? = parsers.firstOrNull { it.canParse(message) }?.parse(message)
}

object TransactionFingerprint {
    fun create(amountPaise: Long, occurredAt: Instant, accountLast4: String?, merchant: String?, reference: String?): String {
        if (!reference.isNullOrBlank()) return sha("ref:${reference.uppercase(Locale.ROOT)}")
        val bucket = occurredAt.truncatedTo(ChronoUnit.MINUTES).epochSecond / 300
        val normalizedMerchant = merchant.orEmpty().uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9]"), "")
        return sha("$amountPaise|$bucket|${accountLast4.orEmpty()}|$normalizedMerchant")
    }
    private fun sha(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}

