package com.sureja.accountant.data.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable data class LoginRequest(val username: String,val pin: String,val deviceId: String,val deviceName: String)
@Serializable data class UserDto(val id: String,val username: String,val displayName: String,val avatarInitials: String)
@Serializable data class LoginResponse(val accessToken: String,val refreshToken: String,val expiresIn: Int,val user: UserDto)
@Serializable data class RefreshRequest(val deviceId: String,val refreshToken: String)
@Serializable data class RefreshResponse(val accessToken: String,val refreshToken: String,val expiresIn: Int)
@Serializable data class MembersResponse(val members: List<MemberDto>)
@Serializable data class MemberDto(val id: String,val username: String,val displayName: String,val avatarInitials: String,val createdAt: String)
@Serializable data class CategoriesResponse(val categories: List<CategoryDto>)
@Serializable data class CategoryDto(val id: String,val name: String,val icon: String="category",val active: Boolean=true,val createdAt: String?=null,val updatedAt: String?=null,val deletedAt: String?=null,val version: Int=1,val updatedBy: String?=null)
@Serializable data class AccountsResponse(val accounts: List<AccountDto>)
@Serializable data class AccountDto(val id: String,val name: String,val bankName: String?=null,val last4: String?=null,val ownerUserId: String?=null,val paymentMethod: String,val active: Boolean=true,val createdAt: String?=null,val updatedAt: String?=null,val deletedAt: String?=null,val version: Int=1,val updatedBy: String?=null)
@Serializable data class MerchantRuleDto(val id:String,val merchantPattern:String,val categoryId:String,val createdByUserId:String?=null,val createdAt:String?=null,val updatedAt:String?=null,val deletedAt:String?=null,val version:Int=1,val updatedBy:String?=null)
@Serializable data class TransactionDto(val id: String,val amountPaise: Long,val currency: String="INR",val categoryId: String?=null,val paidByUserId: String,val paymentMethod: String,val accountId: String?=null,val merchant: String?=null,val note: String?=null,val occurredAt: String,val source: String="MANUAL",val status: String="CONFIRMED",val sourceReference: String?=null,val fingerprint: String?=null,val createdByUserId: String?=null,val updatedByUserId: String?=null,val createdAt: String?=null,val updatedAt: String?=null,val deletedAt: String?=null,val version: Int=1)
@Serializable data class TransactionResponse(val transaction: TransactionDto)
@Serializable data class TransactionsResponse(val transactions: List<TransactionDto>)
@Serializable data class SyncChangeDto(val entity: String,val operation: String,val data: JsonObject)
@Serializable data class SyncRequest(val lastSyncAt: String?,val changes: List<SyncChangeDto>)
@Serializable data class SyncResponse(val serverChanges: List<SyncChangeDto>,val serverTime: String)
