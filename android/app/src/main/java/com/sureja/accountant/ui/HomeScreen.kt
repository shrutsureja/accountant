package com.sureja.accountant.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sureja.accountant.data.local.NamedAmount
import com.sureja.accountant.ui.theme.AccountantColors
import com.sureja.accountant.ui.theme.AccountantSpacing
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

@Composable
fun HomeScreen(
    onAdd: () -> Unit,
    onReview: () -> Unit,
    onTransactions: () -> Unit,
    onReports: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val month by viewModel.selectedMonth.collectAsState()
    val name by viewModel.profileName.collectAsState()
    val currentMonth = month == YearMonth.now()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = AccountantSpacing.page, vertical = AccountantSpacing.page),
        verticalArrangement = Arrangement.spacedBy(AccountantSpacing.base),
    ) {
        item {
            Text(
                "Good ${homeGreeting()}${if (name.isNotBlank()) ", $name" else ""}",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(AccountantSpacing.base))
            HomeMonthSelector(month, viewModel::previousMonth, viewModel::nextMonth)
            Text("${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} spending", style = MaterialTheme.typography.bodyMedium, color = AccountantColors.SecondaryText)
            MoneyText(state.total, Modifier.padding(top = AccountantSpacing.xs), fontSize = 40)
            if (state.total > 0 && state.previousTotal > 0) {
                val difference = (state.total - state.previousTotal) * 100.0 / state.previousTotal
                Text(
                    "${if (difference < 0) "↓" else "↑"} ${"%.1f".format(abs(difference))}% vs ${month.minusMonths(1).month.getDisplayName(TextStyle.FULL, Locale.getDefault())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (difference < 0) AccountantColors.Success else AccountantColors.SecondaryText,
                )
            }
        }
            if (currentMonth) item {
                Row(horizontalArrangement = Arrangement.spacedBy(AccountantSpacing.sm)) {
                    HomeMetric("Today", money(state.todayTotal), Modifier.weight(1f))
                    HomeMetric("This week", money(state.weekTotal), Modifier.weight(1f))
                    HomeMetric("Needs review", state.reviewCount.toString(), Modifier.weight(1f), onReview)
                }
            }
            item { SectionTitle("Household spending") }
            items(state.people, key = { it.id }) { PersonSpendRow(it, state.total, showBar = true) }
            item { SectionTitle("Top categories", onReports, "View reports") }
            if (state.categories.isEmpty()) item { Text("No category spending yet", style = MaterialTheme.typography.bodySmall, color = AccountantColors.SecondaryText) }
            else items(state.categories, key = { it.id }) { PersonSpendRow(it, state.total, showBar = true, category = true) }
            item { SectionTitle("Recent expenses", onTransactions, "See all") }
            if (state.recent.isEmpty()) item { Text("No recent expenses yet", style = MaterialTheme.typography.bodySmall, color = AccountantColors.SecondaryText) }
            else items(state.recent.take(4), key = { it.id }) { TransactionRow(it, onTransactions) }
    }
}

@Composable
private fun HomeMonthSelector(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onPrevious, Modifier.size(48.dp)) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month") }
        Text(
            "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(onNext, Modifier.size(48.dp), enabled = month < YearMonth.now()) { Icon(Icons.Default.ChevronRight, contentDescription = "Next month") }
    }
}

@Composable
private fun HomeReviewRow(count: Int, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.small, color = AccountantColors.BlueLight) {
        Row(Modifier.fillMaxWidth().padding(AccountantSpacing.base), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = AccountantColors.Blue)
            Column(Modifier.weight(1f).padding(horizontal = AccountantSpacing.md)) {
                Text("$count expense${if (count == 1) "" else "s"} need review", style = MaterialTheme.typography.titleSmall)
                Text("Detected from SMS and notifications", style = MaterialTheme.typography.bodySmall, color = AccountantColors.SecondaryText)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccountantColors.SecondaryText)
        }
    }
}

@Composable
private fun HomeMetric(label: String, value: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Surface(
        onClick = onClick ?: {},
        modifier = modifier.heightIn(min = 76.dp),
        enabled = onClick != null,
        shape = MaterialTheme.shapes.small,
        color = AccountantColors.Surface,
        border = BorderStroke(1.dp, AccountantColors.Border),
    ) {
        Column(Modifier.padding(AccountantSpacing.md), verticalArrangement = Arrangement.spacedBy(AccountantSpacing.xs)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = AccountantColors.SecondaryText, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun PersonSpendRow(item: NamedAmount, total: Long, showBar: Boolean, category: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AccountantSpacing.md)) {
        if (category) CategoryBadge(item.name) else MemberAvatar(item.name)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AccountantSpacing.sm)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge)
                Text(money(item.amountPaise), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            if (showBar && item.amountPaise > 0 && total > 0) {
                LinearProgressIndicator(
                    progress = { (item.amountPaise.toFloat() / total).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = AccountantColors.Blue,
                    trackColor = AccountantColors.MutedSurface,
                    drawStopIndicator = {},
                )
            }
        }
    }
}

private fun homeGreeting() = when (LocalTime.now().hour) {
    in 5..11 -> "morning"
    in 12..16 -> "afternoon"
    else -> "evening"
}
