@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sureja.accountant.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sureja.accountant.data.local.CategoryEntity
import com.sureja.accountant.ui.theme.AccountantColors
import com.sureja.accountant.ui.theme.AccountantSpacing

@Composable
fun CategoriesScreen(onBack: () -> Unit, viewModel: CatalogViewModel = hiltViewModel()) {
    val categories by viewModel.categories.collectAsState()
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Categories", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                actions = { IconButton({ editing = null; name = ""; showDialog = true }) { Icon(Icons.Default.Add, contentDescription = "Add category") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (categories.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState("No categories", "Add a category to organize expenses.", "Add category") { editing = null; name = ""; showDialog = true }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = AccountantSpacing.page, vertical = AccountantSpacing.md),
            ) {
                items(categories, key = { it.id }) { category ->
                    var menuOpen by remember(category.id) { mutableStateOf(false) }
                    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = MaterialTheme.shapes.small, color = AccountantColors.BlueLight) {
                            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                CategoryIcon(category.name)
                            }
                        }
                        Column(Modifier.weight(1f).padding(horizontal = AccountantSpacing.md)) {
                            Text(category.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            if (!category.active) Text("Archived", style = MaterialTheme.typography.bodySmall, color = AccountantColors.SecondaryText)
                        }
                        Box {
                            IconButton({ menuOpen = true }, Modifier.size(48.dp)) { Icon(Icons.Default.MoreVert, contentDescription = "Options for ${category.name}") }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(text = { Text("Rename") }, onClick = {
                                    menuOpen = false; editing = category; name = category.name; showDialog = true
                                })
                                DropdownMenuItem(text = { Text(if (category.active) "Archive" else "Restore") }, onClick = {
                                    menuOpen = false; viewModel.toggleCategory(category)
                                })
                            }
                        }
                    }
                    HorizontalDivider(color = AccountantColors.Border)
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editing == null) "Add category" else "Rename category") },
            text = {
                OutlinedTextField(
                    name,
                    { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = editing
                        if (target == null) viewModel.addCategory(name) else viewModel.renameCategory(target, name)
                        showDialog = false
                    },
                    enabled = name.isNotBlank(),
                    shape = MaterialTheme.shapes.small,
                ) { Text(if (editing == null) "Add" else "Save") }
            },
            dismissButton = { TextButton({ showDialog = false }) { Text("Cancel") } },
            shape = MaterialTheme.shapes.large,
        )
    }
}
