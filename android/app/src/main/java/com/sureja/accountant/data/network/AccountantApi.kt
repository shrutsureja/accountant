package com.sureja.accountant.data.network

import retrofit2.http.*
import okhttp3.ResponseBody

interface AccountantApi {
    @GET("api/v1/app/version") suspend fun versionPolicy(): com.sureja.accountant.domain.AppVersionPolicy
    @POST("api/v1/auth/login") suspend fun login(@Body body: LoginRequest): LoginResponse
    @POST("api/v1/auth/refresh") suspend fun refresh(@Body body: RefreshRequest): RefreshResponse
    @POST("api/v1/auth/logout") suspend fun logout()
    @GET("api/v1/members") suspend fun members(): MembersResponse
    @GET("api/v1/categories") suspend fun categories(): CategoriesResponse
    @GET("api/v1/accounts") suspend fun accounts(): AccountsResponse
    @POST("api/v1/sync") suspend fun sync(@Body body: SyncRequest): SyncResponse
    @GET("api/v1/issues") suspend fun issues(): IssuesResponse
    @POST("api/v1/issues") suspend fun createIssue(@Body body: CreateIssueRequest): CreateIssueResponse
    @GET("api/v1/issues/{id}") suspend fun issue(@Path("id") id: String): IssueDetailResponse
    @GET("api/v1/issues/{id}/screenshots/{imageId}") suspend fun issueScreenshot(@Path("id") id: String, @Path("imageId") imageId: String): ResponseBody
}
