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
    val bankName: String? = null,
    val accountIsDebitSource: Boolean = false,
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
    private val debitedByAmount = Regex("(?i)\\bdebited\\s+by\\s+([0-9,]+(?:\\.[0-9]{1,2})?)\\b")
    private val account = Regex("(?i)(?:a/c|acct|account)(?:\\s+(?:ending|no\\.?))?\\s*(?:\\.{2,}|xx|x|\\*)*([0-9]{4,16})")
    private val debitedFromAccount = Regex("(?i)\\bdebited\\s+from\\s+(?:(?:a/c|acct|account)\\s*)?(?:[x*]+|\\.{2,})?([0-9]{4,16})\\b")
    private val transferFromAccount = Regex("(?i)\\b(?:sent|paid|spent|transferred)\\b.{0,45}?\\bfrom\\s+(?:[a-z ]{0,20})?(?:a/c|acct|account)\\s*(?:ending\\s*)?(?:[x*]+|\\.{2,})?([0-9]{4,16})\\b")
    private val accountDebited = Regex("(?i)\\b(?:a/c|acct|account)\\s*(?:[x*]+|\\.{2,})?([0-9]{4,16})\\s+(?:was\\s+|is\\s+)?debited\\b")
    private val merchant = Regex("(?i)(?:at|to|towards)\\s+([A-Z0-9][A-Z0-9 .&_-]{1,40}?)(?:\\s+(?:on|via|ref|upi)|[.,]|$)")
    private val reference = Regex("(?i)(?:ref(?:erence)?(?:no\\.?)?|utr|txn(?: id)?)[: #.-]*([A-Z0-9]{6,30})")
    override fun canParse(message: String) = debit.containsMatchIn(message) &&
        (!credit.containsMatchIn(message) || outgoingDespiteCredit.containsMatchIn(message)) &&
        !unsuccessful.containsMatchIn(message)
    override fun parse(message: String): ParsedTransaction? {
        if (!canParse(message)) return null
        val raw = (amount.find(message) ?: debitedByAmount.find(message))?.groupValues?.get(1)?.replace(",", "")?.toBigDecimalOrNull() ?: return null
        val amountPaise = runCatching { raw.movePointRight(2).longValueExact() }.getOrNull()?.takeIf { it > 0 } ?: return null
        val fundingAccount = debitedFromAccount.find(message)?.groupValues?.get(1)
            ?: transferFromAccount.find(message)?.groupValues?.get(1)
            ?: accountDebited.find(message)?.groupValues?.get(1)
        return ParsedTransaction(
            amountPaise = amountPaise,
            merchant = merchant.find(message)?.groupValues?.get(1)?.trim(),
            accountLast4 = fundingAccount?.takeLast(4) ?: account.find(message)?.groupValues?.get(1)?.takeLast(4),
            paymentMethod = if (message.contains("UPI", true) || Regex("\\b[\\w.-]+@[\\w.-]+\\b").containsMatchIn(message)) "UPI" else "OTHER",
            sourceReference = reference.find(message)?.groupValues?.get(1),
            accountIsDebitSource = fundingAccount != null,
        )
    }
}

class HdfcSmsParser(private val fallback: GenericDebitParser = GenericDebitParser()) : TransactionMessageParser {
    override fun canParse(message: String) = message.contains("HDFC", true) && fallback.canParse(message)
    override fun parse(message: String) = if (canParse(message)) fallback.parse(message)?.copy(bankName = "HDFC") else null
}
class SbiSmsParser(private val fallback: GenericDebitParser = GenericDebitParser()) : TransactionMessageParser {
    override fun canParse(message: String) = (message.contains("SBI", true) || message.contains("State Bank", true)) && fallback.canParse(message)
    override fun parse(message: String) = if (canParse(message)) fallback.parse(message)?.copy(bankName = "SBI") else null
}
class IciciSmsParser(private val fallback: GenericDebitParser = GenericDebitParser()) : TransactionMessageParser {
    override fun canParse(message: String) = message.contains("ICICI", true) && fallback.canParse(message)
    override fun parse(message: String) = if (canParse(message)) fallback.parse(message)?.copy(bankName = "ICICI") else null
}
class BankOfBarodaSmsParser(private val fallback: GenericDebitParser = GenericDebitParser()) : TransactionMessageParser {
    override fun canParse(message: String) = (message.contains("Bank of Baroda", true) || message.contains("-BOB", true)) && fallback.canParse(message)
    override fun parse(message: String) = if (canParse(message)) fallback.parse(message)?.copy(bankName = "Bank of Baroda") else null
}
class KotakSmsParser(private val fallback: GenericDebitParser = GenericDebitParser()) : TransactionMessageParser {
    override fun canParse(message: String) = message.contains("Kotak", true) && fallback.canParse(message)
    override fun parse(message: String) = if (canParse(message)) fallback.parse(message)?.copy(bankName = "Kotak") else null
}

class ParserRegistry(private val parsers: List<TransactionMessageParser> = listOf(HdfcSmsParser(),SbiSmsParser(),IciciSmsParser(),BankOfBarodaSmsParser(),KotakSmsParser(),GenericDebitParser())) {
    fun parse(message: String, sender: String? = null): ParsedTransaction? {
        val parsed = parsers.firstOrNull { it.canParse(message) }?.parse(message) ?: return null
        val source = sender?.uppercase(Locale.ROOT).orEmpty()
        val bank = when {
            "KOTAK" in source -> "Kotak"
            "BARODA" in source || "BOB" in source -> "Bank of Baroda"
            "HDFC" in source -> "HDFC"
            "ICICI" in source -> "ICICI"
            "SBI" in source -> "SBI"
            else -> null
        }
        return parsed.copy(bankName = bank)
    }
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
