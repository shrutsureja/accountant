package com.sureja.accountant.data

import android.content.Context
import com.sureja.accountant.BuildConfig
import com.sureja.accountant.data.network.AccountantApi
import com.sureja.accountant.domain.AppVersionPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateRepository @Inject constructor(
    @ApplicationContext context: Context, private val api: AccountantApi, private val json: Json,
) {
    private val preferences = context.getSharedPreferences("app-update", Context.MODE_PRIVATE)
    private val key = "policy:${BuildConfig.API_BASE_URL}"
    fun cached(): AppVersionPolicy? = runCatching {
        preferences.getString(key, null)?.let { json.decodeFromString<AppVersionPolicy>(it) }?.takeIf { it.isValid() }
    }.getOrNull()
    suspend fun refresh(): AppVersionPolicy? {
        val fresh = try { withTimeoutOrNull(4000) { api.versionPolicy() }?.takeIf { it.isValid() } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { null }
        if (fresh != null) preferences.edit().putString(key, json.encodeToString(fresh)).apply()
        return fresh ?: cached()
    }
}
