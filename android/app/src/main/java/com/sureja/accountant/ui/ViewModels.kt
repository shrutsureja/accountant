@file:OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.sureja.accountant.ui

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.sureja.accountant.data.*
import com.sureja.accountant.data.local.*
import com.sureja.accountant.data.preferences.AuthStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel class SessionViewModel @Inject constructor(private val repository: AccountantRepository,private val store: AuthStore): ViewModel() {
    val hasSession=store.hasSession.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),false)
    val displayName=store.displayName.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),"")
    private val _loginState=MutableStateFlow<LoginState>(LoginState.Idle); val loginState=_loginState.asStateFlow()
    init { viewModelScope.launch { repository.seedForOfflinePreview() } }
    fun login(username:String,pin:String) { if(username.isBlank()||pin.length<4)return; viewModelScope.launch { _loginState.value=LoginState.Loading; _loginState.value=repository.login(username,pin,"${Build.MANUFACTURER} ${Build.MODEL}").fold({LoginState.Success},{LoginState.Error(it.message?:"Could not sign in")}) } }
    fun logout()=viewModelScope.launch { repository.logout() }
}
sealed interface LoginState { data object Idle:LoginState; data object Loading:LoginState; data object Success:LoginState; data class Error(val message:String):LoginState }

data class HomeState(val total:Long=0,val previousTotal:Long=0,val reviewCount:Int=0,val people:List<NamedAmount> = emptyList(),val categories:List<NamedAmount> = emptyList())
@HiltViewModel class HomeViewModel @Inject constructor(repository: AccountantRepository): ViewModel() {
    val selectedMonth = MutableStateFlow(YearMonth.now())
    private val previousMonth = MonthRange.forMonth(YearMonth.now().minusMonths(1))
    val state=selectedMonth.flatMapLatest { month ->
        val range=MonthRange.forMonth(month)
        combine(repository.total(range),repository.total(previousMonth),repository.reviewCount(),repository.memberTotals(range),repository.categoryTotals(range)) { total,previous,review,people,categories -> HomeState(total,previous,review,people,categories.take(5)) }
    }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),HomeState())
    fun previousMonth(){selectedMonth.value=selectedMonth.value.minusMonths(1)}
    fun nextMonth(){if(selectedMonth.value<YearMonth.now())selectedMonth.value=selectedMonth.value.plusMonths(1)}
    fun showPreviousMonth(){selectedMonth.value=YearMonth.now().minusMonths(1)}
}

data class AddState(val members:List<MemberEntity> = emptyList(),val categories:List<CategoryEntity> = emptyList(),val accounts:List<AccountEntity> = emptyList(),val saving:Boolean=false,val saved:Boolean=false,val error:String?=null)
@HiltViewModel class AddViewModel @Inject constructor(private val repository: AccountantRepository): ViewModel() {
    private val progress=MutableStateFlow(AddState())
    val state=combine(repository.members(),repository.categories(),repository.accounts(),progress) { m,c,a,p -> p.copy(members=m,categories=c,accounts=a) }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),AddState())
    fun save(amount:String,categoryId:String?,memberId:String?,payment:PaymentMethod,accountId:String?,merchant:String,note:String,onSaved:()->Unit) {
        val paise=amount.toBigDecimalOrNull()?.movePointRight(2)?.toLong(); if(paise==null||paise<=0||memberId==null){progress.value=progress.value.copy(error="Enter an amount and choose who paid");return}
        viewModelScope.launch { progress.value=progress.value.copy(saving=true,error=null); runCatching { repository.addExpense(ExpenseInput(paise,categoryId,memberId,payment,accountId,merchant,note,OffsetDateTime.now().toString())) }.onSuccess { progress.value=progress.value.copy(saving=false,saved=true);onSaved() }.onFailure { progress.value=progress.value.copy(saving=false,error="Expense was not saved") } }
    }
}

@HiltViewModel class TransactionsViewModel @Inject constructor(private val repository: AccountantRepository):ViewModel() {
    val search=MutableStateFlow(""); val member=MutableStateFlow<String?>(null)
    val items=combine(search.debounce(200),member){q,m->q to m}.flatMapLatest{repository.transactions(it.first,it.second)}.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    fun delete(id:String)=viewModelScope.launch{repository.deleteExpense(id)}
    fun update(item:TransactionListItem,amount:String,categoryId:String?,memberId:String,payment:PaymentMethod,accountId:String?,merchant:String,note:String){
        val paise=amount.toBigDecimalOrNull()?.movePointRight(2)?.toLong()?:return
        viewModelScope.launch{repository.updateExpense(item.id,ExpenseInput(paise,categoryId,memberId,payment,if(payment==PaymentMethod.CASH)null else accountId,merchant,note,item.occurredAt))}
    }
}

@HiltViewModel class ReviewViewModel @Inject constructor(private val repository: AccountantRepository):ViewModel() {
    val items=repository.reviewQueue().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    fun confirm(id:String)=viewModelScope.launch{repository.confirm(id)}; fun ignore(id:String)=viewModelScope.launch{repository.ignore(id)}
}

data class ReportsState(val total:Long=0,val categories:List<NamedAmount> = emptyList(),val people:List<NamedAmount> = emptyList(),val daily:List<DayAmount> = emptyList())
@HiltViewModel class ReportsViewModel @Inject constructor(repository: AccountantRepository):ViewModel() {
    val selectedMonth=MutableStateFlow(YearMonth.now())
    val state=selectedMonth.flatMapLatest { month ->
        val range=MonthRange.forMonth(month)
        combine(repository.total(range),repository.categoryTotals(range),repository.memberTotals(range),repository.dailyTotals(range)){total,c,p,d->ReportsState(total,c,p,d)}
    }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),ReportsState())
    fun previousMonth(){selectedMonth.value=selectedMonth.value.minusMonths(1)}
    fun nextMonth(){if(selectedMonth.value<YearMonth.now())selectedMonth.value=selectedMonth.value.plusMonths(1)}
}

@HiltViewModel class CatalogViewModel @Inject constructor(private val repository: AccountantRepository):ViewModel() {
    val categories=repository.allCategories().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val accounts=repository.allAccounts().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val members=repository.members().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    fun addCategory(name:String)=viewModelScope.launch{if(name.isNotBlank())repository.addCategory(name)}
    fun toggleCategory(item:CategoryEntity)=viewModelScope.launch{repository.setCategoryActive(item.id,!item.active)}
    fun addAccount(name:String,bank:String,last4:String,owner:String?,payment:PaymentMethod)=viewModelScope.launch{if(name.isNotBlank()&&(last4.isBlank()||last4.length==4))repository.addAccount(name,bank,last4,owner,payment)}
    fun toggleAccount(item:AccountEntity)=viewModelScope.launch{repository.setAccountActive(item.id,!item.active)}
}
