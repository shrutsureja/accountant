package com.sureja.accountant.capture

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import com.sureja.accountant.data.AccountantRepository
import com.sureja.accountant.data.local.TransactionSource
import com.sureja.accountant.domain.NotificationTransactionParser
import kotlinx.coroutines.*
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class TransactionNotificationService : NotificationListenerService() {
    @Inject lateinit var repository: AccountantRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Recover still-visible payments after Android reconnects the listener.
        runCatching { activeNotifications }.getOrNull()?.forEach(::onNotificationPosted)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in NotificationTransactionParser.supportedPackages) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val extras = sbn.notification.extras
        val parsed = NotificationTransactionParser.parse(
            sbn.packageName,
            extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
        ) ?: return
        scope.launch {
            try {
                repository.addDetected(parsed, TransactionSource.NOTIFICATION, Instant.ofEpochMilli(sbn.postTime))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Never log financial notification text or exception payloads.
                Log.w("AccountantCapture", "Could not store a detected notification")
            }
        }
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
