@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sureja.accountant.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sureja.accountant.data.local.PaymentMethod
import com.sureja.accountant.ui.theme.AccountantColors
import com.sureja.accountant.ui.theme.AccountantSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private enum class ExpenseSelection { CATEGORY, MEMBER, ACCOUNT }
private data class ExpenseOption(val id: String?, val label: String, val detail: String? = null, val section: String? = null)

@Composable
fun AddExpenseScreen(onSaved: () -> Unit, viewModel: AddViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val lastPayment by viewModel.lastPayment.collectAsState()
    var amount by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var memberId by rememberSaveable { mutableStateOf<String?>(null) }
    var payment by rememberSaveable { mutableStateOf(PaymentMethod.CASH) }
    var paymentChanged by rememberSaveable { mutableStateOf(false) }
    var accountId by rememberSaveable { mutableStateOf<String?>(null) }
    var dateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var merchant by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var selector by remember { mutableStateOf<ExpenseSelection?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.members, state.currentUserId) {
        if (memberId == null && state.currentUserId != null) {
            memberId = state.members.firstOrNull { it.id == state.currentUserId }?.id
        }
    }
    LaunchedEffect(lastPayment) { if (!paymentChanged) payment = lastPayment }

    val selectedDate = LocalDate.parse(dateText)
    val selectedCategory = state.categories.firstOrNull { it.id == categoryId }?.name ?: "Choose category"
    val selectedMember = state.members.firstOrNull { it.id == memberId }?.displayName ?: "Choose person"
    val eligibleAccounts = state.accounts.filter { it.active && it.paymentMethod == payment }
    val selectedAccount = eligibleAccounts.firstOrNull { it.id == accountId }?.let {
        listOfNotNull(it.name, it.last4?.let { last4 -> "••$last4" }).joinToString("  ")
    } ?: "No account"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                PrimaryButton(
                    text = if (state.saving) "Saving…" else "Save expense",
                    onClick = {
                        viewModel.save(amount, categoryId, memberId, payment, accountId, merchant, note, selectedDate, onSaved)
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = AccountantSpacing.page, vertical = AccountantSpacing.md),
                    enabled = !state.saving,
                )
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = AccountantSpacing.page, vertical = AccountantSpacing.page),
            verticalArrangement = Arrangement.spacedBy(AccountantSpacing.base),
        ) {
            item { PageHeader("Add expense") }
            item { ExpenseAmountInput(amount) { amount = it } }
            item { ExpenseSelector("Category", selectedCategory) { selector = ExpenseSelection.CATEGORY } }
            item { ExpenseSelector("Paid by", selectedMember) { selector = ExpenseSelection.MEMBER } }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(AccountantSpacing.sm)) {
                    Text("Payment", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(AccountantSpacing.sm)) {
                        PaymentMethod.entries.forEach { method ->
                            val selected = method == payment
                            Surface(
                                onClick = { payment = method; paymentChanged = true; accountId = null },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                shape = MaterialTheme.shapes.small,
                                color = if (selected) AccountantColors.BlueLight else AccountantColors.Surface,
                                border = BorderStroke(1.dp, if (selected) AccountantColors.Blue else AccountantColors.Border),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        if (method == PaymentMethod.UPI) "UPI" else method.name.lowercase().replaceFirstChar(Char::uppercase),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) AccountantColors.BlueDark else AccountantColors.Text,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (payment != PaymentMethod.CASH) item {
                ExpenseSelector("Account (optional)", selectedAccount) { selector = ExpenseSelection.ACCOUNT }
            }
            item { ExpenseSelector("Date", dateLabel(selectedDate)) { showDatePicker = true } }
            item { ExpenseTextInput("Merchant (optional)", "Where was this spent?", merchant, { merchant = it }) }
            item { ExpenseTextInput("Note (optional)", "Add a note", note, { note = it }, singleLine = false) }
            state.error?.let { error -> item { ErrorBanner(error) } }
        }
    }

    selector?.let { kind ->
        val options = when (kind) {
            ExpenseSelection.CATEGORY -> categoryPickerSections(state.categories, state.categoryUsage).let { sections ->
                sections.frequent.mapIndexed { index, category -> ExpenseOption(category.id, category.name, section = if (index == 0) "Frequently used" else null) } +
                    sections.remaining.mapIndexed { index, category -> ExpenseOption(category.id, category.name, section = if (index == 0 && sections.frequent.isNotEmpty()) "All categories" else null) }
            }
            ExpenseSelection.MEMBER -> state.members.map { ExpenseOption(it.id, it.displayName) }
            ExpenseSelection.ACCOUNT -> listOf(ExpenseOption(null, "No account")) + eligibleAccounts.map {
                ExpenseOption(it.id, it.name, it.last4?.let { last4 -> "••$last4" })
            }
        }
        ExpenseSelectorSheet(
            title = when (kind) {
                ExpenseSelection.CATEGORY -> "Choose category"
                ExpenseSelection.MEMBER -> "Who paid?"
                ExpenseSelection.ACCOUNT -> "Choose account"
            },
            options = options,
            selectedId = when (kind) {
                ExpenseSelection.CATEGORY -> categoryId
                ExpenseSelection.MEMBER -> memberId
                ExpenseSelection.ACCOUNT -> accountId
            },
            searchable = kind == ExpenseSelection.CATEGORY,
            onSelect = { chosen ->
                when (kind) {
                    ExpenseSelection.CATEGORY -> categoryId = chosen
                    ExpenseSelection.MEMBER -> if (chosen != null) memberId = chosen
                    ExpenseSelection.ACCOUNT -> accountId = chosen
                }
                selector = null
            },
            onDismiss = { selector = null },
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton({
                    datePickerState.selectedDateMillis?.let {
                        dateText = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text("Done") }
            },
            dismissButton = { TextButton({ showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(datePickerState) }
    }
}

@Composable
private fun ExpenseAmountInput(value: String, onValueChange: (String) -> Unit) {
    Surface(color = AccountantColors.BlueLight, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(AccountantSpacing.base), verticalArrangement = Arrangement.spacedBy(AccountantSpacing.sm)) {
            Text("Amount", style = MaterialTheme.typography.labelLarge, color = AccountantColors.SecondaryText)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("₹", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = AccountantColors.Text)
                Spacer(Modifier.width(AccountantSpacing.sm))
                BasicTextField(
                    value = value,
                    onValueChange = { input ->
                        val cleaned = input.filter { it.isDigit() || it == '.' }
                        if (cleaned.matches(Regex("\\d*(\\.\\d{0,2})?"))) onValueChange(cleaned)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold, color = AccountantColors.Text),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    cursorBrush = SolidColor(AccountantColors.Blue),
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty()) Text("0", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = AccountantColors.SecondaryText)
                            inner()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ExpenseSelector(label: String, value: String, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AccountantSpacing.sm)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            color = AccountantColors.Surface,
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, AccountantColors.Border),
        ) {
            Row(Modifier.padding(horizontal = AccountantSpacing.base), verticalAlignment = Alignment.CenterVertically) {
                Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccountantColors.SecondaryText)
            }
        }
    }
}

@Composable
private fun ExpenseTextInput(label: String, placeholder: String, value: String, onValueChange: (String) -> Unit, singleLine: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(AccountantSpacing.sm)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = if (singleLine) 52.dp else 76.dp)
                .border(1.dp, AccountantColors.Border, MaterialTheme.shapes.small)
                .background(AccountantColors.Surface, MaterialTheme.shapes.small)
                .padding(AccountantSpacing.base),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = AccountantColors.Text),
            singleLine = singleLine,
            cursorBrush = SolidColor(AccountantColors.Blue),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = AccountantColors.SecondaryText)
                    inner()
                }
            },
        )
    }
}

@Composable
private fun ExpenseSelectorSheet(
    title: String,
    options: List<ExpenseOption>,
    selectedId: String?,
    searchable: Boolean,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AccountantColors.Surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = AccountantSpacing.page)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (searchable) {
                Spacer(Modifier.height(AccountantSpacing.base))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search categories") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                )
            }
            Spacer(Modifier.height(AccountantSpacing.md))
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp), contentPadding = PaddingValues(bottom = AccountantSpacing.lg)) {
                val visible = options.filter { it.label.contains(query, ignoreCase = true) }
                items(visible, key = { it.id ?: "none" }) { option ->
                    option.section?.let { section ->
                        Text(section, Modifier.padding(top = AccountantSpacing.md, bottom = AccountantSpacing.sm), style = MaterialTheme.typography.labelLarge, color = AccountantColors.SecondaryText)
                    }
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { onSelect(option.id) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(option.label, style = MaterialTheme.typography.bodyLarge)
                            option.detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AccountantColors.SecondaryText) }
                        }
                        if (option.id == selectedId) Text("Selected", style = MaterialTheme.typography.labelMedium, color = AccountantColors.Blue)
                    }
                    HorizontalDivider(color = AccountantColors.Border)
                }
            }
        }
    }
}

private fun dateLabel(date: LocalDate): String = when (date) {
    LocalDate.now() -> "Today"
    LocalDate.now().minusDays(1) -> "Yesterday"
    else -> date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
}
