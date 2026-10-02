package com.sureja.accountant.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.sureja.accountant.data.AccountantRepository
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(@Assisted context: Context,@Assisted params: WorkerParameters,private val repository: AccountantRepository): CoroutineWorker(context,params) {
    override suspend fun doWork() = if(repository.sync().isSuccess) Result.success() else Result.retry()
    companion object {
        fun cancelPeriodic(context: Context) { WorkManager.getInstance(context).cancelUniqueWork("accountant-sync") }
        fun now(context: Context) { WorkManager.getInstance(context).enqueueUniqueWork("accountant-sync-now",ExistingWorkPolicy.REPLACE,OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()) }
    }
}

