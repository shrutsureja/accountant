@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sureja.accountant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sureja.accountant.data.local.AccountEntity
import com.sureja.accountant.data.local.PaymentMethod
import com.sureja.accountant.ui.theme.AccountantColors
import com.sureja.accountant.ui.theme.AccountantSpacing

@Composable
fun AccountsScreen(onBack: () -> Unit, viewModel: CatalogViewModel = hiltViewModel()) {
    val accounts by viewModel.accounts.collectAsState()
    val members by viewModel.members.collectAsState()
    var editing by remember { mutableStateOf<AccountEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }
    var last4 by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf<String?>(null) }
    var payment by remember { mutableStateOf(PaymentMethod.UPI) }
    fun edit(account: AccountEntity?) {
        editing = account
        name = account?.name.orEmpty()
        bank = account?.bankName.orEmpty()
        last4 = account?.last4.orEmpty()
        owner = account?.ownerUserId
        payment = account?.paymentMethod ?: PaymentMethod.UPI
        showEditor = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Accounts") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                actions = { IconButton({ edit(null) }) { Icon(Icons.Default.Add, contentDescription = "Add account") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = AccountantSpacing.page, vertical = AccountantSpacing.md),
        ) {
            if (accounts.isEmpty()) item { EmptyState("No accounts yet", "Add a bank or payment account to track where expenses came from.") }
            items(accounts, key = { it.id }) { account ->
                Row(Modifier.fillMaxWidth().heightIn(min = 76.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AccountantSpacing.xs)) {
                        Text(account.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(
                            if (account.paymentMethod == PaymentMethod.CASH) "Cash" else listOfNotNull(
                                account.bankName?.takeIf(String::isNotBlank) ?: "Bank not set",
                                account.last4?.takeIf(String::isNotBlank)?.let { "Account ••$it" } ?: "Number not set",
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = AccountantColors.SecondaryText,
                        )
                        if (!account.active) Text("Archived", style = MaterialTheme.typography.bodySmall, color = AccountantColors.SecondaryText)
                    }
                    if (account.id != "account-cash") {
                        TextButton(onClick = { edit(account) }) { Text("Edit") }
                        TextButton(onClick = { viewModel.toggleAccount(account) }) { Text(if (account.active) "Archive" else "Restore") }
                    }
                }
                HorizontalDivider(color = AccountantColors.Border)
            }
        }
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editing == null) "Add account" else "Edit account") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(AccountantSpacing.sm)) {
                    OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Display name") }, singleLine = true)
                    OutlinedTextField(bank, { bank = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Bank name") }, singleLine = true)
                    OutlinedTextField(last4, { last4 = it.filter(Char::isDigit).take(4) }, modifier = Modifier.fillMaxWidth(), label = { Text("Last 4 digits") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    SelectField("Owner", members, owner, { owner = it.ifBlank { null } }, { it.displayName }, { it.id }, optional = true)
                    SingleChoiceSegmentedButtonRow {
                        listOf(PaymentMethod.UPI, PaymentMethod.OTHER).forEachIndexed { index, method ->
                            SegmentedButton(payment == method, { payment = method }, SegmentedButtonDefaults.itemShape(index, 2)) { Text(if (method == PaymentMethod.UPI) "UPI" else "Other") }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val account = editing
                    if (account == null) viewModel.addAccount(name, bank, last4, owner, payment)
                    else viewModel.updateAccount(account, name, bank, last4, owner, payment)
                    showEditor = false
                }, enabled = name.isNotBlank() && (last4.isBlank() || last4.length == 4)) { Text(if (editing == null) "Add" else "Save") }
            },
            dismissButton = { TextButton({ showEditor = false }) { Text("Cancel") } },
        )
    }
}
