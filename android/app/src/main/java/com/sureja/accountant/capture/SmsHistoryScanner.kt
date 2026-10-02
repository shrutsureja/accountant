package com.sureja.accountant.capture

import android.content.Context
import android.provider.Telephony
import dagger.hilt.android.qualifiers.ApplicationContext
import com.sureja.accountant.data.AccountantRepository
import com.sureja.accountant.data.local.TransactionSource
import com.sureja.accountant.domain.ParserRegistry
import com.sureja.accountant.domain.SmsScanWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.YearMonth
import javax.inject.Inject

class SmsHistoryScanner @Inject constructor(@ApplicationContext private val context: Context, private val repository: AccountantRepository) {
    private val scanMutex = Mutex()
    suspend fun scanMonth(month: YearMonth): Int = scanMutex.withLock { withContext(Dispatchers.IO) {
        var detected = 0
        val window = SmsScanWindow.forMonth(month)
        val parser = ParserRegistry()
        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE),
            "${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms.DATE} < ?",
            arrayOf(window.fromInclusive.toString(), window.toExclusive.toString()),
            "${Telephony.Sms.DATE} ASC",
        )?.use { cursor ->
            val body = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val date = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (cursor.moveToNext()) {
                currentCoroutineContext().ensureActive()
                parser.parse(cursor.getString(body))?.let {
                    if (repository.addDetected(it, TransactionSource.SMS, Instant.ofEpochMilli(cursor.getLong(date)))) detected++
                }
            }
        }
        detected
    } }
}
