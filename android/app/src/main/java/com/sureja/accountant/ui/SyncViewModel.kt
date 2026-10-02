package com.sureja.accountant.ui

import androidx.lifecycle.ViewModel
import com.sureja.accountant.data.AccountantRepository
import com.sureja.accountant.sync.NetworkMonitor
import com.sureja.accountant.sync.runForegroundSync
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(private val repository: AccountantRepository, private val network: NetworkMonitor) : ViewModel() {
    suspend fun runWhileOpen() = runForegroundSync(network.connected) { repository.sync() }
}
