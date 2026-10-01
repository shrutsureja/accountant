package com.sureja.accountant.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpenseAmountTest {
    @Test fun acceptsWholeRupeesAndExactPaise() {
        assertEquals(42000L, parseAmountPaise("420"))
        assertEquals(123456L, parseAmountPaise("1234.56"))
    }

    @Test fun rejectsZeroExtraDecimalsAndOverflow() {
        assertNull(parseAmountPaise("0"))
        assertNull(parseAmountPaise("2.345"))
        assertNull(parseAmountPaise("999999999999999999999"))
    }
}
