package com.sureja.accountant.data

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class MonthRangeTest {
    @Test fun selectedMonthIncludesItsDatesAndExcludesNextMonth() {
        val september = MonthRange.forMonth(YearMonth.of(2026, 9))
        val october = MonthRange.forMonth(YearMonth.of(2026, 10))
        assertTrue(september.from.startsWith("2026-09-01T00:00"))
        assertTrue(september.to.startsWith("2026-10-01T00:00"))
        assertTrue(october.from.startsWith("2026-10-01T00:00"))
        assertTrue(october.to.startsWith("2026-11-01T00:00"))
    }
}
