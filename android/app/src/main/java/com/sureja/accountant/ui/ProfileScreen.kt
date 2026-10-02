package com.sureja.accountant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sureja.accountant.ui.theme.AccountantColors
import com.sureja.accountant.ui.theme.AccountantSpacing
import java.time.Duration
import java.time.OffsetDateTime

@Composable
fun ProfileScreen(
    displayName: String,
    lastSyncAt: String?,
    biometricEnabled: Boolean,
    onBiometricChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onSync: () -> Unit,
    onExport: () -> Unit,
    onScanSms: () -> Unit,
    onEnableLiveSms: () -> Unit,
    onNotificationAccess: () -> Unit,
    onCategories: () -> Unit,
    onAccounts: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationEnabled by remember { mutableStateOf(NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)) }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationEnabled = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = AccountantSpacing.page, vertical = AccountantSpacing.page),
    ) {
        item {
            PageHeader("Profile")
            Spacer(Modifier.height(AccountantSpacing.lg))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).background(AccountantColors.BlueLight, CircleShape), contentAlignment = Alignment.Center) {
                    Text(displayName.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleLarge, color = AccountantColors.BlueDark)
                }
                Column(Modifier.padding(start = AccountantSpacing.base)) {
                    Text(displayName.ifBlank { "Household member" }, style = MaterialTheme.typography.titleLarge)
                    Text("Household member", style = MaterialTheme.typography.bodySmall, color = AccountantColors.SecondaryText)
                }
            }
            Spacer(Modifier.height(AccountantSpacing.lg))
        }
        item { ProfileSection("DATA & SYNC") }
        item { ProfileSettingRow(Icons.Default.Sync, "Sync now", syncSubtitle(lastSyncAt), onSync) }
        item { ProfileSettingRow(Icons.Default.Sms, "Scan last 30 days", null, onScanSms) }
        item { ProfileSettingRow(Icons.Default.Download, "Export CSV", null, onExport) }
        item { ProfileSection("AUTOMATION") }
        item { ProfileSettingRow(Icons.Default.Notifications, "Notification access", if (notificationEnabled) "Enabled · Bank and UPI payments" else "Permission required · Capture bank and UPI payments", onNotificationAccess) }
        item { ProfileSettingRow(Icons.Default.Sms, "Live SMS detection", "Allow incoming message detection", onEnableLiveSms) }
        item { ProfileSection("MANAGE") }
        item { ProfileSettingRow(Icons.Default.Category, "Categories", null, onCategories) }
        item { ProfileSettingRow(Icons.Default.AccountBalance, "Accounts", null, onAccounts) }
        item { ProfileSection("SECURITY") }
        item {
            Surface(onClick = { onBiometricChange(!biometricEnabled) }, color = Color.Transparent) {
                Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = AccountantColors.SecondaryText)
                    Column(Modifier.weight(1f).padding(horizontal = AccountantSpacing.base)) {
                        Text("Biometric lock", style = MaterialTheme.typography.bodyLarge)
                        Text(if (biometricEnabled) "On" else "Off", style = MaterialTheme.typography.bodySmall, color = AccountantColors.SecondaryText)
                    }
                    Switch(checked = biometricEnabled, onCheckedChange = null)
                }
            }
        }
        item {
            HorizontalDivider(Modifier.padding(vertical = AccountantSpacing.base), color = AccountantColors.Border)
            ProfileSettingRow(Icons.Default.Logout, "Log out", null, onLogout, AccountantColors.Danger)
        }
    }
}

@Composable
private fun ProfileSection(title: String) {
    Text(
        title,
        Modifier.fillMaxWidth().padding(top = AccountantSpacing.lg, bottom = AccountantSpacing.sm),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = AccountantColors.SecondaryText,
    )
}

@Composable
private fun ProfileSettingRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit, color: Color = AccountantColors.Text) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (color == AccountantColors.Danger) color else AccountantColors.SecondaryText)
            Column(Modifier.weight(1f).padding(horizontal = AccountantSpacing.base)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = color)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AccountantColors.SecondaryText)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccountantColors.SecondaryText)
        }
    }
}

private fun syncSubtitle(lastSyncAt: String?): String = lastSyncAt?.let { value ->
    runCatching {
        val elapsed = Duration.between(OffsetDateTime.parse(value).toInstant(), java.time.Instant.now()).toMinutes()
        when {
            elapsed < 1 -> "Last synced just now"
            elapsed < 60 -> "Last synced $elapsed min ago"
            elapsed < 1440 -> "Last synced ${elapsed / 60} hr ago"
            else -> "Last synced ${elapsed / 1440} days ago"
        }
    }.getOrDefault("Last sync available")
} ?: "Not synced yet"
