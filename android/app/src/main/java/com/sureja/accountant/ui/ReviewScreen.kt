@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sureja.accountant.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sureja.accountant.data.local.*
import com.sureja.accountant.ui.theme.AccountantColors
import com.sureja.accountant.ui.theme.AccountantSpacing
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private enum class ReviewSelector { CATEGORY, PERSON, ACCOUNT }

private data class ReviewDraft(
    val item: TransactionListItem,
    val amount: String,
    val categoryId: String?,
    val memberId: String,
    val payment: PaymentMethod,
    val accountId: String?,
    val merchant: String,
    val note: String,
) {
    companion object {
        fun from(item: TransactionListItem) = ReviewDraft(
            item, (item.amountPaise / 100.0).toString(), item.categoryId, item.paidByUserId,
            item.paymentMethod, item.accountId, item.merchant.orEmpty(), item.note.orEmpty(),
        )
    }
}

@Composable
fun ReviewScreen(onBack: () -> Unit, viewModel: ReviewViewModel = hiltViewModel(), catalog: CatalogViewModel = hiltViewModel()) {
    val transactions by viewModel.items.collectAsState()
    val categories by catalog.categories.collectAsState()
    val categoryUsage by catalog.categoryUsage.collectAsState()
    val members by catalog.members.collectAsState()
    val accounts by catalog.accounts.collectAsState()
    var draft by remember { mutableStateOf<ReviewDraft?>(null) }
    var selector by remember { mutableStateOf<ReviewSelector?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Needs review", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (transactions.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState("Nothing to review", "All detected transactions are reviewed.")
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = AccountantSpacing.page, vertical = AccountantSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AccountantSpacing.md),
            ) {
                items(transactions, key = { it.id }) { item ->
                    ReviewTransactionCard(
                        item = item,
                        onIgnore = { viewModel.ignore(item.id) },
                        onEdit = { draft = ReviewDraft.from(item) },
                        onConfirm = {
                            if (item.categoryId == null) draft = ReviewDraft.from(item)
                            else viewModel.confirm(item.id)
                        },
                    )
                }
            }
        }
    }

    draft?.let { current ->
        if (selector == null) {
            ReviewEditSheet(
                draft = current,
                categories = categories,
                members = members,
                accounts = accounts,
                onChange = { draft = it },
                onSelect = { selector = it },
                onSave = {
                    val categoryId = current.categoryId
                    if (categoryId != null && parseAmountPaise(current.amount) != null) {
                        viewModel.update(current.item, current.amount, categoryId, current.memberId, current.payment, current.accountId, current.merchant, current.note)
                        draft = null
                    }
                },
                onDismiss = { draft = null },
            )
        } else {
            val kind = selector!!
            if (kind == ReviewSelector.CATEGORY) {
                CategoryPickerSheet(
                    title = "Choose category",
                    categories = categories,
                    usage = categoryUsage,
                    selectedId = current.categoryId,
                    onSelect = { chosen -> draft = current.copy(categoryId = chosen); selector = null },
                    onDismiss = { selector = null },
                )
            } else {
            val options = when (kind) {
                ReviewSelector.CATEGORY -> emptyList()
                ReviewSelector.PERSON -> members.map { it.id to it.displayName }
                ReviewSelector.ACCOUNT -> listOf("" to "No account") + accounts.filter { it.active && it.paymentMethod == current.payment }.map { it.id to it.name }
            }
            ModalBottomSheet(onDismissRequest = { selector = null }, containerColor = AccountantColors.Surface) {
                Column(Modifier.fillMaxWidth().padding(horizontal = AccountantSpacing.page)) {
                    Text(when (kind) { ReviewSelector.CATEGORY -> "Choose category"; ReviewSelector.PERSON -> "Who paid?"; ReviewSelector.ACCOUNT -> "Choose account" }, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(AccountantSpacing.md))
                    LazyColumn(Modifier.heightIn(max = 480.dp), contentPadding = PaddingValues(bottom = AccountantSpacing.lg)) {
                        items(options, key = { it.first }) { (id, label) ->
                            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable {
                                draft = when (kind) {
                                    ReviewSelector.CATEGORY -> current.copy(categoryId = id)
                                    ReviewSelector.PERSON -> current.copy(memberId = id)
                                    ReviewSelector.ACCOUNT -> current.copy(accountId = id.takeIf { it.isNotEmpty() })
                                }
                                selector = null
                            }, verticalAlignment = Alignment.CenterVertically) { Text(label, style = MaterialTheme.typography.bodyLarge) }
                            HorizontalDivider(color = AccountantColors.Border)
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun ReviewTransactionCard(item: TransactionListItem, onIgnore: () -> Unit, onEdit: () -> Unit, onConfirm: () -> Unit) {
    Surface(color = AccountantColors.Surface, shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, AccountantColors.Border)) {
        Column(Modifier.fillMaxWidth().padding(AccountantSpacing.base), verticalArrangement = Arrangement.spacedBy(AccountantSpacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AccountantSpacing.md)) {
                CategoryBadge(item.categoryName ?: "Not categorized")
                MoneyText(item.amountPaise, fontSize = 24)
                Spacer(Modifier.weight(1f))
                Text(if (item.source == TransactionSource.NOTIFICATION) "Notification" else "SMS", style = MaterialTheme.typography.labelSmall, color = AccountantColors.SecondaryText)
            }
            Text(item.merchant?.takeIf { it.isNotBlank() } ?: "Detected expense", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val date = runCatching { OffsetDateTime.parse(item.occurredAt).format(DateTimeFormatter.ofPattern("d MMM · h:mm a")) }.getOrDefault(item.occurredAt.take(16))
            Text(listOfNotNull(if (item.paymentMethod == PaymentMethod.UPI) "UPI" else item.paymentMethod.name.lowercase().replaceFirstChar(Char::uppercase), date, item.accountName).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = AccountantColors.SecondaryText)
            Text("Category: ${item.categoryName ?: "Not categorized"}", style = MaterialTheme.typography.bodySmall, color = AccountantColors.SecondaryText)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onIgnore) { Text("Ignore", color = AccountantColors.SecondaryText) }
                TextButton(onEdit) { Text("Edit") }
                Button(onConfirm, shape = MaterialTheme.shapes.small) { Text("Confirm") }
            }
        }
    }
}

@Composable
private fun ReviewEditSheet(
    draft: ReviewDraft,
    categories: List<CategoryEntity>,
    members: List<MemberEntity>,
    accounts: List<AccountEntity>,
    onChange: (ReviewDraft) -> Unit,
    onSelect: (ReviewSelector) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AccountantColors.Surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = AccountantSpacing.page), verticalArrangement = Arrangement.spacedBy(AccountantSpacing.md)) {
            Text("Edit detected expense", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                draft.amount,
                { onChange(draft.copy(amount = it.filter { char -> char.isDigit() || char == '.' })) },
                Modifier.fillMaxWidth(),
                label = { Text("Amount") },
                prefix = { Text("₹ ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = MaterialTheme.shapes.small,
            )
            if (draft.categoryId == null) ReviewEditSelector("Category", "Not categorized · Choose category", highlighted = true) { onSelect(ReviewSelector.CATEGORY) }
            else ReviewEditSelector("Category", categories.firstOrNull { it.id == draft.categoryId }?.name ?: "Choose category") { onSelect(ReviewSelector.CATEGORY) }
            ReviewEditSelector("Paid by", members.firstOrNull { it.id == draft.memberId }?.displayName ?: "Choose person") { onSelect(ReviewSelector.PERSON) }
            Row(horizontalArrangement = Arrangement.spacedBy(AccountantSpacing.sm)) {
                PaymentMethod.entries.forEach { method ->
                    FilterChip(
                        selected = draft.payment == method,
                        onClick = { onChange(draft.copy(payment = method, accountId = null)) },
                        label = { Text(if (method == PaymentMethod.UPI) "UPI" else method.name.lowercase().replaceFirstChar(Char::uppercase)) },
                        shape = MaterialTheme.shapes.small,
                    )
                }
            }
            if (draft.payment != PaymentMethod.CASH) {
                ReviewEditSelector("Account", accounts.firstOrNull { it.id == draft.accountId }?.name ?: "No account") { onSelect(ReviewSelector.ACCOUNT) }
            }
            OutlinedTextField(draft.merchant, { onChange(draft.copy(merchant = it)) }, Modifier.fillMaxWidth(), label = { Text("Merchant") }, singleLine = true, shape = MaterialTheme.shapes.small)
            OutlinedTextField(draft.note, { onChange(draft.copy(note = it)) }, Modifier.fillMaxWidth(), label = { Text("Note") }, shape = MaterialTheme.shapes.small)
            PrimaryButton("Save changes", onSave, Modifier.fillMaxWidth(), enabled = draft.categoryId != null && parseAmountPaise(draft.amount) != null)
            Spacer(Modifier.height(AccountantSpacing.md))
        }
    }
}

@Composable
private fun ReviewEditSelector(label: String, value: String, highlighted: Boolean = false, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.small, border = BorderStroke(1.dp, if (highlighted) AccountantColors.Blue else AccountantColors.Border), color = if (highlighted) AccountantColors.BlueLight else AccountantColors.Surface) {
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = AccountantSpacing.base), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = AccountantColors.SecondaryText)
                Text(value, style = MaterialTheme.typography.bodyLarge)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccountantColors.SecondaryText)
        }
    }
}
