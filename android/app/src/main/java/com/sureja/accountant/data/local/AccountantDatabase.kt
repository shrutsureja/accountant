package com.sureja.accountant.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [MemberEntity::class, CategoryEntity::class, AccountEntity::class, TransactionEntity::class, MerchantRuleEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AccountantDatabase : RoomDatabase() {
    abstract fun dao(): AccountantDao
}

