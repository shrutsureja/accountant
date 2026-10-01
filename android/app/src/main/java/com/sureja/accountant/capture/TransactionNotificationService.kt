package com.sureja.accountant.capture

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
import com.sureja.accountant.data.AccountantRepository
import com.sureja.accountant.data.local.TransactionSource
import com.sureja.accountant.domain.ParserRegistry
import kotlinx.coroutines.*
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class TransactionNotificationService : NotificationListenerService() {
    @Inject lateinit var repository: AccountantRepository
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val supported=setOf("com.google.android.apps.nbu.paisa.user","com.phonepe.app","net.one97.paytm")
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if(sbn.packageName !in supported)return
        val extras=sbn.notification.extras
        val content=listOfNotNull(extras.getCharSequence("android.title"),extras.getCharSequence("android.text"),extras.getCharSequence("android.bigText")).joinToString(" ")
        ParserRegistry().parse(content)?.let { parsed -> scope.launch { repository.addDetected(parsed,TransactionSource.NOTIFICATION,Instant.ofEpochMilli(sbn.postTime)) } }
    }
    override fun onDestroy(){scope.cancel();super.onDestroy()}
}

