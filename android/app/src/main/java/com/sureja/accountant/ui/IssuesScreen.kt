@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sureja.accountant.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sureja.accountant.data.network.IssueDto
import com.sureja.accountant.ui.theme.AccountantColors
import com.sureja.accountant.ui.theme.AccountantSpacing

@Composable
fun IssuesScreen(onBack: () -> Unit, viewModel: IssuesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var composing by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("BUG") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { selected ->
        attachments = selected.take(3)
    }
    LaunchedEffect(state.submitted) {
        if (state.submitted) {
            composing = false
            category = "BUG"; title = ""; description = ""; attachments = emptyList()
            viewModel.clearSubmitted()
        }
    }
    fun goBack() {
        when {
            composing -> { composing = false; viewModel.clearError() }
            state.detail != null -> viewModel.closeDetail()
            else -> onBack()
        }
    }
    BackHandler(composing || state.detail != null) { goBack() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (composing) "Report an issue" else if (state.detail != null) "Report details" else "Reports & requests") },
                navigationIcon = { IconButton(::goBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            if (!composing && state.detail == null) FloatingActionButton(onClick = { composing = true; viewModel.clearError() }, containerColor = AccountantColors.Blue, contentColor = AccountantColors.Surface) {
                Icon(Icons.Default.Add, contentDescription = "Create report")
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = AccountantSpacing.page, vertical = AccountantSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AccountantSpacing.base),
        ) {
            state.error?.let { error -> item { Text(error, color = AccountantColors.Danger, style = MaterialTheme.typography.bodyMedium) } }
            if (composing) {
                item { Text("What would you like to report?", style = MaterialTheme.typography.titleMedium) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(AccountantSpacing.sm)) {
                        listOf("BUG" to "Bug", "FEATURE" to "Feature", "OTHER" to "Other").forEach { (value, label) ->
                            FilterChip(selected = category == value, onClick = { category = value }, label = { Text(label) })
                        }
                    }
                }
                item { OutlinedTextField(title, { title = it.take(120) }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { OutlinedTextField(description, { description = it.take(4000) }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth().heightIn(min = 144.dp), minLines = 4) }
                item {
                    Column {
                        OutlinedButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Icon(Icons.Default.AttachFile, contentDescription = null)
                            Spacer(Modifier.width(AccountantSpacing.sm))
                            Text("Attach screenshots")
                        }
                        Text("${attachments.size} of 3 selected", style = MaterialTheme.typography.bodySmall, color = AccountantColors.SecondaryText)
                        if (attachments.isNotEmpty()) TextButton(onClick = { attachments = emptyList() }) { Text("Remove screenshots") }
                    }
                }
                item {
                    Button(
                        onClick = { viewModel.submit(category, title, description, attachments) },
                        enabled = !state.submitting && title.trim().length >= 3 && description.trim().length >= 10,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) { Text(if (state.submitting) "Sending…" else "Submit report") }
                }
            } else if (state.detail != null) {
                val detail = state.detail!!
                item { IssueHeading(detail.issue) }
                item { Text(detail.issue.description, style = MaterialTheme.typography.bodyLarge) }
                if (detail.screenshots.isNotEmpty()) item { Text("Screenshots", style = MaterialTheme.typography.titleMedium) }
                items(detail.screenshots, key = { it.id }) { screenshot ->
                    val bitmap = state.screenshots[screenshot.id]
                    if (bitmap == null) CircularProgressIndicator(Modifier.size(24.dp))
                    else Image(bitmap.asImageBitmap(), contentDescription = "Attached screenshot", modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp), contentScale = ContentScale.Fit)
                }
            } else {
                item { Text("Share bugs and ideas with the household. Reports are visible to everyone signed in.", style = MaterialTheme.typography.bodyMedium, color = AccountantColors.SecondaryText) }
                if (state.loading && state.issues.isEmpty()) item { CircularProgressIndicator() }
                else if (state.issues.isEmpty()) item { EmptyState("No reports yet", "Tap + to report a bug or suggest a feature.") }
                else items(state.issues, key = { it.id }) { issue ->
                    Surface(onClick = { viewModel.openIssue(issue.id) }, shape = MaterialTheme.shapes.small, color = AccountantColors.Surface) {
                        Column(Modifier.fillMaxWidth().padding(vertical = AccountantSpacing.md), verticalArrangement = Arrangement.spacedBy(AccountantSpacing.xs)) {
                            IssueHeading(issue)
                            Text(issue.description, maxLines = 2, style = MaterialTheme.typography.bodyMedium, color = AccountantColors.SecondaryText)
                            if (issue.screenshotCount > 0) Text("${issue.screenshotCount} screenshot${if (issue.screenshotCount == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = AccountantColors.SecondaryText)
                        }
                    }
                    HorizontalDivider(color = AccountantColors.Border)
                }
            }
        }
    }
}

@Composable
private fun IssueHeading(issue: IssueDto) {
    Column(verticalArrangement = Arrangement.spacedBy(AccountantSpacing.xs)) {
        Text(issue.title, style = MaterialTheme.typography.titleMedium)
        Text("${issue.category.lowercase().replaceFirstChar(Char::uppercase)} · ${issue.status.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)} · ${issue.reporterName} · ${issue.createdAt.take(10)}", style = MaterialTheme.typography.bodySmall, color = AccountantColors.SecondaryText)
    }
}
