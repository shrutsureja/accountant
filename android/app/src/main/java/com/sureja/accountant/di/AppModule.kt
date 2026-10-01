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
import com.sureja.accountant.data.preferences.AuthStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun database(@ApplicationContext context: Context): AccountantDatabase = Room.databaseBuilder(context,AccountantDatabase::class.java,"accountant.db").build()
    @Provides fun dao(db: AccountantDatabase): AccountantDao = db.dao()
    @Provides @Singleton fun json(): Json = Json { ignoreUnknownKeys=true; explicitNulls=false; encodeDefaults=true }
    @Provides @Singleton fun api(store: AuthStore,json: Json): AccountantApi {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = runBlocking { store.session()?.accessToken }
                chain.proceed(chain.request().newBuilder().apply { if (token != null) header("Authorization","Bearer $token") }.build())
            }
            .addInterceptor(HttpLoggingInterceptor().apply { level=HttpLoggingInterceptor.Level.BASIC })
            .build()
        return Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL).client(client).addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(AccountantApi::class.java)
    }
}
