package com.sureja.accountant.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import javax.inject.Inject

const val FOREGROUND_SYNC_INTERVAL_MS = 60_000L

suspend fun runForegroundSync(connected: Flow<Boolean>, sync: suspend () -> Unit) {
    connected.distinctUntilChanged().collectLatest { online ->
        if (online) while (currentCoroutineContext().isActive) {
            sync()
            delay(FOREGROUND_SYNC_INTERVAL_MS)
        }
    }
}

class NetworkMonitor @Inject constructor(@ApplicationContext context: Context) {
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    val connected: Flow<Boolean> = callbackFlow {
        fun report() {
            trySend(manager.getNetworkCapabilities(manager.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = report()
            override fun onLost(network: Network) = report()
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = report()
        }
        manager.registerDefaultNetworkCallback(callback)
        report()
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }
}
