package com.sureja.accountant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.sureja.accountant.BuildConfig
import com.sureja.accountant.domain.UpdateRequirement
import com.sureja.accountant.domain.updateRequirement

@Composable
fun AppUpdateGate(sessionActive: Boolean, content: @Composable () -> Unit) {
    val model: AppUpdateViewModel = hiltViewModel()
    val state by model.state.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(sessionActive) { model.refresh() }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) model.refresh() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    var dismissedVersion by rememberSaveable { mutableIntStateOf(0) }
    val requirement = updateRequirement(state.policy, BuildConfig.VERSION_CODE)
    if (!state.checked || requirement == UpdateRequirement.REQUIRED) {
        Surface(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                if (!state.checked) LoadingState("Checking app version…") else PageHeader("Accountant", "An update is required to continue.")
            }
        }
    } else content()
    val policy = state.policy
    if (state.checked && policy != null && (requirement == UpdateRequirement.REQUIRED || (requirement == UpdateRequirement.OPTIONAL && dismissedVersion != policy.latestVersionCode))) {
        val required = requirement == UpdateRequirement.REQUIRED
        AlertDialog(
            onDismissRequest = { if (!required) dismissedVersion = policy.latestVersionCode },
            properties = DialogProperties(dismissOnBackPress = !required, dismissOnClickOutside = !required),
            title = { Text(if (required) "Update required" else "Update available") },
            text = { Text("Accountant ${policy.latestVersionName} is available.\n\n${policy.message}") },
            confirmButton = { TextButton(onClick = { if (required) model.refresh() else dismissedVersion = policy.latestVersionCode }) { Text(if (required) "Check again" else "OK") } },
            dismissButton = if (!required) {{ TextButton(onClick = { dismissedVersion = policy.latestVersionCode }) { Text("Later") } }} else null,
        )
    }
}
