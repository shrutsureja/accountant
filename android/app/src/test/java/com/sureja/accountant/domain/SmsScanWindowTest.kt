package com.sureja.accountant.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class SmsScanWindowTest {
    @Test fun usesLocalCalendarBoundariesRatherThanThirtyDays() {
        val w = SmsScanWindow.forMonth(YearMonth.of(2026, 10), ZoneId.of("Asia/Kolkata"))
        assertEquals(Instant.parse("2026-09-30T18:30:00Z").toEpochMilli(), w.fromInclusive)
        assertEquals(Instant.parse("2026-10-31T18:30:00Z").toEpochMilli(), w.toExclusive)
    }
    @Test fun includesLeapDayAndAdjacentMonthsDoNotOverlap() {
        val feb = SmsScanWindow.forMonth(YearMonth.of(2024, 2), ZoneId.of("UTC"))
        val march = SmsScanWindow.forMonth(YearMonth.of(2024, 3), ZoneId.of("UTC"))
        assertEquals(29L * 24 * 60 * 60 * 1000, feb.toExclusive - feb.fromInclusive)
        assertEquals(feb.toExclusive, march.fromInclusive)
    }
    @Test fun januaryPreviousMonthCrossesYearBoundary() {
        val w = SmsScanWindow.forMonth(YearMonth.of(2026, 1).minusMonths(1), ZoneId.of("UTC"))
        assertEquals(Instant.parse("2025-12-01T00:00:00Z").toEpochMilli(), w.fromInclusive)
        assertEquals(Instant.parse("2026-01-01T00:00:00Z").toEpochMilli(), w.toExclusive)
    }
}
