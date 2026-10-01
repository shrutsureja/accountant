package com.sureja.accountant.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("accountant_preferences")

data class Session(val userId: String, val displayName: String, val username: String, val deviceId: String, val accessToken: String, val refreshToken: String)

@Singleton
class AuthStore @Inject constructor(@ApplicationContext private val context: Context, private val cipher: KeystoreCipher) {
    private object Keys {
        val userId = stringPreferencesKey("user_id"); val displayName = stringPreferencesKey("display_name"); val username = stringPreferencesKey("username")
        val deviceId = stringPreferencesKey("device_id"); val access = stringPreferencesKey("access_token"); val refresh = stringPreferencesKey("refresh_token")
        val lastSync = stringPreferencesKey("last_sync_at"); val biometric = booleanPreferencesKey("biometric_enabled"); val lastPayment = stringPreferencesKey("last_payment")
    }
    val hasSession: Flow<Boolean> = context.dataStore.data.map { it[Keys.refresh] != null }
    val userId: Flow<String?> = context.dataStore.data.map { it[Keys.userId] }
    val displayName: Flow<String> = context.dataStore.data.map { it[Keys.displayName].orEmpty() }
    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.biometric] ?: true }

    suspend fun session(): Session? {
        val p = context.dataStore.data.first()
        return Session(p[Keys.userId] ?: return null,p[Keys.displayName] ?: return null,p[Keys.username] ?: return null,p[Keys.deviceId] ?: return null,p[Keys.access]?.let(cipher::decrypt) ?: return null,p[Keys.refresh]?.let(cipher::decrypt) ?: return null)
    }
    suspend fun saveSession(session: Session) = context.dataStore.edit { p ->
        p[Keys.userId]=session.userId; p[Keys.displayName]=session.displayName; p[Keys.username]=session.username; p[Keys.deviceId]=session.deviceId
        p[Keys.access]=cipher.encrypt(session.accessToken); p[Keys.refresh]=cipher.encrypt(session.refreshToken)
    }
    suspend fun updateTokens(access: String, refresh: String) = context.dataStore.edit { it[Keys.access]=cipher.encrypt(access); it[Keys.refresh]=cipher.encrypt(refresh) }
    suspend fun clear() = context.dataStore.edit { it.clear() }
    suspend fun lastSync(): String? = context.dataStore.data.first()[Keys.lastSync]
    suspend fun setLastSync(value: String) = context.dataStore.edit { it[Keys.lastSync]=value }
    suspend fun setBiometric(value: Boolean) = context.dataStore.edit { it[Keys.biometric]=value }
    suspend fun lastPayment(): String = context.dataStore.data.first()[Keys.lastPayment] ?: "CASH"
    suspend fun setLastPayment(value: String) = context.dataStore.edit { it[Keys.lastPayment]=value }
}
