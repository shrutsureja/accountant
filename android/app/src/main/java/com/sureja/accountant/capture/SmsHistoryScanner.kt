package com.sureja.accountant.capture

import android.content.Context
import android.provider.Telephony
import dagger.hilt.android.qualifiers.ApplicationContext
import com.sureja.accountant.data.AccountantRepository
import com.sureja.accountant.data.local.TransactionSource
import com.sureja.accountant.domain.ParserRegistry
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class SmsHistoryScanner @Inject constructor(@ApplicationContext private val context:Context,private val repository:AccountantRepository) {
    suspend fun scanLast30Days():Int {
        var detected=0;val since=Instant.now().minus(30,ChronoUnit.DAYS).toEpochMilli();val projection=arrayOf(Telephony.Sms.BODY,Telephony.Sms.DATE)
        context.contentResolver.query(Telephony.Sms.Inbox.CONTENT_URI,projection,"${Telephony.Sms.DATE} >= ?",arrayOf(since.toString()),"${Telephony.Sms.DATE} ASC")?.use{cursor->val body=cursor.getColumnIndexOrThrow(Telephony.Sms.BODY);val date=cursor.getColumnIndexOrThrow(Telephony.Sms.DATE);while(cursor.moveToNext()){ParserRegistry().parse(cursor.getString(body))?.let{repository.addDetected(it,TransactionSource.SMS,Instant.ofEpochMilli(cursor.getLong(date)));detected++}}}
        return detected
    }
}

