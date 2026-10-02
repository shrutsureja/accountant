package com.sureja.accountant.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountantDao {
    @Query("SELECT * FROM members ORDER BY displayName") fun observeMembers(): Flow<List<MemberEntity>>
    @Query("SELECT * FROM members ORDER BY displayName") suspend fun members(): List<MemberEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertMembers(items: List<MemberEntity>)

    @Query("SELECT * FROM categories WHERE deletedAt IS NULL AND active=1 ORDER BY name") fun observeCategories(): Flow<List<CategoryEntity>>
    @Query("SELECT * FROM categories WHERE deletedAt IS NULL ORDER BY active DESC,name") fun observeAllCategories(): Flow<List<CategoryEntity>>
    @Query("SELECT * FROM categories WHERE deletedAt IS NULL ORDER BY name") suspend fun categories(): List<CategoryEntity>
    @Query("SELECT * FROM categories WHERE syncStatus!='SYNCED'") suspend fun pendingCategories(): List<CategoryEntity>
    @Query("""SELECT t.categoryId, COUNT(*) AS useCount FROM transactions t
        JOIN categories c ON c.id=t.categoryId AND c.active=1 AND c.deletedAt IS NULL
        WHERE t.categoryId IS NOT NULL AND t.status='CONFIRMED' AND t.deletedAt IS NULL
        AND lower(trim(c.name)) NOT IN ('not categorized','uncategorized')
        AND (:userId IS NULL OR t.paidByUserId=:userId)
        AND date(t.occurredAt) >= date(:sinceDate)
        GROUP BY t.categoryId""") fun observeRecentCategoryUsage(sinceDate: String, userId: String? = null): Flow<List<CategoryUsage>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertCategories(items: List<CategoryEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertCategory(item: CategoryEntity)

    @Query("SELECT * FROM accounts WHERE deletedAt IS NULL AND active=1 ORDER BY name") fun observeAccounts(): Flow<List<AccountEntity>>
    @Query("SELECT * FROM accounts WHERE deletedAt IS NULL ORDER BY active DESC,name") fun observeAllAccounts(): Flow<List<AccountEntity>>
    @Query("SELECT * FROM accounts WHERE deletedAt IS NULL ORDER BY name") suspend fun accounts(): List<AccountEntity>
    @Query("SELECT * FROM accounts WHERE syncStatus!='SYNCED'") suspend fun pendingAccounts(): List<AccountEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAccounts(items: List<AccountEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAccount(item: AccountEntity)

    @Query("""
        SELECT t.id,t.amountPaise,t.categoryId,c.name categoryName,t.paidByUserId,m.displayName memberName,
        t.paymentMethod,t.accountId,a.name accountName,t.merchant,t.note,t.occurredAt,t.source,t.status,t.updatedByUserId,t.syncStatus,t.createdByUserId
        FROM transactions t LEFT JOIN categories c ON c.id=t.categoryId LEFT JOIN members m ON m.id=t.paidByUserId LEFT JOIN accounts a ON a.id=t.accountId
        WHERE t.deletedAt IS NULL AND t.status='CONFIRMED' AND
        (:search='' OR t.merchant LIKE '%' || :search || '%' OR t.note LIKE '%' || :search || '%' OR c.name LIKE '%' || :search || '%' OR m.displayName LIKE '%' || :search || '%') AND
        (:memberId IS NULL OR t.paidByUserId=:memberId) AND
        (:categoryId IS NULL OR (:categoryId='__uncategorized_filter__' AND t.categoryId IS NULL) OR t.categoryId=:categoryId) AND
        (:fromDate IS NULL OR t.occurredAt>=:fromDate) AND
        (:toDate IS NULL OR t.occurredAt<:toDate)
        ORDER BY t.occurredAt DESC
    """) fun observeTransactions(search: String = "", memberId: String? = null, categoryId: String? = null, fromDate: String? = null, toDate: String? = null): Flow<List<TransactionListItem>>

    @Query("""
        SELECT t.id,t.amountPaise,t.categoryId,c.name categoryName,t.paidByUserId,m.displayName memberName,
        t.paymentMethod,t.accountId,a.name accountName,t.merchant,t.note,t.occurredAt,t.source,t.status,t.updatedByUserId,t.syncStatus,t.createdByUserId
        FROM transactions t LEFT JOIN categories c ON c.id=t.categoryId LEFT JOIN members m ON m.id=t.paidByUserId LEFT JOIN accounts a ON a.id=t.accountId
        WHERE t.deletedAt IS NULL AND t.status='DETECTED' ORDER BY t.occurredAt DESC
    """) fun observeReviewQueue(): Flow<List<TransactionListItem>>

    @Query("SELECT * FROM transactions WHERE id=:id") suspend fun transaction(id: String): TransactionEntity?
    @Query("SELECT COUNT(*) FROM transactions WHERE fingerprint=:fingerprint AND status!='DELETED' AND deletedAt IS NULL") suspend fun fingerprintCount(fingerprint: String): Int
    @Query("SELECT * FROM transactions WHERE syncStatus!='SYNCED'") suspend fun pendingTransactions(): List<TransactionEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertTransaction(item: TransactionEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertTransactions(items: List<TransactionEntity>)
    @Query("UPDATE transactions SET syncStatus='SYNCED' WHERE id IN (:ids)") suspend fun markTransactionsSynced(ids: List<String>)

    @Query("SELECT COALESCE(SUM(amountPaise),0) FROM transactions WHERE status='CONFIRMED' AND deletedAt IS NULL AND occurredAt>=:from AND occurredAt<:to") fun observeTotal(from: String, to: String): Flow<Long>
    @Query("SELECT COUNT(*) FROM transactions WHERE status='DETECTED' AND deletedAt IS NULL") fun observeReviewCount(): Flow<Int>
    @Query("""SELECT m.id,m.displayName name,COALESCE(SUM(t.amountPaise),0) amountPaise FROM members m LEFT JOIN transactions t ON t.paidByUserId=m.id AND t.status='CONFIRMED' AND t.deletedAt IS NULL AND t.occurredAt>=:from AND t.occurredAt<:to GROUP BY m.id,m.displayName ORDER BY amountPaise DESC, m.displayName COLLATE NOCASE""") fun observeMemberTotals(from: String, to: String): Flow<List<NamedAmount>>
    @Query("""SELECT COALESCE(c.id,'uncategorized') id,COALESCE(c.name,'Not categorized') name,SUM(t.amountPaise) amountPaise FROM transactions t LEFT JOIN categories c ON c.id=t.categoryId WHERE t.status='CONFIRMED' AND t.deletedAt IS NULL AND t.occurredAt>=:from AND t.occurredAt<:to GROUP BY COALESCE(c.id,'uncategorized'),COALESCE(c.name,'Not categorized') ORDER BY amountPaise DESC""") fun observeCategoryTotals(from: String, to: String): Flow<List<NamedAmount>>
    @Query("""SELECT substr(occurredAt,1,10) day,SUM(amountPaise) amountPaise FROM transactions WHERE status='CONFIRMED' AND deletedAt IS NULL AND occurredAt>=:from AND occurredAt<:to GROUP BY day ORDER BY day""") fun observeDailyTotals(from: String, to: String): Flow<List<DayAmount>>

    @Query("SELECT * FROM merchant_rules WHERE deletedAt IS NULL") suspend fun merchantRules(): List<MerchantRuleEntity>
    @Query("SELECT * FROM merchant_rules WHERE syncStatus!='SYNCED'") suspend fun pendingMerchantRules(): List<MerchantRuleEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertMerchantRules(items: List<MerchantRuleEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertMerchantRule(item: MerchantRuleEntity)
    @Query("UPDATE categories SET syncStatus='SYNCED' WHERE id IN (:ids)") suspend fun markCategoriesSynced(ids: List<String>)
    @Query("UPDATE accounts SET syncStatus='SYNCED' WHERE id IN (:ids)") suspend fun markAccountsSynced(ids: List<String>)
    @Query("UPDATE merchant_rules SET syncStatus='SYNCED' WHERE id IN (:ids)") suspend fun markMerchantRulesSynced(ids: List<String>)

    @Transaction suspend fun seed(members: List<MemberEntity>, categories: List<CategoryEntity>, cash: AccountEntity) {
        upsertMembers(members); upsertCategories(categories); upsertAccount(cash)
    }
}
