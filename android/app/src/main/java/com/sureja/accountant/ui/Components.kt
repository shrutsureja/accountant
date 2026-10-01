package com.sureja.accountant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sureja.accountant.data.local.TransactionListItem
import com.sureja.accountant.data.local.PaymentMethod
import com.sureja.accountant.ui.theme.AccountantColors
import com.sureja.accountant.ui.theme.AccountantSpacing
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun money(paise: Long): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).apply {
    maximumFractionDigits = if (paise % 100 == 0L) 0 else 2
}.format(BigDecimal.valueOf(paise, 2))

@Composable
fun MoneyText(amount: Long, modifier: Modifier = Modifier, fontSize: Int = 28) {
    Text(
        text = money(amount),
        modifier = modifier,
        fontSize = fontSize.sp,
        lineHeight = (fontSize + 6).sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
fun AmountText(amount: Long, modifier: Modifier = Modifier, fontSize: Int = 28) {
    MoneyText(amount, modifier, fontSize)
}

@Composable
fun PageHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AccountantSpacing.xs)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (subtitle != null) Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SectionTitle(text: String, action: (() -> Unit)? = null, actionLabel: String = "View all") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium)
        if (action != null) TextButton(onClick = action) { Text(actionLabel) }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick, modifier.heightIn(min = 52.dp), enabled = enabled, shape = MaterialTheme.shapes.small) { Text(text) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick, modifier.heightIn(min = 52.dp), enabled = enabled, shape = MaterialTheme.shapes.small) { Text(text) }
}

@Composable
fun EmptyState(title: String, body: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Column(
        Modifier.fillMaxWidth().padding(AccountantSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AccountantSpacing.sm),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null) PrimaryButton(actionLabel, onAction)
    }
}

@Composable
fun LoadingState(label: String = "Loading…") {
    Row(
        Modifier.fillMaxWidth().padding(AccountantSpacing.page),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(AccountantSpacing.md))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TransactionRow(item: TransactionListItem, onClick: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 64.dp).padding(vertical = AccountantSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.merchant?.takeIf { it.isNotBlank() } ?: item.categoryName ?: "Expense", style = MaterialTheme.typography.titleSmall)
            Text(
                listOfNotNull(item.categoryName, item.memberName, if (item.paymentMethod == PaymentMethod.UPI) "UPI" else item.paymentMethod.name.lowercase().replaceFirstChar(Char::uppercase)).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                runCatching { OffsetDateTime.parse(item.occurredAt).format(DateTimeFormatter.ofPattern("d MMM, h:mm a")) }.getOrDefault(item.occurredAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MoneyText(item.amountPaise, fontSize = 17)
    }
}

@Composable
fun ErrorBanner(message: String) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
        Text(message, Modifier.fillMaxWidth().padding(AccountantSpacing.md), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

data class AccountantDestination(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AccountantNavigationBar(
    destinations: List<AccountantDestination>,
    selectedRoute: String?,
    onNavigate: (String) -> Unit,
) {
    Surface(color = AccountantColors.Surface, shadowElevation = 0.dp) {
        Column {
            HorizontalDivider(color = AccountantColors.Border)
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                destinations.forEach { destination ->
                    val selected = selectedRoute == destination.route
                    val tint = if (selected) AccountantColors.Blue else AccountantColors.SecondaryText
                    Column(
                        Modifier.weight(1f).heightIn(min = 64.dp).selectable(
                            selected = selected,
                            onClick = { onNavigate(destination.route) },
                            role = Role.Tab,
                        ).padding(vertical = AccountantSpacing.xs),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            Modifier.size(width = 44.dp, height = 28.dp).background(
                                if (selected) AccountantColors.BlueLight else Color.Transparent,
                                RoundedCornerShape(10.dp),
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(destination.icon, contentDescription = null, modifier = Modifier.size(21.dp), tint = tint)
                        }
                        Text(
                            destination.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) AccountantColors.BlueDark else AccountantColors.SecondaryText,
                        )
                    }
                }
            }
        }
    }
}
