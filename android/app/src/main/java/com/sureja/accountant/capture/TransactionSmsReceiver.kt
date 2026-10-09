package com.sureja.accountant.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
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
            try {
                val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (parts.isNotEmpty()) {
                    val body = parts.joinToString("") { it.messageBody.orEmpty() }
                    val receivedAt = parts.minOf { it.timestampMillis }
                    ParserRegistry().parse(body, parts.first().originatingAddress)?.let { if (repository.addDetected(it,TransactionSource.SMS,Instant.ofEpochMilli(receivedAt))) DetectionNotifier.show(context) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Keep private message contents out of diagnostic logs.
                Log.w("AccountantCapture", "Could not store a detected SMS")
            } finally { pending.finish() }
        }
    }
}
