package com.sureja.accountant.di

import android.content.Context
import androidx.room.Room
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.sureja.accountant.BuildConfig
import com.sureja.accountant.data.local.AccountantDao
import com.sureja.accountant.data.local.AccountantDatabase
import com.sureja.accountant.data.network.AccountantApi
import com.sureja.accountant.data.network.RefreshRequest
import com.sureja.accountant.data.network.RefreshResponse
import com.sureja.accountant.data.preferences.AuthStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object AppModule {
    private val refreshLock = Any()

    @Provides @Singleton fun database(@ApplicationContext context: Context): AccountantDatabase = Room.databaseBuilder(context,AccountantDatabase::class.java,"accountant.db").build()
    @Provides fun dao(db: AccountantDatabase): AccountantDao = db.dao()
    @Provides @Singleton fun json(): Json = Json { ignoreUnknownKeys=true; explicitNulls=false; encodeDefaults=true }
    @Provides @Singleton fun api(store: AuthStore,json: Json): AccountantApi {
        val refreshClient = OkHttpClient()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = runBlocking { store.session()?.accessToken }
                chain.proceed(chain.request().newBuilder().apply { if (token != null) header("Authorization","Bearer $token") }.build())
            }
            .authenticator { _, response ->
                if (response.request.url.encodedPath.startsWith("/api/v1/auth/") || response.retryCount() > 1) return@authenticator null
                synchronized(refreshLock) {
                    val session = runBlocking { store.session() } ?: return@synchronized null
                    val currentHeader = "Bearer ${session.accessToken}"
                    if (response.request.header("Authorization") != currentHeader) {
                        return@synchronized response.request.newBuilder().header("Authorization", currentHeader).build()
                    }
                    val refreshBody = json.encodeToString(RefreshRequest(session.deviceId, session.refreshToken))
                        .toRequestBody("application/json".toMediaType())
                    val refreshUrl = response.request.url.newBuilder().encodedPath("/api/v1/auth/refresh").build()
                    val request = Request.Builder().url(refreshUrl).post(refreshBody).build()
                    runCatching {
                        refreshClient.newCall(request).execute().use { refreshResponse ->
                            if (!refreshResponse.isSuccessful) return@use null
                            val body = refreshResponse.body?.string() ?: return@use null
                            val tokens = json.decodeFromString<RefreshResponse>(body)
                            runBlocking { store.updateTokens(tokens.accessToken, tokens.refreshToken) }
                            response.request.newBuilder().header("Authorization", "Bearer ${tokens.accessToken}").build()
                        }
                    }.getOrNull()
                }
            }
            .addInterceptor(HttpLoggingInterceptor().apply { level=HttpLoggingInterceptor.Level.BASIC })
            .build()
        return Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL).client(client).addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(AccountantApi::class.java)
    }

    private fun Response.retryCount(): Int {
        var count = 1
        var prior = priorResponse
        while (prior != null) { count++; prior = prior.priorResponse }
        return count
    }
}
