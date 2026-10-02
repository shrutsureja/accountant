@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sureja.accountant.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sureja.accountant.data.local.*
import com.sureja.accountant.ui.theme.AccountantColors
import com.sureja.accountant.ui.theme.AccountantSpacing

private enum class TransactionPicker { CATEGORY, PERSON }

@Composable
fun TransactionsScreen(viewModel: TransactionsViewModel = hiltViewModel(), catalog: CatalogViewModel = hiltViewModel()) {
    val transactions by viewModel.items.collectAsState()
    val search by viewModel.search.collectAsState()
    val personId by viewModel.member.collectAsState()
    val categoryId by viewModel.category.collectAsState()
    val thisMonth by viewModel.thisMonth.collectAsState()
    val categories by catalog.categories.collectAsState()
    val categoryUsage by catalog.categoryUsage.collectAsState()
    val members by catalog.members.collectAsState()
    val accounts by catalog.accounts.collectAsState()
    var picker by remember { mutableStateOf<TransactionPicker?>(null) }
    var editTarget by remember { mutableStateOf<TransactionListItem?>(null) }
    val groups = remember(transactions) { groupTransactions(transactions) }
    val orderedCategories = remember(categories, categoryUsage) {
        val sections = categoryPickerSections(categories, categoryUsage)
        sections.frequent + sections.remaining
    }
    val displayedCategory = when (categoryId) {
        UNCATEGORIZED_FILTER_ID -> "Not categorized"
        null -> "Category"
        else -> categories.firstOrNull { it.id == categoryId }?.name ?: "Category"
    }

    Column(Modifier.fillMaxSize().padding(horizontal = AccountantSpacing.page)) {
        PageHeader("Transactions", modifier = Modifier.padding(top = AccountantSpacing.page, bottom = AccountantSpacing.base))
        OutlinedTextField(
            value = search,
            onValueChange = { viewModel.search.value = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search transactions…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = AccountantSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AccountantSpacing.sm),
        ) {
            TransactionFilterChip("All", !thisMonth && personId == null && categoryId == null) { viewModel.clearFilters() }
            TransactionFilterChip("This month", thisMonth) { viewModel.thisMonth.value = !thisMonth }
            TransactionFilterChip(displayedCategory, categoryId != null) { picker = TransactionPicker.CATEGORY }
            TransactionFilterChip(members.firstOrNull { it.id == personId }?.displayName ?: "Person", personId != null) { picker = TransactionPicker.PERSON }
        }
        if (groups.isEmpty()) {
            EmptyState("No matching expenses", "Try a different search or filter. New expenses will appear here.")
        } else {
            androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = AccountantSpacing.lg)) {
                groups.forEach { group ->
                    item(key = "heading-${group.date}") {
                        Text(group.label, Modifier.padding(top = AccountantSpacing.base, bottom = AccountantSpacing.sm), style = MaterialTheme.typography.labelMedium, color = AccountantColors.SecondaryText)
                    }
                    items(group.items.size, key = { group.items[it].id }) { index ->
                        TransactionRow(group.items[index]) { editTarget = group.items[index] }
                        if (index < group.items.lastIndex) HorizontalDivider(color = AccountantColors.Border.copy(alpha = 0.55f))
                    }
                }
            }
        }
    }

    picker?.let { kind ->
        val options = when (kind) {
            TransactionPicker.CATEGORY -> emptyList()
            TransactionPicker.PERSON -> listOf(null to "Everyone") + members.map { it.id to it.displayName }
        }
        if (kind == TransactionPicker.CATEGORY) {
            CategoryPickerSheet(
                title = "Filter by category",
                categories = orderedCategories,
                usage = categoryUsage,
                selectedId = categoryId,
                includeAll = true,
                includeUncategorized = true,
                onSelect = { viewModel.category.value = it; picker = null },
                onDismiss = { picker = null },
            )
        } else ModalBottomSheet(onDismissRequest = { picker = null }, containerColor = AccountantColors.Surface) {
            Column(Modifier.fillMaxWidth().padding(horizontal = AccountantSpacing.page)) {
                Text("Filter by person", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(AccountantSpacing.md))
                androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 480.dp), contentPadding = PaddingValues(bottom = AccountantSpacing.lg)) {
                    items(options.size, key = { options[it].first ?: "all" }) { index ->
                        val (id, name) = options[index]
                        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable {
                            if (kind == TransactionPicker.CATEGORY) viewModel.category.value = id else viewModel.member.value = id
                            picker = null
                        }, verticalAlignment = Alignment.CenterVertically) {
                            Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            if (id == (if (kind == TransactionPicker.CATEGORY) categoryId else personId)) Text("Selected", color = AccountantColors.Blue, style = MaterialTheme.typography.labelMedium)
                        }
                        HorizontalDivider(color = AccountantColors.Border)
                    }
                }
            }
        }
    }

    editTarget?.let { item ->
        TransactionEditDialog(item, orderedCategories, categoryUsage, members, accounts, viewModel, onDismiss = { editTarget = null })
    }
}

@Composable
private fun TransactionFilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, maxLines = 1) },
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccountantColors.BlueLight, selectedLabelColor = AccountantColors.BlueDark),
    )
}

@Composable
private fun TransactionEditDialog(item: TransactionListItem, categories: List<CategoryEntity>, categoryUsage: List<CategoryUsage>, members: List<MemberEntity>, accounts: List<AccountEntity>, viewModel: TransactionsViewModel, onDismiss: () -> Unit) {
    var amount by remember(item.id) { mutableStateOf((item.amountPaise / 100.0).toString()) }
    var category by remember(item.id) { mutableStateOf(item.categoryId) }
    var member by remember(item.id) { mutableStateOf(item.paidByUserId) }
    var payment by remember(item.id) { mutableStateOf(item.paymentMethod) }
    var account by remember(item.id) { mutableStateOf(item.accountId) }
    var merchant by remember(item.id) { mutableStateOf(item.merchant.orEmpty()) }
    var note by remember(item.id) { mutableStateOf(item.note.orEmpty()) }
    var chooseCategory by remember(item.id) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit expense") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(AccountantSpacing.sm)) {
                OutlinedTextField(amount, { amount = it.filter { char -> char.isDigit() || char == '.' } }, label = { Text("Amount") }, prefix = { Text("₹ ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                TransactionEditSelector("Category", categories.firstOrNull { it.id == category }?.name ?: "Not categorized") { chooseCategory = true }
                SelectField("Paid by", members, member, { member = it }, { it.displayName }, { it.id })
                SingleChoiceSegmentedButtonRow {
                    PaymentMethod.entries.forEachIndexed { index, method ->
                        SegmentedButton(method == payment, { payment = method }, SegmentedButtonDefaults.itemShape(index, PaymentMethod.entries.size)) { Text(method.name) }
                    }
                }
                if (payment != PaymentMethod.CASH) SelectField("Account", accounts.filter { it.paymentMethod == payment }, account, { account = it }, { it.name }, { it.id }, true)
                OutlinedTextField(merchant, { merchant = it }, label = { Text("Merchant") })
                OutlinedTextField(note, { note = it }, label = { Text("Note") })
            }
        },
        confirmButton = { Button({ viewModel.update(item, amount, category, member, payment, account, merchant, note); onDismiss() }) { Text("Save") } },
        dismissButton = {
            Row {
                TextButton({ viewModel.delete(item.id); onDismiss() }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                TextButton(onDismiss) { Text("Cancel") }
            }
        },
    )
    if (chooseCategory) CategoryPickerSheet(
        title = "Choose category",
        categories = categories,
        usage = categoryUsage,
        selectedId = category,
        onSelect = { category = it; chooseCategory = false },
        onDismiss = { chooseCategory = false },
    )
}

@Composable
private fun TransactionEditSelector(label: String, value: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.small, border = BorderStroke(1.dp, AccountantColors.Border), color = AccountantColors.Surface) {
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = AccountantSpacing.base), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = AccountantColors.SecondaryText)
                Text(value, style = MaterialTheme.typography.bodyLarge)
            }
            Text("›", color = AccountantColors.SecondaryText)
        }
    }
}
