package com.sureja.accountant.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import dagger.hilt.android.AndroidEntryPoint
import com.sureja.accountant.data.AccountantRepository
import com.sureja.accountant.data.local.TransactionSource
import com.sureja.accountant.domain.ParserRegistry
import kotlinx.coroutines.*
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class TransactionSmsReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: AccountantRepository
    override fun onReceive(context: Context,intent: Intent) {
        if(intent.action!=Telephony.Sms.Intents.SMS_RECEIVED_ACTION)return
        val pending=goAsync()
        CoroutineScope(SupervisorJob()+Dispatchers.IO).launch {
            try { Telephony.Sms.Intents.getMessagesFromIntent(intent).forEach { sms -> ParserRegistry().parse(sms.messageBody)?.let { repository.addDetected(it,TransactionSource.SMS,Instant.ofEpochMilli(sms.timestampMillis)) } } } finally { pending.finish() }
        }
    }
}

