package com.sureja.accountant.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class TransactionDetectionTest {
    private val parser=GenericDebitParser()
    @Test fun parsesDebitSms(){val result=parser.parse("Rs. 845 debited from A/c XX1234 at AMAZON via UPI Ref 123ABC789")!!;assertEquals(84500,result.amountPaise);assertEquals("1234",result.accountLast4);assertEquals("UPI",result.paymentMethod);assertEquals("AMAZON",result.merchant)}
    @Test fun ignoresCredits(){assertNull(parser.parse("INR 420 credited to account XX4432"))}
    @Test fun ignoresFailedAndPendingPayments(){assertNull(parser.parse("Rs 500 paid via UPI failed due to timeout"));assertNull(parser.parse("INR 200 debited, reversal pending"))}
    @Test fun ignoresZeroAndOverflowAmounts(){assertNull(parser.parse("Rs 0 debited from account 1234"));assertNull(parser.parse("INR 999999999999999999999999999 paid via UPI"))}
    @Test fun parsesBankOfBarodaTransfer(){val result=ParserRegistry().parse("Rs.420 transferred from A/c ...4432 to:UPI/123456789012. Total Bal:Rs.2000.00CR. - Bank of Baroda")!!;assertEquals(42000,result.amountPaise);assertEquals("4432",result.accountLast4);assertEquals("UPI",result.paymentMethod)}
    @Test fun parsesBankOfBarodaDebitAndCreditToPayee(){val result=ParserRegistry().parse("Rs 845.00 debited from A/C 1234567890 and credited to shop@upi UPI Ref:123456789012. -BOB")!!;assertEquals(84500,result.amountPaise);assertEquals("7890",result.accountLast4)}
    @Test fun parsesBankOfBarodaDrAndCr(){val result=ParserRegistry().parse("Rs.845.00 Dr. from A/C 1234567890 and Cr. to shop@upi. Ref:123456789012. -BOB")!!;assertEquals(84500,result.amountPaise);assertEquals("UPI",result.paymentMethod)}
    @Test fun parsesKotakSentPayment(){val result=ParserRegistry().parse("Sent Rs.420.00 from Kotak Bank A/c 12345 to Demo Store on 01-10-26. UPI Ref 123456789012")!!;assertEquals(42000,result.amountPaise);assertEquals("2345",result.accountLast4);assertEquals("Demo Store",result.merchant)}
    @Test fun ignoresUpiMandateRequest(){assertNull(ParserRegistry().parse("A user has sent UPI AutoPay request of amount upto Rs. 900.00 on BHIM. Check mandates."))}
    @Test fun ignoresSbiCredit(){assertNull(ParserRegistry().parse("Dear Customer, INR 1,200.00 credited to your A/c No 123456 on 01/10/2026 -SBI"))}
    @Test fun parsesDecimalAndCommas(){assertEquals(125050,parser.parse("INR 1,250.50 paid from account ending 4432 to DMART via UPI")!!.amountPaise)}
    @Test fun referenceFingerprintWinsAcrossSources(){val instant=Instant.parse("2026-09-30T10:00:00Z");assertEquals(TransactionFingerprint.create(84500,instant,"1234","Amazon","ABC12345"),TransactionFingerprint.create(84500,instant.plusSeconds(200),"1234","AMAZON","abc12345"))}
    @Test fun nearbyTransactionsShareFingerprint(){val instant=Instant.parse("2026-09-30T10:01:00Z");assertEquals(TransactionFingerprint.create(84500,instant,"1234","Amazon",null),TransactionFingerprint.create(84500,instant.plusSeconds(60),"1234","Amazon",null))}
}
