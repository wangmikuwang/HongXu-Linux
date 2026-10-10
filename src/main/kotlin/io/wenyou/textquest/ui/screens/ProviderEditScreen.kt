package io.wenyou.textquest.ui.screens

import io.wenyou.textquest.ui.common.AppIcons
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.wenyou.textquest.ui.theme.readableAccent

import io.wenyou.textquest.ui.common.AppTextButton
import io.wenyou.textquest.ui.common.AppOutlinedButton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import io.wenyou.textquest.ui.common.AppIcon as Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import io.wenyou.textquest.ui.common.AppText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import io.wenyou.textquest.WenYouApp
import io.wenyou.textquest.ui.common.AppDropdown
import io.wenyou.textquest.ui.common.AppField
import io.wenyou.textquest.ui.common.SectionHeader
import io.wenyou.textquest.ui.common.TonalCard
import io.wenyou.textquest.ui.vm.ProviderEditorState
import io.wenyou.textquest.ui.vm.ProviderEditorViewModel
import io.wenyou.textquest.ui.vm.Vms

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderEditScreen(container: WenYouApp.AppContainer, nav: NavHostController, providerId: String) {
    val vm: ProviderEditorViewModel = viewModel(
        factory = Vms.factory { ProviderEditorViewModel(if (providerId == "new") null else providerId, container) }
    )
    val ui by vm.ui.collectAsStateWithLifecycle()
    val profile = ui.profile
    var showDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { io.wenyou.textquest.ui.common.AppText(if (ui.isNew) "接入 AI 服务" else "编辑服务") },
                navigationIcon = {
                    IconButton(onClick = { nav.navigateUp() }) {
                        Icon(AppIcons.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (!ui.isNew) {
                        IconButton(onClick = { showDelete = true }) {
                            Icon(AppIcons.Delete, "删除服务", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (profile == null) {
            Text("服务不存在", Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (ui.message.isNotBlank()) {
                item {
                    TonalCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                        io.wenyou.textquest.ui.common.AppText(ui.message, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
            item { SectionHeader("1 · 选择厂商（可一键填充地址与模型）") }
            item {
                TonalCard {
                    val presetKey = profile.let { p ->
                        vm.presets.firstOrNull { it.kind == p.kind && it.baseUrl == p.baseUrl }?.key
                    }
                    AppDropdown(
                        label = "厂商 / 预设",
                        options = vm.presets.map { it.label to it.key },
                        selected = presetKey,
                        onSelect = { vm.applyPreset(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    AppField(value = profile.name, onValueChange = { vm.setName(it) },
                        label = "服务名称", singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    AppField(value = profile.baseUrl, onValueChange = { vm.setBase(it) },
                        label = "接口地址 baseUrl", singleLine = true,
                        supporting = "OpenAI 兼容：…/v1（含 /chat/completions 会自动补全）")
                    Spacer(Modifier.height(8.dp))
                    AppField(value = profile.model, onValueChange = { vm.setModel(it) },
                        label = "模型名 model", singleLine = true,
                        supporting = "选择厂商会自动填入常用模型，也可在下方「读取可用模型」中挑选")
                    Spacer(Modifier.height(8.dp))
                    vm.presets.firstOrNull { it.kind == profile.kind && it.baseUrl == profile.baseUrl }?.keyUrl?.takeIf { it.isNotBlank() }?.let { url ->
                        val context = io.wenyou.textquest.platform.platformContext()
                        AppTextButton(onClick = { io.wenyou.textquest.platform.openLink(context, url) }) { Text("还没有 Key？去服务商平台获取") }
                    }
                    AppField(value = profile.apiKey, onValueChange = { vm.setKey(it) },
                        label = "API Key", singleLine = true,
                        supporting = if (profile.kind.label.contains("Ollama") || profile.note.contains("本地"))
                            "本地服务通常无需 Key" else "只存本机，不会上传",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (profile.apiKey.length > 6) PasswordVisualTransformation() else VisualTransformation.None)
                    Spacer(Modifier.height(8.dp))
                    AppField(value = profile.note, onValueChange = { vm.setNote(it) },
                        label = "备注（可选）", singleLine = true)
                }
            }
            item { ModelPickerCard(ui, vm) }
            item { SectionHeader("2 · 生成参数") }
            item {
                TonalCard {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("创意温度", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(90.dp))
                        Slider(
                            value = profile.temperature.toFloat().coerceAtMost(1f),
                            onValueChange = { vm.setTemperature(it.toDouble()) },
                            valueRange = 0.0f..1.0f
                        )
                        io.wenyou.textquest.ui.common.RawText(String.format(java.util.Locale.ROOT, "%.2f", minOf(profile.temperature, 1.0)), style = MaterialTheme.typography.labelMedium)
                    }
                    Text("越高越有想象力，越低越稳定；剧情游玩和 AI 创建都使用这个值。", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { SectionHeader("3 · 费用估算（可选）") }
            item {
                TonalCard {
                    Text("DeepSeek 官方接口的已收录模型，全部留空时采用官方价格快照（2026-10-03，含峰谷时段）。填写任一价格将优先使用手动单价，单位为每百万 tokens；0 表示免费。其他服务请填写单价。换模型后请核对。", style = MaterialTheme.typography.bodySmall)
                    AppField(profile.priceCurrency, vm::setCurrency, "币种（CNY / USD）", singleLine = true)
                    listOf(Triple("input", "普通输入单价", profile.inputPrice), Triple("output", "输出单价", profile.outputPrice),
                        Triple("cached", "缓存读取单价", profile.cachedPrice), Triple("write", "缓存写入单价", profile.cacheWritePrice)).forEach { (key, label, value) ->
                        var text by remember(profile.id, key) { mutableStateOf(value?.toString().orEmpty()) }
                        AppField(text, { text = it; vm.setPrice(key, it) }, label, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    }
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppOutlinedButton(
                        onClick = { vm.test() },
                        enabled = !ui.testing,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (ui.testing) CircularProgressIndicator(Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp)
                        else Text("测试连接")
                    }
                    Button(onClick = { vm.save() }, modifier = Modifier.weight(1f)) {
                        Icon(AppIcons.Check, null)
                        Spacer(Modifier.width(6.dp))
                        Text("保存")
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("删除服务？") },
            text = { val name = profile?.name.orEmpty(); Text("「$name」将被移除。") },
            confirmButton = {
                AppTextButton(onClick = {
                    vm.delete()
                    nav.navigateUp()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                AppTextButton(onClick = { showDelete = false }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ModelPickerCard(ui: ProviderEditorState, vm: ProviderEditorViewModel) {
    TonalCard {
        Text("可用模型（从接口读取）", style = MaterialTheme.typography.labelLarge)
        Text("填好接口地址与 Key 后点「读取」，从下面点选一个模型名即可自动填入。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppOutlinedButton(onClick = { vm.refreshModels() }, enabled = !ui.listingModels) {
                if (ui.listingModels) {
                    CircularProgressIndicator(Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("读取可用模型")
                }
            }
            if (ui.listMessage.isNotBlank()) {
                Spacer(Modifier.width(10.dp))
                io.wenyou.textquest.ui.common.AppText(ui.listMessage, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.readableAccent(MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier.weight(1f))
            }
        }
        if (ui.availableModels.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ui.availableModels.forEach { model ->
                    FilterChip(
                        selected = model == ui.profile?.model,
                        onClick = { vm.setModel(model) },
                        label = { io.wenyou.textquest.ui.common.RawText(model, maxLines = 1) }
                    )
                }
            }
        }
    }
}


