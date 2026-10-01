package com.sureja.accountant.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class TransactionDetectionTest {
    private val parser=GenericDebitParser()
    @Test fun parsesDebitSms(){val result=parser.parse("Rs. 845 debited from A/c XX1234 at AMAZON via UPI Ref 123ABC789")!!;assertEquals(84500,result.amountPaise);assertEquals("1234",result.accountLast4);assertEquals("UPI",result.paymentMethod);assertEquals("AMAZON",result.merchant)}
    @Test fun ignoresCredits(){assertNull(parser.parse("INR 420 credited to account XX4432"))}
    @Test fun parsesDecimalAndCommas(){assertEquals(125050,parser.parse("INR 1,250.50 paid from account ending 4432 to DMART via UPI")!!.amountPaise)}
    @Test fun referenceFingerprintWinsAcrossSources(){val instant=Instant.parse("2026-09-30T10:00:00Z");assertEquals(TransactionFingerprint.create(84500,instant,"1234","Amazon","ABC12345"),TransactionFingerprint.create(84500,instant.plusSeconds(200),"1234","AMAZON","abc12345"))}
    @Test fun nearbyTransactionsShareFingerprint(){val instant=Instant.parse("2026-09-30T10:01:00Z");assertEquals(TransactionFingerprint.create(84500,instant,"1234","Amazon",null),TransactionFingerprint.create(84500,instant.plusSeconds(60),"1234","Amazon",null))}
}

