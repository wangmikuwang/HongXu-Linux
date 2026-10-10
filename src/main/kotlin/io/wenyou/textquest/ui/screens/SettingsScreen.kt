package io.wenyou.textquest.ui.screens

import io.wenyou.textquest.platform.appName
import io.wenyou.textquest.platform.openInput
import io.wenyou.textquest.platform.openLink
import io.wenyou.textquest.platform.platformContext
import io.wenyou.textquest.platform.rememberCreateDocument
import io.wenyou.textquest.platform.rememberOpenDocument
import io.wenyou.textquest.platform.rememberPickDirectory
import io.wenyou.textquest.platform.showMessage
import io.wenyou.textquest.platform.writeBytes
import io.wenyou.textquest.ui.common.AppIcons
import io.wenyou.textquest.ui.common.AppOutlinedButton

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import io.wenyou.textquest.ui.common.AppIcon as Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import io.wenyou.textquest.ui.common.AppText as Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.width
import io.wenyou.textquest.ui.common.AppTextButton
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import io.wenyou.textquest.BuildConfig
import io.wenyou.textquest.CrashLog
import io.wenyou.textquest.WenYouApp
import io.wenyou.textquest.ui.HubScaffold
import io.wenyou.textquest.ui.R
import io.wenyou.textquest.ui.common.AppUpdateCard
import io.wenyou.textquest.ui.vm.AppUpdateViewModel
import io.wenyou.textquest.ui.common.AppDropdown
import io.wenyou.textquest.ui.common.EasterEggTitle
import io.wenyou.textquest.ui.common.SectionHeader
import io.wenyou.textquest.ui.common.TonalCard
import io.wenyou.textquest.ui.theme.PrideTheme
import io.wenyou.textquest.ui.common.LocalPrideGalleryOpen
import io.wenyou.textquest.ui.theme.ThemeMode
import io.wenyou.textquest.ui.theme.ThemeStyle
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import io.wenyou.textquest.ui.vm.SettingsViewModel
import io.wenyou.textquest.ui.vm.Vms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(container: WenYouApp.AppContainer, nav: NavHostController, updateVm: AppUpdateViewModel = viewModel(), category: String? = null) {
    if (category == null) { SettingsMenuScreen(nav); return }
    val vm: SettingsViewModel = viewModel(factory = Vms.factory { SettingsViewModel(container) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = platformContext()
    val scope = rememberCoroutineScope()
    val updateState by updateVm.ui.collectAsStateWithLifecycle()
    val openUpdatePage: (Boolean) -> Unit = { opened -> if (!opened) updateVm.showOpenError() }

    var exportKeys by remember { mutableStateOf(false) }
    var confirmImport by remember { mutableStateOf(false) }
    val exportBackup = rememberCreateDocument("application/json") { uri ->
        if (uri == null) return@rememberCreateDocument
        scope.launch {
            val ok = withContext(Dispatchers.IO) { writeBytes(context, uri, vm.exportString(exportKeys).toByteArray(Charsets.UTF_8)) }
            vm.setMessage(if (!ok) "导出失败" else if (exportKeys) "已导出全部数据（含 API Key，请妥善保管文件）" else "已导出全部数据（不含 API Key）")
        }
    }

    // 连点版本号解锁内容开关（α 版默认隐藏 LGBT/18+ 开关）
    var lastTapAt by remember { mutableStateOf(0L) }
    var tapCount by remember { mutableStateOf(0) }
    val openPrideGallery = LocalPrideGalleryOpen.current
    val onVersionTap = {
        if (ui.prideUnlocked) openPrideGallery()
        else {
            val now = System.currentTimeMillis()
            if (now - lastTapAt > 2000L) tapCount = 0
            lastTapAt = now
            tapCount++
            if (tapCount >= 10) {
                tapCount = 0
                vm.unlockContentPrefs()
                openPrideGallery()
            }
        }
    }

    // Developer mode: a long-press on the version unlocks it once; its switches then live in this page.
    val unlockDeveloper = {
        val first = container.devMode.unlock()
        showMessage(context, if (first) "已开启开发者模式" else "开发者模式已开启")
    }

    val importBackup = rememberOpenDocument { uri ->
        if (uri == null) return@rememberOpenDocument
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    openInput(context, uri)?.use { input ->
                        input.readBytes().toString(Charsets.UTF_8)
                    }
                }.getOrNull()
            }
            if (text == null) vm.setMessage("读取文件失败")
            else vm.importString(text)
        }
    }

    // 选择崩溃日志保存目录（系统“文档/Documents”）
    val pickCrashDir = rememberPickDirectory { dir ->
        if (dir == null) return@rememberPickDirectory
        vm.setCrashDir(dir)
        vm.setMessage("崩溃日志目录已设为「Documents」")
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(settingsSections.firstOrNull { it.id == category }?.title ?: "设置") },
                navigationIcon = {
                    IconButton(onClick = { nav.navigateUp() }) {
                        Icon(AppIcons.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).testTag("settings-list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (category == "system") item {
                Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    EasterEggTitle(appName(), MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        tapMessage = "🎬 幕后导演\n导演悄悄递来一张纸条：最精彩的剧情，往往从你不按套路的选择开始。\n今天，主角的名字叫你。",
                        holdMessage = "🪄 第四面墙\n旁白：你长按了标题。\n角色：等等，谁在故事外面戳我们？\n导演：嘘，这是主角的新能力。")
                    Text("外观、AI 与本地数据", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (category == "system") item { io.wenyou.textquest.ui.common.WhatsNewCard() }

            if (category == "system") item {
                AppUpdateCard(updateState, { updateVm.check() }) { openUpdatePage(openLink(context, AppUpdateViewModel.RELEASE_PAGE)) }
            }

            if (ui.message.isNotBlank()) {
                item {
                    TonalCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                        io.wenyou.textquest.ui.common.AppText(ui.message, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }

            if (category == "ai") {
                item {
                    TonalCard(Modifier.clickable { nav.navigate(R.PROVIDERS) }.testTag("settings-manage-providers")) {
                        Text("管理 AI 服务", style = MaterialTheme.typography.titleMedium)
                        Text("添加、编辑服务与模型，查看生成用量和费用", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item { SectionHeader("AI 与生成") }
                item {
                    TonalCard {
                        AppDropdown(
                            label = "默认服务",
                            options = listOf("（使用第一个可用）" to "") +
                                ui.providers.map { it.name to it.id },
                            selected = ui.defaultProviderId ?: "",
                            onSelect = { id -> vm.setDefaultProvider(id.ifBlank { null }) }
                        )
                    }
                }

            }

            if (category == "content") {
                if (!ui.contentUnlocked) item { TonalCard { Text("预置内容按当前偏好显示；单篇剧情的内容范围可在剧情编辑中调整。", style = MaterialTheme.typography.bodyMedium) } }
                // 解锁后，LGBT 内容开关仅在旗帜墙显示。
                if (ui.contentUnlocked && !ui.prideUnlocked) {
                    item { SectionHeader("内容偏好") }
                    item {
                        TonalCard {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text("显示 LGBT（LGBTQ+）内容", style = MaterialTheme.typography.labelLarge)
                                    Text("关闭后隐藏 LGBT 预设剧情与人物。",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = ui.showLgbt, onCheckedChange = { vm.setShowLgbt(it) })
                            }
                        }
                    }
                }

                if (ui.contentUnlocked) {
                    item { SectionHeader("成人内容") }
                    item {
                        TonalCard {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text("显示成人（18+）内容", style = MaterialTheme.typography.labelLarge)
                                    Text("开启后显示成人预设，允许成年、自愿的亲密描写；关闭后保持非露骨。",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = ui.adultContent, onCheckedChange = { vm.setAdultContent(it) })
                            }
                        }
                    }
                }
            }

            if (category == "backup") {
                item { SectionHeader("数据备份") }
                item {
                    TonalCard {
                        Text("整体备份剧情、人物、AI 服务与存档，供恢复或迁移。默认不含 API Key；导入时会保留本机已有的 Key。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = {
                                exportBackup("${BuildConfig.APP_FILE_PREFIX}-backup-${System.currentTimeMillis()}.json")
                            }) { Text("导出备份") }
                            AppOutlinedButton(onClick = { confirmImport = true }) { Text("导入备份") }
                            if (vm.hasImportSnapshot()) AppTextButton(onClick = { vm.undoLastImport() }) { Text("撤销上次导入") }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("导出时包含 API Key", style = MaterialTheme.typography.labelLarge)
                                Text("只在迁移到自己的新设备时开启；文件里的 Key 是明文。", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.width(12.dp))
                            Switch(exportKeys, { exportKeys = it })
                        }
                        if (confirmImport) AlertDialog(onDismissRequest = { confirmImport = false }, title = { Text("导入备份") },
                            text = { Text("导入会用备份替换当前的全部剧情、人物、存档与 AI 服务。导入前会自动保留一份当前数据，之后可以「撤销上次导入」。") },
                            confirmButton = { AppTextButton(onClick = { confirmImport = false; importBackup(arrayOf("application/json", "text/plain", "*/*")) }) { Text("选择备份文件") } },
                            dismissButton = { AppTextButton(onClick = { confirmImport = false }) { Text("取消") } })
                    }
                }

            }

            if (category == "rules") {
                item { SectionHeader("AI 安全") }
                item { BaselineCard(container) }

            }

            if (category == "system") {
                item { SectionHeader("诊断与关于") }
                item {
                    TonalCard {
                        Text("崩溃日志保存位置", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        val dir = vm.crashDir()
                        io.wenyou.textquest.ui.common.AppText(if (dir != null) "已设置：$dir" else "默认保存在应用内。可选择系统文档目录。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AppOutlinedButton(onClick = { pickCrashDir() }) { Text("选择系统文档目录") }
                            Button(onClick = {
                                val t = "测试日志 time=${System.currentTimeMillis()}\nversion=${BuildConfig.VERSION_NAME}\n"
                                CrashLog.write(context, t, vm.crashDir())
                                vm.setMessage("已写入测试日志（请到所选 Documents 目录查看 crash.log）")
                            }) { Text("写入测试日志") }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("日志文件：crash.log",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }

                item {
                    TonalCard {
                        Text("版本", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        Text("v${BuildConfig.VERSION_NAME.substringBefore('-')}（build ${BuildConfig.VERSION_CODE}）\nAI 密钥保存在本机。\n",
                            modifier = Modifier.testTag("pride-version").combinedClickable(onClickLabel = "版本号", onClick = onVersionTap,
                                onLongClickLabel = "开发者模式", onLongClick = unlockDeveloper),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item { DeveloperCard(container) }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
