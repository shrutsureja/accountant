@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sureja.accountant.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sureja.accountant.data.local.CategoryEntity
import com.sureja.accountant.data.local.CategoryUsage
import com.sureja.accountant.ui.theme.AccountantColors
import com.sureja.accountant.ui.theme.AccountantSpacing

const val UNCATEGORIZED_FILTER_ID = "__uncategorized_filter__"

@Composable
fun CategoryPickerSheet(
    title: String,
    categories: List<CategoryEntity>,
    usage: List<CategoryUsage>,
    selectedId: String?,
    includeAll: Boolean = false,
    includeUncategorized: Boolean = false,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val sections = remember(categories, usage) { categoryPickerSections(categories, usage) }
    val frequent = sections.frequent.filter { it.name.contains(query, true) }
    val remaining = sections.remaining.filter { it.name.contains(query, true) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AccountantColors.Surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = AccountantSpacing.page)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
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
            Spacer(Modifier.height(AccountantSpacing.sm))
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp), contentPadding = PaddingValues(bottom = AccountantSpacing.lg)) {
                if (includeAll && "all categories".contains(query, true)) {
                    item { CategoryPickerRow("All categories", selectedId == null, null) { onSelect(null) } }
                }
                if (includeUncategorized && "not categorized".contains(query, true)) {
                    item { CategoryPickerRow("Not categorized", selectedId == UNCATEGORIZED_FILTER_ID, null) { onSelect(UNCATEGORIZED_FILTER_ID) } }
                }
                if (frequent.isNotEmpty()) {
                    item { Text("Frequently used", Modifier.padding(top = AccountantSpacing.sm, bottom = AccountantSpacing.xs), style = MaterialTheme.typography.labelLarge, color = AccountantColors.SecondaryText) }
                    items(frequent, key = { it.id }) { category ->
                        CategoryPickerRow(category.name, selectedId == category.id, category.name) { onSelect(category.id) }
                    }
                }
                if (remaining.isNotEmpty()) {
                    item { Text(if (frequent.isEmpty()) "Categories" else "All categories", Modifier.padding(top = AccountantSpacing.md, bottom = AccountantSpacing.xs), style = MaterialTheme.typography.labelLarge, color = AccountantColors.SecondaryText) }
                    items(remaining, key = { it.id }) { category ->
                        CategoryPickerRow(category.name, selectedId == category.id, category.name) { onSelect(category.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPickerRow(name: String, selected: Boolean, iconName: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onClick).padding(horizontal = AccountantSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
        if (iconName != null) CategoryIcon(iconName, Modifier.size(20.dp))
        Text(name, Modifier.weight(1f).padding(start = if (iconName != null) AccountantSpacing.md else 0.dp), style = MaterialTheme.typography.bodyLarge)
        if (selected) Text("Selected", style = MaterialTheme.typography.labelMedium, color = AccountantColors.Blue)
    }
    HorizontalDivider(color = AccountantColors.Border)
}
