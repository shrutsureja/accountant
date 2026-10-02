@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sureja.accountant.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sureja.accountant.data.local.DayAmount
import com.sureja.accountant.data.local.NamedAmount
import com.sureja.accountant.ui.theme.AccountantColors
import com.sureja.accountant.ui.theme.AccountantSpacing
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

@Composable
fun ReportsScreen(viewModel: ReportsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val month by viewModel.selectedMonth.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = AccountantSpacing.page, vertical = AccountantSpacing.page)) {
            PageHeader("Reports")
            Spacer(Modifier.height(AccountantSpacing.base))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(viewModel::previousMonth, Modifier.size(48.dp)) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month") }
                Text("${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                IconButton(viewModel::nextMonth, Modifier.size(48.dp), enabled = month < YearMonth.now()) { Icon(Icons.Default.ChevronRight, contentDescription = "Next month") }
            }
            MoneyText(state.total, fontSize = 36)
            Text("Total spending", style = MaterialTheme.typography.bodyMedium, color = AccountantColors.SecondaryText)
            if (state.previousTotal > 0) {
                val change = (state.total - state.previousTotal) * 100.0 / state.previousTotal
                Text("${if (change < 0) "↓" else "↑"} ${"%.1f".format(abs(change))}% vs ${month.minusMonths(1).month.getDisplayName(TextStyle.FULL, Locale.getDefault())}", style = MaterialTheme.typography.bodySmall, color = if (change < 0) AccountantColors.Success else AccountantColors.SecondaryText)
            }
        }
        PrimaryTabRow(selectedTabIndex = tab, containerColor = AccountantColors.Surface, contentColor = AccountantColors.Blue) {
            listOf("Overview", "Categories", "People").forEachIndexed { index, label ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) }, selectedContentColor = AccountantColors.Blue, unselectedContentColor = AccountantColors.SecondaryText)
            }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = AccountantSpacing.page, vertical = AccountantSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AccountantSpacing.lg),
        ) {
            when (tab) {
                0 -> {
                    item { Text("Daily spending", style = MaterialTheme.typography.titleMedium) }
                    if (state.daily.isEmpty()) item { Text("No spending recorded this month.", style = MaterialTheme.typography.bodyMedium, color = AccountantColors.SecondaryText) }
                    else item { DailySpendingChart(month, state.daily) }
                    item {
                        val activeDays = state.daily.count { it.amountPaise > 0 }
                        val dailyAverage = if (activeDays == 0) 0 else state.total / activeDays
                        val highest = state.daily.maxOfOrNull { it.amountPaise } ?: 0
                        Row(horizontalArrangement = Arrangement.spacedBy(AccountantSpacing.sm)) {
                            ReportMetric("Days with expenses", activeDays.toString(), Modifier.weight(1f))
                            ReportMetric("Daily average", money(dailyAverage), Modifier.weight(1f))
                            ReportMetric("Highest day", money(highest), Modifier.weight(1f))
                        }
                    }
                }
                1 -> {
                    if (state.categories.isEmpty()) item { EmptyState("No category data", "Confirmed expenses will appear here by category.") }
                    else items(state.categories, key = { it.id }) { RankedSpendRow(it, state.total) }
                }
                else -> {
                    if (state.people.isEmpty()) item { EmptyState("No member data", "Household spending will appear here.") }
                    else items(state.people, key = { it.id }) { RankedSpendRow(it, state.total) }
                }
            }
        }
    }
}

@Composable
private fun DailySpendingChart(month: YearMonth, days: List<DayAmount>) {
    val amounts = remember(month, days) {
        val byDay = days.associate { LocalDate.parse(it.day).dayOfMonth to it.amountPaise }
        (1..month.lengthOfMonth()).map { byDay[it] ?: 0L }
    }
    val peak = amounts.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    Column(verticalArrangement = Arrangement.spacedBy(AccountantSpacing.sm)) {
        Canvas(
            Modifier.fillMaxWidth().height(190.dp).semantics {
                contentDescription = "Daily spending chart for ${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())}; ${days.count { it.amountPaise > 0 }} days with expenses"
            },
        ) {
            val width = size.width
            val height = size.height
            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
                val y = height * (1f - fraction)
                drawLine(AccountantColors.Border, Offset(0f, y), Offset(width, y), strokeWidth = 1.dp.toPx())
            }
            val slot = width / amounts.size
            val barWidth = (slot * 0.65f).coerceAtLeast(2.dp.toPx())
            amounts.forEachIndexed { index, amount ->
                if (amount > 0) {
                    val barHeight = (amount.toFloat() / peak * height).coerceAtLeast(2.dp.toPx())
                    drawRect(AccountantColors.Blue, topLeft = Offset(index * slot + (slot - barWidth) / 2f, height - barHeight), size = androidx.compose.ui.geometry.Size(barWidth, barHeight))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(1, 8, 15, 22, month.lengthOfMonth()).distinct().forEach { day ->
                Text(day.toString(), style = MaterialTheme.typography.labelSmall, color = AccountantColors.SecondaryText)
            }
        }
    }
}

@Composable
private fun ReportMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.heightIn(min = 78.dp), color = AccountantColors.MutedSurface, shape = MaterialTheme.shapes.small) {
        Column(Modifier.padding(AccountantSpacing.md), verticalArrangement = Arrangement.spacedBy(AccountantSpacing.xs)) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = AccountantColors.SecondaryText)
        }
    }
}

@Composable
private fun RankedSpendRow(item: NamedAmount, total: Long) {
    val share = if (total > 0) item.amountPaise.toDouble() / total else 0.0
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AccountantSpacing.sm)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(item.name, style = MaterialTheme.typography.titleSmall)
            Text(money(item.amountPaise), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        if (item.amountPaise > 0 && total > 0) {
            LinearProgressIndicator(
                progress = { share.toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = AccountantColors.Blue,
                trackColor = AccountantColors.MutedSurface,
                drawStopIndicator = {},
            )
        }
        Text("${(share * 100).toInt()}% of spending", style = MaterialTheme.typography.bodySmall, color = AccountantColors.SecondaryText)
    }
}
