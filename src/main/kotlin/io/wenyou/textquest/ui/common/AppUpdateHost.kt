package io.wenyou.textquest.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.wenyou.textquest.BuildConfig
import io.wenyou.textquest.platform.openLink
import io.wenyou.textquest.platform.platformContext
import io.wenyou.textquest.ui.vm.AppUpdateState
import io.wenyou.textquest.ui.vm.AppUpdateViewModel
import kotlin.system.exitProcess
import io.wenyou.textquest.ui.common.AppText as Text

/** Checks for updates at launch, offers a newer release, and blocks versions below the supported minimum. */
@Composable
internal fun AppUpdateHost(vm: AppUpdateViewModel, content: @Composable () -> Unit) {
    val state by vm.ui.collectAsStateWithLifecycle()
    val context = platformContext()
    LaunchedEffect(vm) { vm.resume() }
    val openRelease = { if (!openLink(context, AppUpdateViewModel.RELEASE_PAGE)) vm.showOpenError() }
    if (state.required) AppUpdateGate(state, { vm.check() }, openRelease) { exitProcess(0) }
    else {
        content()
        if (state.promptVisible) AlertDialog(onDismissRequest = vm::dismissPrompt,
            title = { Text("发现新版本") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { AppUpdateCard(state, { vm.check() }, openRelease) } },
            confirmButton = { AppTextButton(onClick = vm::dismissPrompt) { Text("稍后再说") } })
    }
}

@Composable
internal fun AppUpdateGate(state: AppUpdateState, onCheck: () -> Unit, onOpenRelease: () -> Unit, onExit: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("需要升级后才能使用", style = MaterialTheme.typography.headlineSmall)
            Text("当前版本 ${BuildConfig.VERSION_NAME}，最低支持版本 ${state.policy.minimumVersion}。升级将保留本地资料。")
            AppUpdateCard(state, onCheck, onOpenRelease)
            AppTextButton(onClick = onExit) { Text("退出应用") }
        }
    }
}
