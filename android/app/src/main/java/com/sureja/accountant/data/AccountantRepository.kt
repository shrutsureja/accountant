package com.sureja.accountant.data

import com.sureja.accountant.data.local.*
import com.sureja.accountant.data.network.*
import com.sureja.accountant.data.preferences.AuthStore
import com.sureja.accountant.data.preferences.Session
import com.sureja.accountant.domain.ParsedTransaction
import com.sureja.accountant.domain.TransactionFingerprint
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.time.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class MonthRange(val from: String,val to: String) {
    companion object {
        fun forMonth(month: YearMonth): MonthRange {
            val start = month.atDay(1).atStartOfDay(ZoneId.systemDefault())
            return MonthRange(start.toOffsetDateTime().toString(),start.plusMonths(1).toOffsetDateTime().toString())
        }
        fun current(): MonthRange = forMonth(YearMonth.now())
    }
}

data class ExpenseInput(val amountPaise: Long,val categoryId: String?,val paidByUserId: String,val paymentMethod: PaymentMethod,val accountId: String?,val merchant: String?,val note: String?,val occurredAt: String)

data class SyncState(val running: Boolean = false, val error: String? = null)

@Singleton
class AccountantRepository @Inject constructor(private val dao: AccountantDao,private val api: AccountantApi,private val authStore: AuthStore,private val json: Json, private val database: AccountantDatabase) {
    private val detectionMutex = Mutex()
    private val syncMutex = Mutex()
    private val _syncState = MutableStateFlow(SyncState())
    val syncState = _syncState.asStateFlow()

    fun members() = dao.observeMembers(); fun categories() = dao.observeCategories(); fun allCategories() = dao.observeAllCategories(); fun accounts() = dao.observeAccounts();fun allAccounts()=dao.observeAllAccounts()
    fun recentCategoryUsage(sinceDate: LocalDate, userId: String? = null) = dao.observeRecentCategoryUsage(sinceDate.toString(), userId)
    fun transactions(search: String="",member: String?=null,category: String?=null,range: MonthRange?=null) = dao.observeTransactions(search,member,category,range?.from,range?.to)
    fun reviewQueue() = dao.observeReviewQueue()
    fun total(range: MonthRange=MonthRange.current()) = dao.observeTotal(range.from,range.to)
    fun reviewCount() = dao.observeReviewCount()
    fun memberTotals(range: MonthRange=MonthRange.current()) = dao.observeMemberTotals(range.from,range.to)
    fun categoryTotals(range: MonthRange=MonthRange.current()) = dao.observeCategoryTotals(range.from,range.to)
    fun dailyTotals(range: MonthRange=MonthRange.current()) = dao.observeDailyTotals(range.from,range.to)
    suspend fun exportTransactions(): List<TransactionListItem> = transactions().first()

    suspend fun login(username: String,pin: String,deviceName: String): Result<Unit> = runCatching {
        val deviceId = "android-${UUID.randomUUID()}"
        val response = api.login(LoginRequest(username,pin,deviceId,deviceName))
        authStore.saveSession(Session(response.user.id,response.user.displayName,response.user.username,deviceId,response.accessToken,response.refreshToken))
        refreshCatalog()
    }

    suspend fun seedForOfflinePreview() {
        if (dao.members().isNotEmpty()) return
        val now=OffsetDateTime.now().toString()
        dao.seed(
            listOf(MemberEntity("user-shrut","shrut","Shrut","S",now),MemberEntity("user-mom","alpa","Alpa","A",now),MemberEntity("user-dad","hitesh","Hitesh","H",now)),
            defaultCategories.map { CategoryEntity("cat-${it.first}",it.second,createdAt=now,updatedAt=now) },
            AccountEntity("account-cash","Cash",paymentMethod=PaymentMethod.CASH,createdAt=now,updatedAt=now)
        )
    }

    suspend fun refreshCatalog() {
        val (members,categories,accounts)=listOf(api.members(),api.categories(),api.accounts())
        val memberEntities = (members as MembersResponse).members.map { MemberEntity(it.id,it.username,it.displayName,it.avatarInitials,it.createdAt) }
        dao.upsertMembers(memberEntities)
        val currentUserId = authStore.userId.first()
        memberEntities.firstOrNull { it.id == currentUserId }?.let { authStore.updateDisplayName(it.displayName) }
        database.withTransaction {
            val dirtyCategories = dao.pendingCategories().map { it.id }.toSet()
            val dirtyAccounts = dao.pendingAccounts().map { it.id }.toSet()
            dao.upsertCategories((categories as CategoriesResponse).categories.filter { it.id !in dirtyCategories }.map { it.toEntity() })
            dao.upsertAccounts((accounts as AccountsResponse).accounts.filter { it.id !in dirtyAccounts }.map { it.toEntity() })
        }
    }

    suspend fun addExpense(input: ExpenseInput): String {
        val session=authStore.session(); val userId=session?.userId ?: input.paidByUserId; val now=OffsetDateTime.now().toString(); val id=UUID.randomUUID().toString()
        dao.upsertTransaction(TransactionEntity(id,input.amountPaise,categoryId=input.categoryId,paidByUserId=input.paidByUserId,paymentMethod=input.paymentMethod,accountId=input.accountId,merchant=input.merchant?.takeIf(String::isNotBlank),note=input.note?.takeIf(String::isNotBlank),occurredAt=input.occurredAt,source=TransactionSource.MANUAL,status=TransactionStatus.CONFIRMED,createdByUserId=userId,updatedByUserId=userId,createdAt=now,updatedAt=now,syncStatus=SyncStatus.PENDING_CREATE))
        authStore.setLastPayment(input.paymentMethod.name); return id
    }

    suspend fun updateExpense(id: String,input: ExpenseInput) {
        val old=dao.transaction(id) ?: return; val userId=authStore.session()?.userId ?: old.updatedByUserId
        dao.upsertTransaction(old.copy(amountPaise=input.amountPaise,categoryId=input.categoryId,paidByUserId=input.paidByUserId,paymentMethod=input.paymentMethod,accountId=input.accountId,merchant=input.merchant,note=input.note,occurredAt=input.occurredAt,updatedByUserId=userId,updatedAt=OffsetDateTime.now().toString(),version=old.version+1,syncStatus=if(old.syncStatus==SyncStatus.PENDING_CREATE) old.syncStatus else SyncStatus.PENDING_UPDATE))
    }
    suspend fun deleteExpense(id: String) { val old=dao.transaction(id) ?: return; val now=OffsetDateTime.now().toString(); dao.upsertTransaction(old.copy(status=TransactionStatus.DELETED,deletedAt=now,updatedAt=now,syncStatus=SyncStatus.PENDING_DELETE)) }
    suspend fun confirm(id: String) = setReviewStatus(id,TransactionStatus.CONFIRMED)
    suspend fun ignore(id: String) = setReviewStatus(id,TransactionStatus.IGNORED)
    private suspend fun setReviewStatus(id: String,status: TransactionStatus) { val old=dao.transaction(id) ?: return; dao.upsertTransaction(old.copy(status=status,updatedAt=OffsetDateTime.now().toString(),syncStatus=if(old.syncStatus==SyncStatus.PENDING_CREATE) old.syncStatus else SyncStatus.PENDING_UPDATE)) }

    suspend fun addDetected(parsed: ParsedTransaction,source: TransactionSource,occurredAt: Instant=Instant.now()): Boolean = detectionMutex.withLock {
        seedForOfflinePreview(); val member=authStore.session()?.userId ?: dao.members().first().id
        val fingerprint=TransactionFingerprint.create(parsed.amountPaise,occurredAt,parsed.accountLast4,parsed.merchant,parsed.sourceReference)
        if (dao.fingerprintCount(fingerprint)>0) return@withLock false
        val rules=dao.merchantRules(); val category=rules.firstOrNull { parsed.merchant?.contains(it.merchantPattern,true)==true }?.categoryId
        val now=OffsetDateTime.now().toString()
        dao.upsertTransaction(TransactionEntity(UUID.randomUUID().toString(),parsed.amountPaise,categoryId=category,paidByUserId=member,paymentMethod=PaymentMethod.valueOf(parsed.paymentMethod),merchant=parsed.merchant,occurredAt=occurredAt.atZone(ZoneId.systemDefault()).toOffsetDateTime().toString(),source=source,status=TransactionStatus.DETECTED,sourceReference=parsed.sourceReference,fingerprint=fingerprint,createdByUserId=member,updatedByUserId=member,createdAt=now,updatedAt=now,syncStatus=SyncStatus.PENDING_CREATE))
        true
    }

    suspend fun logout() { runCatching { api.logout() }; authStore.clear() }

    suspend fun addCategory(name:String) {
        val now=OffsetDateTime.now().toString();val user=authStore.session()?.userId
        dao.upsertCategory(CategoryEntity(UUID.randomUUID().toString(),name.trim(),createdAt=now,updatedAt=now,updatedBy=user,syncStatus=SyncStatus.PENDING_CREATE))
    }
    suspend fun renameCategory(id:String,name:String) {
        val old=dao.categories().firstOrNull { it.id==id } ?: return
        dao.upsertCategory(old.copy(name=name.trim(),updatedAt=OffsetDateTime.now().toString(),updatedBy=authStore.session()?.userId,version=old.version+1,syncStatus=if(old.syncStatus==SyncStatus.PENDING_CREATE)old.syncStatus else SyncStatus.PENDING_UPDATE))
    }
    suspend fun setCategoryActive(id:String,active:Boolean) {
        val old=dao.categories().firstOrNull{it.id==id}?:return
        dao.upsertCategory(old.copy(active=active,updatedAt=OffsetDateTime.now().toString(),updatedBy=authStore.session()?.userId,version=old.version+1,syncStatus=if(old.syncStatus==SyncStatus.PENDING_CREATE)old.syncStatus else SyncStatus.PENDING_UPDATE))
    }
    suspend fun addAccount(name:String,bank:String,last4:String,owner:String?,payment:PaymentMethod) {
        val now=OffsetDateTime.now().toString();val user=authStore.session()?.userId
        dao.upsertAccount(AccountEntity(UUID.randomUUID().toString(),name.trim(),bank.trim().ifBlank{null},last4.trim().ifBlank{null},owner,payment,true,now,now,updatedBy=user,syncStatus=SyncStatus.PENDING_CREATE))
    }
    suspend fun setAccountActive(id:String,active:Boolean) {
        val old=dao.accounts().firstOrNull{it.id==id}?:return
        dao.upsertAccount(old.copy(active=active,updatedAt=OffsetDateTime.now().toString(),updatedBy=authStore.session()?.userId,version=old.version+1,syncStatus=if(old.syncStatus==SyncStatus.PENDING_CREATE)old.syncStatus else SyncStatus.PENDING_UPDATE))
    }

    suspend fun sync(): Result<Unit> = syncMutex.withLock {
        if (authStore.session() == null) return@withLock Result.success(Unit)
        _syncState.value = SyncState(running = true)
        try {
        val pending=dao.pendingTransactions();val pendingCategories=dao.pendingCategories();val pendingAccounts=dao.pendingAccounts();val pendingRules=dao.pendingMerchantRules()
        val changes=pending.map { entity -> SyncChangeDto("transaction",if(entity.syncStatus==SyncStatus.PENDING_DELETE) "DELETE" else "UPSERT",json.encodeToJsonElement(entity.toDto()).jsonObject) }+
            pendingCategories.map{SyncChangeDto("category",if(it.syncStatus==SyncStatus.PENDING_DELETE)"DELETE" else "UPSERT",json.encodeToJsonElement(it.toDto()).jsonObject)}+
            pendingAccounts.map{SyncChangeDto("account",if(it.syncStatus==SyncStatus.PENDING_DELETE)"DELETE" else "UPSERT",json.encodeToJsonElement(it.toDto()).jsonObject)}+
            pendingRules.map{SyncChangeDto("merchant_rule",if(it.syncStatus==SyncStatus.PENDING_DELETE)"DELETE" else "UPSERT",json.encodeToJsonElement(it.toDto()).jsonObject)}
        val result=api.sync(SyncRequest(authStore.lastSync(),changes))
        database.withTransaction {
            val dirtyTransactions = dao.pendingTransactions().associateBy { it.id }
            val dirtyCategories = dao.pendingCategories().associateBy { it.id }
            val dirtyAccounts = dao.pendingAccounts().associateBy { it.id }
            val dirtyRules = dao.pendingMerchantRules().associateBy { it.id }
            val sentTransactions = pending.associateBy { it.id }
            val sentCategories = pendingCategories.associateBy { it.id }
            val sentAccounts = pendingAccounts.associateBy { it.id }
            val sentRules = pendingRules.associateBy { it.id }
            result.serverChanges.forEach { change -> when(change.entity) {
                "transaction" -> {
                    val entity = json.decodeFromJsonElement<TransactionDto>(change.data).toEntity(SyncStatus.SYNCED)
                    if (canApplySyncResponse(dirtyTransactions[entity.id], sentTransactions[entity.id])) dao.upsertTransaction(entity)
                }
                "category" -> {
                    val entity = json.decodeFromJsonElement<CategoryDto>(change.data).toEntity()
                    if (canApplySyncResponse(dirtyCategories[entity.id], sentCategories[entity.id])) dao.upsertCategory(entity)
                }
                "account" -> {
                    val entity = json.decodeFromJsonElement<AccountDto>(change.data).toEntity()
                    if (canApplySyncResponse(dirtyAccounts[entity.id], sentAccounts[entity.id])) dao.upsertAccount(entity)
                }
                "merchant_rule" -> {
                    val entity = json.decodeFromJsonElement<MerchantRuleDto>(change.data).toEntity()
                    if (canApplySyncResponse(dirtyRules[entity.id], sentRules[entity.id])) dao.upsertMerchantRule(entity)
                }
            } }
            // Only acknowledge the exact snapshot uploaded, preserving edits made during the request.
            dao.markTransactionsSynced(pending.filter { dirtyTransactions[it.id] == it }.map { it.id })
            dao.markCategoriesSynced(pendingCategories.filter { dirtyCategories[it.id] == it }.map { it.id })
            dao.markAccountsSynced(pendingAccounts.filter { dirtyAccounts[it.id] == it }.map { it.id })
            dao.markMerchantRulesSynced(pendingRules.filter { dirtyRules[it.id] == it }.map { it.id })
        }
        authStore.setLastSync(result.serverTime)
        refreshCatalog()
        _syncState.value = SyncState()
        Result.success(Unit)
        } catch (cancelled: CancellationException) {
            _syncState.value = SyncState()
            throw cancelled
        } catch (error: Exception) {
            _syncState.value = SyncState(error = "Couldn't sync. Your changes are saved on this phone.")
            Result.failure(error)
        }
    }

    private val defaultCategories=listOf("groceries" to "Groceries","food" to "Food & Dining","household" to "Household","shopping" to "Shopping","fuel" to "Fuel","transport" to "Transport","medical" to "Medical","utilities" to "Utilities","entertainment" to "Entertainment","education" to "Education","travel" to "Travel","care" to "Personal Care","gifts" to "Gifts","investment" to "Investment","other" to "Other")
}

private fun CategoryDto.toEntity()=CategoryEntity(id,name,icon,active,createdAt?:updatedAt?:OffsetDateTime.now().toString(),updatedAt?:OffsetDateTime.now().toString(),deletedAt,version,updatedBy)
private fun AccountDto.toEntity()=AccountEntity(id,name,bankName,last4,ownerUserId,PaymentMethod.valueOf(paymentMethod),active,createdAt?:updatedAt?:OffsetDateTime.now().toString(),updatedAt?:OffsetDateTime.now().toString(),deletedAt,version,updatedBy)
private fun TransactionEntity.toDto()=TransactionDto(id,amountPaise,currency,categoryId,paidByUserId,paymentMethod.name,accountId,merchant,note,occurredAt,source.name,status.name,sourceReference,fingerprint,createdByUserId,updatedByUserId,createdAt,updatedAt,deletedAt,version)
private fun TransactionDto.toEntity(sync: SyncStatus)=TransactionEntity(id,amountPaise,currency,categoryId,paidByUserId,PaymentMethod.valueOf(paymentMethod),accountId,merchant,note,occurredAt,TransactionSource.valueOf(source),TransactionStatus.valueOf(status),sourceReference,fingerprint,createdByUserId?:paidByUserId,updatedByUserId?:paidByUserId,createdAt?:updatedAt?:occurredAt,updatedAt?:occurredAt,deletedAt,version,sync)
private fun CategoryEntity.toDto()=CategoryDto(id,name,icon,active,createdAt,updatedAt,deletedAt,version,updatedBy)
private fun AccountEntity.toDto()=AccountDto(id,name,bankName,last4,ownerUserId,paymentMethod.name,active,createdAt,updatedAt,deletedAt,version,updatedBy)
private fun MerchantRuleEntity.toDto()=MerchantRuleDto(id,merchantPattern,categoryId,createdByUserId,createdAt,updatedAt,deletedAt,version,updatedBy)
private fun MerchantRuleDto.toEntity()=MerchantRuleEntity(id,merchantPattern,categoryId,createdByUserId?:updatedBy.orEmpty(),createdAt?:updatedAt?:OffsetDateTime.now().toString(),updatedAt?:OffsetDateTime.now().toString(),deletedAt,version,updatedBy,SyncStatus.SYNCED)

internal fun <T> canApplySyncResponse(currentDirty: T?, uploaded: T?): Boolean = currentDirty == null || currentDirty == uploaded
