package com.sureja.accountant

import android.Manifest
import android.app.KeyguardManager
import android.content.Intent
import android.content.ComponentName
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import java.time.YearMonth
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.*
import androidx.navigation.NavType
import androidx.navigation.navArgument
import android.net.Uri
import dagger.hilt.android.AndroidEntryPoint
import com.sureja.accountant.data.AccountantRepository
import com.sureja.accountant.capture.SmsHistoryScanner
import com.sureja.accountant.capture.TransactionNotificationService
import com.sureja.accountant.export.CsvExporter
import com.sureja.accountant.export.PdfSummaryExporter
import com.sureja.accountant.sync.SyncWorker
import com.sureja.accountant.ui.*
import com.sureja.accountant.ui.theme.AccountantTheme
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var repository: AccountantRepository
    @Inject lateinit var exporter: CsvExporter
    @Inject lateinit var pdfExporter: PdfSummaryExporter
    @Inject lateinit var smsScanner: SmsHistoryScanner
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState);SyncWorker.cancelPeriodic(this);setContent{AccountantTheme{AccountantRoot(::requestBiometric)}} }
    override fun onStart() {
        super.onStart()
        if (NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)) {
            NotificationListenerService.requestRebind(ComponentName(this, TransactionNotificationService::class.java))
        }
    }
    private fun requestBiometric(onSuccess:()->Unit,onUnavailable:()->Unit) {
        val hasDeviceLock = getSystemService(KeyguardManager::class.java).isDeviceSecure
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            (if (hasDeviceLock) BiometricManager.Authenticators.DEVICE_CREDENTIAL else 0)
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Accountant")
            .setSubtitle(if (hasDeviceLock) "Use your fingerprint or phone screen lock" else "Use your fingerprint")
            .setAllowedAuthenticators(authenticators)
        if (!hasDeviceLock) builder.setNegativeButtonText("Cancel")
        val prompt = BiometricPrompt(this, mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            override fun onAuthenticationError(code: Int, message: CharSequence) {
                if (code == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                    code == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                    code == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL
                ) onUnavailable()
            }
        })
        prompt.authenticate(builder.build())
    }
    fun exportCsv(){lifecycleScope.launch{runCatching{exporter.share(repository.exportTransactions())}.onFailure{Toast.makeText(this@MainActivity,"Could not export expenses",Toast.LENGTH_SHORT).show()}}}
    fun exportPdf(){lifecycleScope.launch{runCatching{pdfExporter.share(repository.exportTransactions())}.onFailure{Toast.makeText(this@MainActivity,"Could not export PDF summary",Toast.LENGTH_SHORT).show()}}}
    fun scanSms(month: YearMonth){lifecycleScope.launch{runCatching{smsScanner.scanMonth(month)}.onSuccess{Toast.makeText(this@MainActivity,"$it possible expenses added for review",Toast.LENGTH_LONG).show()}.onFailure{Toast.makeText(this@MainActivity,"Could not scan messages",Toast.LENGTH_SHORT).show()}}}
}

@Composable
private fun MainActivity.AccountantRoot(biometric:((()->Unit),(()->Unit))->Unit,session:SessionViewModel=hiltViewModel()) {
    val hasSession by session.hasSession.collectAsState();val biometricEnabled by session.biometricEnabled.collectAsState();var unlocked by remember{mutableStateOf(false)}
    val foregroundSync: SyncViewModel = hiltViewModel()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    AppUpdateGate(hasSession) {
        LaunchedEffect(hasSession, lifecycle) {
            if (hasSession) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { foregroundSync.runWhileOpen() }
        }
        LaunchedEffect(hasSession,biometricEnabled){if(!hasSession)unlocked=false else if(BuildConfig.DEBUG && BuildConfig.LOCAL_AUTH_BYPASS)unlocked=true else if(!biometricEnabled)unlocked=true else if(!unlocked)biometric({unlocked=true},{})}
        when { !hasSession -> LoginScreen(session);!unlocked -> LockedScreen({biometric({unlocked=true},{})},{session.logout()});else -> MainShell(session) }
    }
}

@Composable
private fun MainActivity.MainShell(session:SessionViewModel) {
    val activity = this
    val nav = rememberNavController()
    val displayName by session.displayName.collectAsState()
    val lastSyncAt by session.lastSyncAt.collectAsState()
    val biometricEnabled by session.biometricEnabled.collectAsState()
    val items = listOf(
        AccountantDestination("home", "Home", Icons.Default.Home),
        AccountantDestination("transactions", "Transactions", Icons.Default.ReceiptLong),
        AccountantDestination("add", "Add", Icons.Default.AddCircle),
        AccountantDestination("reports", "Reports", Icons.Default.BarChart),
        AccountantDestination("profile", "Profile", Icons.Default.Person),
    )
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route
    fun openFilteredTransactions(kind: String, id: String) {
        val filterId = if (kind == "category" && id == "uncategorized") UNCATEGORIZED_FILTER_ID else id
        nav.navigate("transactions/filter/$kind/${Uri.encode(filterId)}") { launchSingleTop = true }
    }
    var scanMonth by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val smsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) activity.scanSms(YearMonth.parse(scanMonth)) else Toast.makeText(activity, "SMS permission is needed for the history scan", Toast.LENGTH_LONG).show()
    }
    val alertPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var askedForAlerts by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !askedForAlerts &&
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askedForAlerts = true
            alertPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val liveSmsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Toast.makeText(activity, if (granted) "Live SMS detection enabled" else "Live SMS detection remains off", Toast.LENGTH_LONG).show()
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = {
        if (route !in setOf("review", "categories", "accounts", "issues")) AccountantNavigationBar(items, if (route?.startsWith("transactions/filter/") == true) "transactions" else route) { destination ->
            nav.navigate(destination) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true }
        }
    }) { contentPadding ->
        NavHost(nav, "home", Modifier.padding(contentPadding)) {
            composable("home") { HomeScreen({ nav.navigate("add") }, { nav.navigate("review") }, { nav.navigate("transactions") }, { nav.navigate("reports") }, onCategory = { openFilteredTransactions("category", it) }, onPerson = { openFilteredTransactions("person", it) }) }
            composable("transactions") { TransactionsScreen() }
            composable("transactions/filter/{kind}/{id}", arguments = listOf(navArgument("kind") { type = NavType.StringType }, navArgument("id") { type = NavType.StringType })) { entry ->
                TransactionsScreen(initialCategoryId = entry.arguments?.getString("id")?.takeIf { entry.arguments?.getString("kind") == "category" }, initialPersonId = entry.arguments?.getString("id")?.takeIf { entry.arguments?.getString("kind") == "person" })
            }
            composable("add") { AddExpenseScreen(onSaved = { nav.navigate("transactions") { popUpTo("add") { inclusive = true } } }) }
            composable("reports") { ReportsScreen(onCategory = { openFilteredTransactions("category", it) }, onPerson = { openFilteredTransactions("person", it) }) }
            composable("profile") {
                ProfileScreen(
                    displayName = displayName,
                    lastSyncAt = lastSyncAt,
                    biometricEnabled = biometricEnabled,
                    onBiometricChange = session::setBiometricEnabled,
                    onLogout = { session.logout() },
                    onSync = { SyncWorker.now(activity); Toast.makeText(activity, "Sync scheduled", Toast.LENGTH_SHORT).show() },
                    onExport = activity::exportCsv,
                    onExportPdf = activity::exportPdf,
                    onScanSms = { month -> scanMonth = month.toString(); smsPermission.launch(Manifest.permission.READ_SMS) },
                    onEnableLiveSms = { liveSmsPermission.launch(Manifest.permission.RECEIVE_SMS) },
                    onNotificationAccess = { activity.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                    onBackgroundAutostart = {
                        runCatching { activity.startActivity(Intent("miui.intent.action.OP_AUTO_START")) }
                            .onFailure { activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${activity.packageName}"))) }
                    },
                    onCategories = { nav.navigate("categories") },
                    onAccounts = { nav.navigate("accounts") },
                    onIssues = { nav.navigate("issues") },
                )
            }
            composable("review") { ReviewScreen(onBack = { nav.popBackStack() }) }
            composable("categories") { CategoriesScreen(onBack = { nav.popBackStack() }, onCategory = { openFilteredTransactions("category", it) }) }
            composable("accounts") { AccountsScreen(onBack = { nav.popBackStack() }) }
            composable("issues") { IssuesScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
