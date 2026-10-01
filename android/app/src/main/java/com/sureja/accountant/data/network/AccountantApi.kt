package com.sureja.accountant.data.network

import retrofit2.http.*

interface AccountantApi {
    @POST("api/v1/auth/login") suspend fun login(@Body body: LoginRequest): LoginResponse
    @POST("api/v1/auth/refresh") suspend fun refresh(@Body body: RefreshRequest): RefreshResponse
    @POST("api/v1/auth/logout") suspend fun logout()
    @GET("api/v1/members") suspend fun members(): MembersResponse
    @GET("api/v1/categories") suspend fun categories(): CategoriesResponse
    @GET("api/v1/accounts") suspend fun accounts(): AccountsResponse
    @POST("api/v1/sync") suspend fun sync(@Body body: SyncRequest): SyncResponse
}

