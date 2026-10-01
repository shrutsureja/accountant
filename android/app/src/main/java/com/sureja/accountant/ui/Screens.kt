@file:OptIn(ExperimentalMaterial3Api::class)

package com.sureja.accountant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sureja.accountant.data.local.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable fun LoginScreen(viewModel:SessionViewModel=hiltViewModel()) {
    var username by remember{mutableStateOf("")};var pin by remember{mutableStateOf("")};val state by viewModel.loginState.collectAsState()
    Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center){Column(Modifier.widthIn(max=420.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){Icon(Icons.Default.AccountBalanceWallet,null,Modifier.size(44.dp),tint=MaterialTheme.colorScheme.primary);Text("Accountant",style=MaterialTheme.typography.headlineLarge);Text("Household expenses, kept simple.",color=MaterialTheme.colorScheme.onSurfaceVariant);OutlinedTextField(username,{username=it},Modifier.fillMaxWidth(),label={Text("Username")},singleLine=true);OutlinedTextField(pin,{pin=it.filter(Char::isDigit).take(8)},Modifier.fillMaxWidth(),label={Text("PIN")},singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword));if(state is LoginState.Error)ErrorBanner((state as LoginState.Error).message);Button({viewModel.login(username,pin)},Modifier.fillMaxWidth().height(52.dp),enabled=state !is LoginState.Loading){if(state is LoginState.Loading)CircularProgressIndicator(Modifier.size(22.dp),strokeWidth=2.dp)else Text("Sign in")};Text("Your expenses remain available offline after the first sign-in.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
}

@Composable fun LockedScreen(onUnlock:()->Unit,onUsePin:()->Unit) { Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(14.dp)){Icon(Icons.Default.Fingerprint,null,Modifier.size(64.dp),tint=MaterialTheme.colorScheme.primary);Text("Accountant",style=MaterialTheme.typography.headlineMedium);Button(onUnlock){Text("Unlock with fingerprint")};TextButton(onUsePin){Text("Use account PIN")}}} }

@Composable private fun SummaryRow(name:String,amount:Long,total:Long){Column(Modifier.fillMaxWidth().padding(vertical=5.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(name);Text(money(amount),fontWeight=androidx.compose.ui.text.font.FontWeight.Medium)};LinearProgressIndicator(if(total==0L)0f else amount.toFloat()/total,Modifier.fillMaxWidth().padding(top=7.dp).height(4.dp))}}


@Composable internal fun <T>SelectField(label:String,items:List<T>,selected:String?,onSelected:(String)->Unit,name:(T)->String,id:(T)->String,optional:Boolean=false){var expanded by remember{mutableStateOf(false)};ExposedDropdownMenuBox(expanded,{expanded=it}){OutlinedTextField(items.firstOrNull{id(it)==selected}?.let(name)?:if(optional)"None" else "Select",{},Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),readOnly=true,label={Text(label)},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(expanded)});ExposedDropdownMenu(expanded,{expanded=false}){if(optional)DropdownMenuItem({Text("None")},{onSelected("");expanded=false});items.forEach{item->DropdownMenuItem({Text(name(item))},{onSelected(id(item));expanded=false})}}}}

@Composable fun ReviewScreen(onBack:()->Unit,viewModel:ReviewViewModel=hiltViewModel()){val items by viewModel.items.collectAsState();Scaffold(topBar={TopAppBar({Text("Needs review")},navigationIcon={IconButton(onBack){Icon(Icons.Default.ArrowBack,null)}})}){padding->if(items.isEmpty())EmptyState("All caught up","There are no detected transactions waiting for review.")else LazyColumn(Modifier.padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){items(items,key={it.id}){item->Card{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){AmountText(item.amountPaise);Text(item.merchant?:"Detected expense",style=MaterialTheme.typography.titleMedium);Text(listOfNotNull(item.paymentMethod.name,item.accountName,item.categoryName?.let{"Suggested: $it"}).joinToString(" • "),color=MaterialTheme.colorScheme.onSurfaceVariant);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){TextButton({viewModel.ignore(item.id)}){Text("Ignore")};Button({viewModel.confirm(item.id)}){Text("Confirm")}}}}}}}}

@Composable fun ReportsScreen(viewModel:ReportsViewModel=hiltViewModel()){val state by viewModel.state.collectAsState();val month by viewModel.selectedMonth.collectAsState();var tab by remember{mutableIntStateOf(0)};Column(Modifier.fillMaxSize()){Column(Modifier.padding(20.dp)){Text("Reports",style=MaterialTheme.typography.headlineSmall);Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){IconButton({viewModel.previousMonth()}){Icon(Icons.Default.ChevronLeft,"Previous month")};Box(Modifier.weight(1f),contentAlignment=Alignment.Center){Text("${month.month.getDisplayName(TextStyle.FULL,Locale.getDefault())} ${month.year}",color=MaterialTheme.colorScheme.onSurfaceVariant)};IconButton({viewModel.nextMonth()},enabled=month<YearMonth.now()){Icon(Icons.Default.ChevronRight,"Next month")}};AmountText(state.total,Modifier.padding(vertical=8.dp),32)};TabRow(tab){listOf("Overview","Categories","People").forEachIndexed{i,t->Tab(i==tab,{tab=i},text={Text(t)})}};LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){when(tab){0->{item{Text("Daily spending",style=MaterialTheme.typography.titleMedium)};if(state.daily.isEmpty())item{EmptyState("No report data","Add expenses to see daily spending.")}else items(state.daily){SummaryRow(it.day.takeLast(2),it.amountPaise,state.daily.maxOf{d->d.amountPaise})}};1->{items(state.categories){SummaryRow(it.name,it.amountPaise,state.total)}};else->{items(state.people){SummaryRow(it.name,it.amountPaise,state.total)}}}}}}

@Composable fun ProfileScreen(displayName:String,onLogout:()->Unit,onSync:()->Unit,onExport:()->Unit,onScanSms:()->Unit,onEnableLiveSms:()->Unit,onNotificationAccess:()->Unit,onCategories:()->Unit,onAccounts:()->Unit){Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(displayName.ifBlank{"Accountant"},style=MaterialTheme.typography.headlineSmall);Text("Household member",color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(20.dp));ProfileItem(Icons.Default.Sync,"Sync now",onSync);ProfileItem(Icons.Default.Sms,"Scan last 30 days",onScanSms);ProfileItem(Icons.Default.Sms,"Enable live SMS detection",onEnableLiveSms);ProfileItem(Icons.Default.Notifications,"Notification access",onNotificationAccess);ProfileItem(Icons.Default.Category,"Categories",onCategories);ProfileItem(Icons.Default.AccountBalance,"Accounts",onAccounts);ProfileItem(Icons.Default.Download,"Export CSV",onExport);HorizontalDivider(Modifier.padding(vertical=8.dp));ProfileItem(Icons.Default.Logout,"Log out",onLogout,MaterialTheme.colorScheme.error)}}
@Composable private fun ProfileItem(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,onClick:()->Unit,color:androidx.compose.ui.graphics.Color=MaterialTheme.colorScheme.onSurface){Surface(onClick=onClick,color=androidx.compose.ui.graphics.Color.Transparent){Row(Modifier.fillMaxWidth().height(54.dp),verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=color);Text(title,Modifier.weight(1f).padding(horizontal=16.dp),color=color);Icon(Icons.Default.ChevronRight,null,tint=MaterialTheme.colorScheme.onSurfaceVariant)}}}

@Composable fun CategoriesScreen(onBack:()->Unit,viewModel:CatalogViewModel=hiltViewModel()) {
    val categories by viewModel.categories.collectAsState();var showAdd by remember{mutableStateOf(false)};var name by remember{mutableStateOf("")}
    Scaffold(topBar={TopAppBar({Text("Categories")},navigationIcon={IconButton(onBack){Icon(Icons.Default.ArrowBack,null)}},actions={IconButton({showAdd=true}){Icon(Icons.Default.Add,null)}})}){padding->
        LazyColumn(Modifier.padding(padding),contentPadding=PaddingValues(horizontal=20.dp)){items(categories,key={it.id}){item->Row(Modifier.fillMaxWidth().padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(item.name);if(!item.active)Text("Archived",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};TextButton({viewModel.toggleCategory(item)}){Text(if(item.active)"Archive" else "Restore")}};HorizontalDivider()}}
    }
    if(showAdd)AlertDialog({showAdd=false},title={Text("Add category")},text={OutlinedTextField(name,{name=it},label={Text("Name")},singleLine=true)},confirmButton={Button({viewModel.addCategory(name);name="";showAdd=false}){Text("Add")}},dismissButton={TextButton({showAdd=false}){Text("Cancel")}})
}

@Composable fun AccountsScreen(onBack:()->Unit,viewModel:CatalogViewModel=hiltViewModel()) {
    val accounts by viewModel.accounts.collectAsState();val members by viewModel.members.collectAsState();var showAdd by remember{mutableStateOf(false)};var name by remember{mutableStateOf("")};var bank by remember{mutableStateOf("")};var last4 by remember{mutableStateOf("")};var owner by remember{mutableStateOf<String?>(null)};var payment by remember{mutableStateOf(PaymentMethod.UPI)}
    Scaffold(topBar={TopAppBar({Text("Accounts")},navigationIcon={IconButton(onBack){Icon(Icons.Default.ArrowBack,null)}},actions={IconButton({showAdd=true}){Icon(Icons.Default.Add,null)}})}){padding->
        LazyColumn(Modifier.padding(padding),contentPadding=PaddingValues(horizontal=20.dp)){items(accounts,key={it.id}){item->Row(Modifier.fillMaxWidth().padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(item.name);Text(listOfNotNull(item.bankName,item.last4?.let{"••$it"},item.paymentMethod.name,if(!item.active)"Archived" else null).joinToString(" • "),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};if(item.id!="account-cash")TextButton({viewModel.toggleAccount(item)}){Text(if(item.active)"Archive" else "Restore")}};HorizontalDivider()}}
    }
    if(showAdd)AlertDialog({showAdd=false},title={Text("Add account")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){OutlinedTextField(name,{name=it},label={Text("Display name")},singleLine=true);OutlinedTextField(bank,{bank=it},label={Text("Bank (optional)")},singleLine=true);OutlinedTextField(last4,{last4=it.filter(Char::isDigit).take(4)},label={Text("Last 4 digits")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number));SelectField("Owner",members,owner,{owner=it},{it.displayName},{it.id},true);SingleChoiceSegmentedButtonRow{listOf(PaymentMethod.UPI,PaymentMethod.OTHER).forEachIndexed{i,p->SegmentedButton(p==payment,{payment=p},SegmentedButtonDefaults.itemShape(i,2)){Text(p.name)}}}}},confirmButton={Button({viewModel.addAccount(name,bank,last4,owner,payment);name="";bank="";last4="";showAdd=false}){Text("Add")}},dismissButton={TextButton({showAdd=false}){Text("Cancel")}})
}
