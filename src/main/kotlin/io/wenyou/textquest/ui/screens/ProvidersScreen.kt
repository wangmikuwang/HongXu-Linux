package io.wenyou.textquest.ui.screens

import io.wenyou.textquest.ui.common.AppIcons
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.wenyou.textquest.ui.common.AppTextButton
import io.wenyou.textquest.ui.common.AppOutlinedButton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import io.wenyou.textquest.ui.common.AppIcon as Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import io.wenyou.textquest.ui.common.AppText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import io.wenyou.textquest.WenYouApp
import io.wenyou.textquest.data.model.ApiProfile
import io.wenyou.textquest.ui.HubScaffold
import io.wenyou.textquest.ui.R
import io.wenyou.textquest.ui.common.UsagePanel
import io.wenyou.textquest.ui.common.Pill
import io.wenyou.textquest.ui.vm.LibraryViewModel
import io.wenyou.textquest.ui.vm.SettingsViewModel
import io.wenyou.textquest.ui.vm.Vms

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvidersScreen(container: WenYouApp.AppContainer, nav: NavHostController) {
    val libraryVm: LibraryViewModel = viewModel(factory = Vms.factory { LibraryViewModel(container) })
    val settingsVm: SettingsViewModel = viewModel(factory = Vms.factory { SettingsViewModel(container) })
    val providers by libraryVm.providers.collectAsStateWithLifecycle()
    val prefs by settingsVm.ui.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<ApiProfile?>(null) }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("AI 服务") }, navigationIcon = {
            IconButton(onClick = { nav.navigateUp() }) { Icon(AppIcons.ArrowBack, "返回") }
        }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { nav.navigate(R.providerEdit("new")) },
                modifier = Modifier.semantics { contentDescription = "添加服务" },
                icon = { Icon(AppIcons.Add, null) }, text = { Text("添加服务") })
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 116.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("密钥只保存在本机。AI 场景与 AI 导演模式使用「默认服务」（未指定时用列表第一项）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item { UsagePanel(container.chatClient.usage, showLast = false) }
                if (providers.isEmpty()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("还没有接入任何服务", style = MaterialTheme.typography.titleMedium)
                            AppOutlinedButton(
                                onClick = { nav.navigate(R.providerEdit("new")) },
                                modifier = Modifier.padding(top = 12.dp)
                            ) { Text("添加第一家（支持 DeepSeek / Kimi / GLM / Qwen / OpenAI / Claude / Gemini / 本地 Ollama）") }
                        }
                    }
                } else {
                    items(providers, key = { it.id }) { p ->
                        ProviderCard(
                            profile = p,
                            isDefault = p.id == prefs.defaultProviderId,
                            onEdit = { nav.navigate(R.providerEdit(p.id)) },
                            onDelete = { pendingDelete = p },
                            onSetDefault = { settingsVm.setDefaultProvider(p.id) }
                        )
                    }
                }
            }

        }
    }

    pendingDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除服务？") },
            text = { Text("「${p.name}」将从本机移除。") },
            confirmButton = {
                AppTextButton(onClick = {
                    libraryVm.deleteProvider(p.id)
                    if (prefs.defaultProviderId == p.id) settingsVm.setDefaultProvider(null)
                    pendingDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                AppTextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun ProviderCard(
    profile: ApiProfile,
    isDefault: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isDefault) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    io.wenyou.textquest.ui.common.RawText(profile.name, style = MaterialTheme.typography.titleMedium)
                    if (isDefault) {
                        Spacer(Modifier.width(6.dp))
                        Pill("默认", container = MaterialTheme.colorScheme.primary)
                    }
                }
                io.wenyou.textquest.ui.common.AppText(profile.kind.label, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text("模型：${profile.model}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Text("地址：${profile.baseUrl}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                if (!isDefault) {
                    AppTextButton(onClick = onSetDefault) { Text("设为默认") }
                }
            }
            IconButton(onClick = onEdit) { Icon(AppIcons.Edit, "编辑") }
            IconButton(onClick = onDelete) { Icon(AppIcons.Delete, "删除", tint = MaterialTheme.colorScheme.outline) }
        }
    }
}
