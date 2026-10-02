package com.sureja.accountant.domain

import java.time.YearMonth
import java.time.ZoneId

data class SmsScanWindow(val fromInclusive: Long, val toExclusive: Long) {
    companion object {
        fun forMonth(month: YearMonth, zone: ZoneId = ZoneId.systemDefault()) = SmsScanWindow(
            month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli(),
        )
    }
}
