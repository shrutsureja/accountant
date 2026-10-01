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
    private val debit = Regex("(?i)\\b(debited|spent|paid|purchase|sent|transferred)\\b|\\bDr\\.\\s+from\\b")
    private val credit = Regex("(?i)\\b(credited|received|refund(?:ed)?)\\b")
    private val outgoingDespiteCredit = Regex("(?i)\\bdebited\\s+from\\b|\\bdebited\\b.{0,100}\\bcredited\\s+to\\b")
    private val unsuccessful = Regex("(?i)\\b(fail(?:ed|ure)?|declined|rejected|pending|revers(?:ed|al)|cancel(?:led|ed)|request|mandate)\\b")
    private val amount = Regex("(?i)(?:INR|Rs\\.?|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
    private val account = Regex("(?i)(?:a/c|acct|account)(?:\\s+(?:ending|no\\.?))?\\s*(?:\\.{2,}|xx|x|\\*)*([0-9]{4,16})")
    private val merchant = Regex("(?i)(?:at|to|towards)\\s+([A-Z0-9][A-Z0-9 .&_-]{1,40}?)(?:\\s+(?:on|via|ref|upi)|[.,]|$)")
    private val reference = Regex("(?i)(?:ref(?:erence)?|utr|txn(?: id)?)[: #.-]*([A-Z0-9]{6,30})")
    override fun canParse(message: String) = debit.containsMatchIn(message) &&
        (!credit.containsMatchIn(message) || outgoingDespiteCredit.containsMatchIn(message)) &&
        !unsuccessful.containsMatchIn(message)
    override fun parse(message: String): ParsedTransaction? {
        if (!canParse(message)) return null
        val raw = amount.find(message)?.groupValues?.get(1)?.replace(",", "")?.toBigDecimalOrNull() ?: return null
        val amountPaise = runCatching { raw.movePointRight(2).longValueExact() }.getOrNull()?.takeIf { it > 0 } ?: return null
        return ParsedTransaction(
            amountPaise = amountPaise,
            merchant = merchant.find(message)?.groupValues?.get(1)?.trim(),
            accountLast4 = account.find(message)?.groupValues?.get(1)?.takeLast(4),
            paymentMethod = if (message.contains("UPI", true) || Regex("\\b[\\w.-]+@[\\w.-]+\\b").containsMatchIn(message)) "UPI" else "OTHER",
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
class BankOfBarodaSmsParser(private val fallback: GenericDebitParser = GenericDebitParser()) : TransactionMessageParser {
    override fun canParse(message: String) = (message.contains("Bank of Baroda", true) || message.contains("-BOB", true)) && fallback.canParse(message)
    override fun parse(message: String) = if (canParse(message)) fallback.parse(message) else null
}
class KotakSmsParser(private val fallback: GenericDebitParser = GenericDebitParser()) : TransactionMessageParser {
    override fun canParse(message: String) = message.contains("Kotak", true) && fallback.canParse(message)
    override fun parse(message: String) = if (canParse(message)) fallback.parse(message) else null
}

class ParserRegistry(private val parsers: List<TransactionMessageParser> = listOf(HdfcSmsParser(),SbiSmsParser(),IciciSmsParser(),BankOfBarodaSmsParser(),KotakSmsParser(),GenericDebitParser())) {
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
