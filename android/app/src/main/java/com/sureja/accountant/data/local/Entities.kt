package com.sureja.accountant.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PaymentMethod { CASH, UPI, OTHER }
enum class TransactionSource { MANUAL, SMS, NOTIFICATION }
enum class TransactionStatus { DETECTED, CONFIRMED, IGNORED, DELETED }
enum class SyncStatus { SYNCED, PENDING_CREATE, PENDING_UPDATE, PENDING_DELETE, SYNC_ERROR }

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val avatarInitials: String,
    val createdAt: String,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String = "category",
    val active: Boolean = true,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val version: Int = 1,
    val updatedBy: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val bankName: String? = null,
    val last4: String? = null,
    val ownerUserId: String? = null,
    val paymentMethod: PaymentMethod,
    val active: Boolean = true,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val version: Int = 1,
    val updatedBy: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
)

@Entity(
    tableName = "transactions",
    indices = [Index("occurredAt"), Index("status"), Index("fingerprint"), Index("syncStatus")]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amountPaise: Long,
    val currency: String = "INR",
    val categoryId: String? = null,
    val paidByUserId: String,
    val paymentMethod: PaymentMethod,
    val accountId: String? = null,
    val merchant: String? = null,
    val note: String? = null,
    val occurredAt: String,
    val source: TransactionSource,
    val status: TransactionStatus,
    val sourceReference: String? = null,
    val fingerprint: String? = null,
    val createdByUserId: String,
    val updatedByUserId: String,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val version: Int = 1,
    val syncStatus: SyncStatus,
)

@Entity(tableName = "merchant_rules", indices = [Index(value = ["merchantPattern"], unique = true)])
data class MerchantRuleEntity(
    @PrimaryKey val id: String,
    val merchantPattern: String,
    val categoryId: String,
    val createdByUserId: String,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val version: Int = 1,
    val updatedBy: String? = null,
    val syncStatus: SyncStatus,
)

data class TransactionListItem(
    val id: String,
    val amountPaise: Long,
    val categoryId: String?,
    val categoryName: String?,
    val paidByUserId: String,
    val memberName: String,
    val paymentMethod: PaymentMethod,
    val accountId: String?,
    val accountName: String?,
    val merchant: String?,
    val note: String?,
    val occurredAt: String,
    val source: TransactionSource,
    val status: TransactionStatus,
    val updatedByUserId: String,
    val syncStatus: SyncStatus,
    val createdByUserId: String,
)

data class NamedAmount(val id: String, val name: String, val amountPaise: Long)
data class DayAmount(val day: String, val amountPaise: Long)
data class CategoryUsage(val categoryId: String, val useCount: Int)
