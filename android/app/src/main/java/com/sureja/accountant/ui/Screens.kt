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
    Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center){Column(Modifier.widthIn(max=420.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){Icon(Icons.Default.AccountBalanceWallet,null,Modifier.size(44.dp),tint=MaterialTheme.colorScheme.primary);Text("Accountant",style=MaterialTheme.typography.headlineLarge);Text("Household expenses, kept simple.",color=MaterialTheme.colorScheme.onSurfaceVariant);OutlinedTextField(username,{username=it},Modifier.fillMaxWidth(),label={Text("Username")},singleLine=true);OutlinedTextField(pin,{pin=it.filter(Char::isDigit).take(8)},Modifier.fillMaxWidth(),label={Text("PIN")},singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword));if(state is LoginState.Error)ErrorBanner((state as LoginState.Error).message);Button({viewModel.login(username,pin)},Modifier.fillMaxWidth().height(52.dp),enabled=state !is LoginState.Loading){if(state is LoginState.Loading)CircularProgressIndicator(Modifier.size(22.dp),strokeWidth=2.dp)else Text("Sign in")};Text("Version ${com.sureja.accountant.BuildConfig.VERSION_NAME} (${com.sureja.accountant.BuildConfig.VERSION_CODE})",style=MaterialTheme.typography.bodySmall);Text("Your expenses remain available offline after the first sign-in.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
}

@Composable fun LockedScreen(onUnlock:()->Unit,onUsePin:()->Unit) { Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(14.dp)){Icon(Icons.Default.Fingerprint,null,Modifier.size(64.dp),tint=MaterialTheme.colorScheme.primary);Text("Accountant",style=MaterialTheme.typography.headlineMedium);Button(onUnlock){Text("Unlock with fingerprint")};TextButton(onUsePin){Text("Use account PIN")}}} }

@Composable internal fun <T>SelectField(label:String,items:List<T>,selected:String?,onSelected:(String)->Unit,name:(T)->String,id:(T)->String,optional:Boolean=false){var expanded by remember{mutableStateOf(false)};ExposedDropdownMenuBox(expanded,{expanded=it}){OutlinedTextField(items.firstOrNull{id(it)==selected}?.let(name)?:if(optional)"None" else "Select",{},Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),readOnly=true,label={Text(label)},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(expanded)});ExposedDropdownMenu(expanded,{expanded=false}){if(optional)DropdownMenuItem({Text("None")},{onSelected("");expanded=false});items.forEach{item->DropdownMenuItem({Text(name(item))},{onSelected(id(item));expanded=false})}}}}

@Composable fun AccountsScreen(onBack:()->Unit,viewModel:CatalogViewModel=hiltViewModel()) {
    val accounts by viewModel.accounts.collectAsState();val members by viewModel.members.collectAsState();var showAdd by remember{mutableStateOf(false)};var name by remember{mutableStateOf("")};var bank by remember{mutableStateOf("")};var last4 by remember{mutableStateOf("")};var owner by remember{mutableStateOf<String?>(null)};var payment by remember{mutableStateOf(PaymentMethod.UPI)}
    Scaffold(topBar={TopAppBar({Text("Accounts")},navigationIcon={IconButton(onBack){Icon(Icons.Default.ArrowBack,null)}},actions={IconButton({showAdd=true}){Icon(Icons.Default.Add,null)}})}){padding->
        LazyColumn(Modifier.padding(padding),contentPadding=PaddingValues(horizontal=20.dp)){items(accounts,key={it.id}){item->Row(Modifier.fillMaxWidth().padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(item.name);Text(listOfNotNull(item.bankName,item.last4?.let{"••$it"},item.paymentMethod.name,if(!item.active)"Archived" else null).joinToString(" • "),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};if(item.id!="account-cash")TextButton({viewModel.toggleAccount(item)}){Text(if(item.active)"Archive" else "Restore")}};HorizontalDivider()}}
    }
    if(showAdd)AlertDialog({showAdd=false},title={Text("Add account")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){OutlinedTextField(name,{name=it},label={Text("Display name")},singleLine=true);OutlinedTextField(bank,{bank=it},label={Text("Bank (optional)")},singleLine=true);OutlinedTextField(last4,{last4=it.filter(Char::isDigit).take(4)},label={Text("Last 4 digits")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number));SelectField("Owner",members,owner,{owner=it},{it.displayName},{it.id},true);SingleChoiceSegmentedButtonRow{listOf(PaymentMethod.UPI,PaymentMethod.OTHER).forEachIndexed{i,p->SegmentedButton(p==payment,{payment=p},SegmentedButtonDefaults.itemShape(i,2)){Text(p.name)}}}}},confirmButton={Button({viewModel.addAccount(name,bank,last4,owner,payment);name="";bank="";last4="";showAdd=false}){Text("Add")}},dismissButton={TextButton({showAdd=false}){Text("Cancel")}})
}
