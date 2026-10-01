package com.sureja.accountant.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormattingTest {
    @Test fun wholeRupeesOmitDecimals() {
        assertEquals("₹1,234", money(123400))
    }

    @Test fun paiseRemainExact() {
        assertEquals("₹1,234.56", money(123456))
    }
}
