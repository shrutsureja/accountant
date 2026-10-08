package com.sureja.accountant.domain

/** Exact app IDs only: bank promotions and unrelated apps must never become expenses. */
object NotificationTransactionParser {
    private val upiApps = setOf(
        "com.google.android.apps.nbu.paisa.user", "in.org.npci.upiapp",
        "com.phonepe.app", "net.one97.paytm",
    )
    val supportedPackages = upiApps + setOf(
        "com.kotak.bank.mobile",
        "com.kotak811mobilebankingapp.instantsavingsupiscanandpayrecharge",
        "com.bankofbaroda.mconnect", "com.bankofbaroda.bobworlddmb",
        "com.sbi.lotusintouch",
    )
    private val parser = ParserRegistry()
    private val bankPackages = mapOf(
        "com.kotak.bank.mobile" to "Kotak",
        "com.kotak811mobilebankingapp.instantsavingsupiscanandpayrecharge" to "Kotak",
        "com.bankofbaroda.mconnect" to "Bank of Baroda",
        "com.bankofbaroda.bobworlddmb" to "Bank of Baroda",
        "com.sbi.lotusintouch" to "SBI",
    )
    private val promotion = Regex("(?i)\\b(cashback|reward|offer|win|insurance|reminder|due|otp)\\b")
    private val maskedDebitAccount = Regex("(?i)\\bdebited\\s+from\\s+(?:[x*]+)([0-9]{4})\\b")

    fun parse(packageName: String, title: String?, text: String?, bigText: String? = null): ParsedTransaction? {
        if (packageName !in supportedPackages) return null
        // Keep field boundaries: a body must not become part of the title's payee name.
        val parts = listOfNotNull(title, text, bigText).map(String::trim).filter(String::isNotEmpty).distinct()
        val content = parts.joinToString("\n")
        if (promotion.containsMatchIn(content)) return null
        val parsed = parser.parse(content) ?: return null
        val titleMerchant = title?.let { parser.parse(it)?.merchant }
        val debitedAccount = maskedDebitAccount.find(content)?.groupValues?.get(1)
        return parsed.copy(
            merchant = titleMerchant ?: parsed.merchant,
            accountLast4 = debitedAccount ?: parsed.accountLast4,
            paymentMethod = if (packageName in upiApps) "UPI" else parsed.paymentMethod,
            bankName = bankPackages[packageName],
            accountIsDebitSource = debitedAccount != null || parsed.accountIsDebitSource,
        )
    }
}
