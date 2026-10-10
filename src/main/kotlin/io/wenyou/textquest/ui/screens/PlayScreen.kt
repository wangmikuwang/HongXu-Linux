package io.wenyou.textquest.ui.screens
import androidx.compose.material3.Slider
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.ui.semantics.Role
import io.wenyou.textquest.platform.copyText
import io.wenyou.textquest.platform.platformContext
import io.wenyou.textquest.platform.rememberCreateDocument
import io.wenyou.textquest.platform.shareText
import io.wenyou.textquest.platform.showMessage
import io.wenyou.textquest.platform.writeBytes
import io.wenyou.textquest.ui.common.AppIcons
import androidx.compose.ui.platform.testTag

import io.wenyou.textquest.ui.theme.readableAccent

import io.wenyou.textquest.ui.common.AppTextButton
import io.wenyou.textquest.ui.common.AppOutlinedButton

import io.wenyou.textquest.ui.common.UsagePanel

import io.wenyou.textquest.ui.common.GlassBackdrop
import io.wenyou.textquest.ui.common.liquidGlass
import io.wenyou.textquest.ui.theme.LocalGlassEnabled
import io.wenyou.textquest.ui.theme.LocalThemeStyle
import io.wenyou.textquest.ui.theme.ThemeStyle
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.AlertDialog
import io.wenyou.textquest.ui.vm.ChapterDraft
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import io.wenyou.textquest.ui.common.AppIcon as Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import io.wenyou.textquest.ui.common.AppText as Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import io.wenyou.textquest.data.engine.Transcript
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.wenyou.textquest.WenYouApp
import io.wenyou.textquest.data.model.ApiProfile
import io.wenyou.textquest.data.model.CharacterData
import io.wenyou.textquest.data.model.CharacterMetrics
import io.wenyou.textquest.data.model.EntryKind
import io.wenyou.textquest.data.engine.GameEngine
import io.wenyou.textquest.data.model.LogEntry
import io.wenyou.textquest.data.model.NodeKind
import io.wenyou.textquest.ui.theme.avatarColor
import io.wenyou.textquest.ui.vm.PlayStage
import io.wenyou.textquest.ui.vm.PlayUi
import io.wenyou.textquest.ui.common.StoryBranchTreeDialog
import io.wenyou.textquest.ui.vm.PlayViewModel
import io.wenyou.textquest.ui.vm.Vms

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun PlayScreen(container: WenYouApp.AppContainer, nav: NavHostController, storyId: String, saveId: String) {
    val vm: PlayViewModel = viewModel(
        factory = Vms.factory { PlayViewModel(storyId, saveId, container) }
    )
    val ui by vm.ui.collectAsStateWithLifecycle()
    val progress by container.library.progress.collectAsStateWithLifecycle()
    val visited = progress.firstOrNull { it.storyId == ui.story?.id }?.visitedNodes.orEmpty()
    if (ui.stage == PlayStage.ROLE_SELECT) RoleSelectionDialog(ui.characters,
        onStart = vm::selectPlayerCharacter, onCancel = { nav.navigateUp() })
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val capturing by rememberUpdatedState(androidx.compose.ui.platform.LocalScrollCaptureInProgress.current)

    LaunchedEffect(ui.lastMessage) {
        if (ui.lastMessage.isNotBlank()) snackbar.showSnackbar(ui.lastMessage)
    }
    val achievementMessage = ui.achievementMessages.firstOrNull()
    LaunchedEffect(achievementMessage) {
        if (achievementMessage != null) {
            val title = io.wenyou.textquest.data.engine.Achievement.entries.firstOrNull { it.name == achievementMessage }?.title
            if (title != null) snackbar.showSnackbar("🏆 解锁成就：$title")
            vm.consumeAchievementMessage()
        }
    }
    var achievementsOpen by rememberSaveable { mutableStateOf(false) }
    if (achievementsOpen) AchievementsDialog(container.library, onDismiss = { achievementsOpen = false })
    ui.chapter?.let { ChapterDialog(it, vm, ui.story?.title.orEmpty()) }
    val history = ui.session?.history.orEmpty()
    val live = ui.stage == PlayStage.AI_WORKING
    var showProvider by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    // Follow new lines only while the reader is at the end or has just acted; otherwise offer a jump instead.
    val nearEnd by remember { derivedStateOf {
        val info = listState.layoutInfo
        (info.visibleItemsInfo.lastOrNull()?.index ?: -1) >= info.totalItemsCount - 3
    } }
    var unseen by remember { mutableStateOf(false) }
    val lastItem = history.size - 1 + if (live) 1 else 0
    // The list as it was before the latest change: a reply of several lines must not count as "scrolled away".
    var shownSize by remember { mutableStateOf(-1) }
    var shownLast by remember { mutableStateOf(-1) }
    LaunchedEffect(history.size, live) {
        val oldSize = shownSize
        val oldLast = shownLast
        shownSize = history.size
        shownLast = lastItem
        if (capturing || lastItem < 0) return@LaunchedEffect
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        if (oldLast < 0 || lastVisible >= oldLast - 2 || history.lastOrNull()?.kind == EntryKind.CHOICE) {
            // Land on the first new line so a long reply reads from its start; the list stops at its end anyway.
            listState.scrollToItem(if (oldSize in 0 until history.size) oldSize else lastItem)
            unseen = false
        } else unseen = true
    }
    LaunchedEffect(nearEnd) { if (nearEnd) unseen = false }

    var usageOpen by remember { mutableStateOf(false) }
    var paceOpen by remember { mutableStateOf(false) }
    var directorOpen by remember { mutableStateOf(false) }
    val dev by container.devMode.state.collectAsStateWithLifecycle()
    if (paceOpen) PaceDialog(ui, vm) { paceOpen = false }
    if (directorOpen) DirectorChatDialog(ui, vm) { directorOpen = false }
    if (usageOpen) io.wenyou.textquest.ui.common.UsageDialog(container.chatClient.usage) { usageOpen = false }
    fun fromConsole(action: () -> Unit) { scope.launch { drawerState.close() }; action() }
    ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
        ConsoleDrawer(ui, vm, directorChat = dev.directorChatOn,
            onPace = { fromConsole { paceOpen = true } },
            onChapter = { fromConsole { vm.draftChapter() } },
            onDirector = { fromConsole { directorOpen = true } },
            onAchievements = { fromConsole { achievementsOpen = true } },
            onProvider = { fromConsole { showProvider = true } },
            onUsage = { fromConsole { usageOpen = true } })
    }) {
        Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        io.wenyou.textquest.ui.common.AppText(ui.story?.title ?: "对局", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (ui.nodeTitle.isNotBlank() && ui.nodeTitle != ui.story?.title) {
                            io.wenyou.textquest.ui.common.RawText(ui.nodeTitle, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { nav.navigateUp() }) {
                        Icon(AppIcons.ArrowBack, "返回")
                    }
                },
                actions = {
                    AppTextButton(onClick = { vm.saveNow() }, enabled = ui.stage != PlayStage.INIT && ui.stage != PlayStage.ROLE_SELECT,
                        modifier = Modifier.semantics { contentDescription = "存档" }) { Text("存档") }
                    // Everything else (pacing, chapters, achievements, AI service, usage, character states) is in the console.
                    IconButton(onClick = { scope.launch { drawerState.open() } }, modifier = Modifier.testTag("open-console")) {
                        Icon(AppIcons.List, "控制台")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        BoxWithConstraints(Modifier.padding(padding).consumeWindowInsets(padding).imePadding().fillMaxSize()) {
        val panelHeight = maxHeight * 0.5f
        val glass = LocalGlassEnabled.current
        val historyContent: @Composable () -> Unit = { Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().testTag("play-history"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 16.dp,
                    bottom = 16.dp
                )
            ) {
                itemsIndexed(history, contentType = { _, entry -> entry.kind }) { _, entry ->
                    StoryEntry(entry, ui.characters)
                    Spacer(Modifier.height(10.dp))
                }
                if (live) {
                    item(key = "live") {
                        if (ui.aiReasoningDelta.isNotBlank()) ThinkingBlock(ui.aiReasoningDelta)
                        StreamingCard()
                        Spacer(Modifier.height(10.dp))
                    }
                }
                item(key = "bottom-space") { Spacer(Modifier.height(8.dp)) }
            }
            if (unseen) FilledTonalButton(onClick = { scope.launch { listState.animateScrollToItem(lastItem); unseen = false } },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).testTag("jump-to-latest")) { Text("↓ 新内容") }
        } }
        if (glass) {
            GlassBackdrop(content = historyContent, controls = {}, footer = {
                Box(Modifier.padding(8.dp).fillMaxWidth().heightIn(max = panelHeight)
                    .testTag("play-actions").liquidGlass().verticalScroll(rememberScrollState())) {
                    Column { UsagePanel(container.chatClient.usage, showLast = false, showStats = false); ActionPanel(vm, ui, nav, visited) }
                }
            })
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) { historyContent() }
                Box(Modifier.fillMaxWidth().heightIn(max = panelHeight).testTag("play-actions").verticalScroll(rememberScrollState())) {
                    Column { UsagePanel(container.chatClient.usage, showLast = false, showStats = false); ActionPanel(vm, ui, nav, visited) }
                }
            }
        }
        }
    }
    }

    if (showProvider) {
        ProviderDialog(
            ui = ui,
            vm = vm,
            container = container,
            onDismiss = { showProvider = false }
        )
    }
}

// ---------------------------------------------------------------------------
// 对局内切换 AI 服务（模型）
// ---------------------------------------------------------------------------

@Composable
private fun ProviderDialog(
    ui: PlayUi,
    vm: PlayViewModel,
    container: WenYouApp.AppContainer,
    onDismiss: () -> Unit
) {
    val defaultId = container.settings.defaultProviderId
    val effective = ui.selectedProviderId
        ?: ui.providers.firstOrNull { it.id == defaultId }?.id
        ?: ui.providers.firstOrNull()?.id
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("使用哪个 AI 服务？") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ProviderOption(
                    label = "跟随默认服务",
                    sub = ui.providers.firstOrNull { it.id == defaultId }?.let { "${it.name} · ${it.model}" }
                        ?: ui.providers.firstOrNull()?.let { "${it.name} · ${it.model}" }
                        ?: "（暂无）",
                    selected = ui.selectedProviderId == null,
                    onClick = { vm.selectProvider(null); onDismiss() }
                )
                ui.providers.forEach { p ->
                    ProviderOption(
                        label = p.name,
                        sub = "${p.model} · ${p.kind.label}",
                        selected = p.id == effective,
                        onClick = { vm.selectProvider(p.id); onDismiss() }
                    )
                }
            }
        },
        confirmButton = { AppTextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ProviderOption(
    label: String,
    sub: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                io.wenyou.textquest.ui.common.RawText(label, style = MaterialTheme.typography.titleSmall)
                if (sub.isNotBlank())
                    io.wenyou.textquest.ui.common.RawText(sub, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (selected) {
                Icon(AppIcons.Check, "当前", tint = MaterialTheme.colorScheme.readableAccent())
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 文案渲染
// ---------------------------------------------------------------------------

@Composable
private fun StoryEntry(entry: LogEntry, characters: List<CharacterData>) {
    val text = entry.text
    if (text.isBlank() && entry.reasoning.isBlank()) return
    if (entry.reasoning.isNotBlank()) ThinkingBlock(entry.reasoning)
    when (entry.kind) {
        EntryKind.CHARACTER -> {
            val char = characters.firstOrNull { it.id == entry.speakerId }
            val color = avatarColor(char?.colorIndex ?: 0)
            val name = entry.speaker.ifBlank { char?.name ?: "角色" }
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = Color(color.red, color.green, color.blue, alpha = 0.10f)
                )
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(50), color = color) {
                            io.wenyou.textquest.ui.common.RawText(char?.emoji ?: "🎭",
                                Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 13.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.readableAccent(),
                            fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    SelectionContainer {
                        io.wenyou.textquest.ui.common.RawText(text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        EntryKind.CHOICE -> {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        if (entry.speakerId.isNotBlank()) Text("${entry.speaker} · 你", style = MaterialTheme.typography.labelSmall)
                        io.wenyou.textquest.ui.common.RawText(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }
        EntryKind.NARRATION, EntryKind.DM -> {
            if (text.isBlank()) return
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("旁白",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(Modifier.height(8.dp))
                    SelectionContainer {
                        io.wenyou.textquest.ui.common.RawText(text, style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
        }
        EntryKind.SYSTEM -> {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                io.wenyou.textquest.ui.common.RawText(text, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.readableAccent(),
                    fontStyle = FontStyle.Italic)
            }
        }
        EntryKind.ERROR -> {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                io.wenyou.textquest.ui.common.RawText(text,
                    Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
private fun StreamingCard() {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text("AI 正在构思…", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("▍", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.readableAccent())
        }
    }
}

// ---------------------------------------------------------------------------
// 底部操作面板
// ---------------------------------------------------------------------------

@Composable
private fun ActionPanel(vm: PlayViewModel, ui: PlayUi, nav: NavHostController, visited: Set<String>) {
    if (ui.providerMissing && ui.stage == PlayStage.DM_INPUT) MissingProviderCard(nav)
    when (ui.stage) {
        PlayStage.INIT, PlayStage.ROLE_SELECT -> Unit
        PlayStage.AI_WORKING -> {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.width(16.dp).height(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("正在写作……", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    AppTextButton(onClick = vm::stopAi, modifier = Modifier.testTag("stop-ai")) { Text("停止生成") }
                }
            }
        }
        PlayStage.AUTHORED -> {
            val node = ui.story?.nodes?.get(ui.nodeId)
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (ui.visibleChoices.isNotEmpty()) {
                    var treeOpen by rememberSaveable { mutableStateOf(false) }
                    if (treeOpen) ui.story?.let { StoryBranchTreeDialog(it, onEdit = null, onDismiss = { treeOpen = false }, currentNodeId = ui.nodeId, visited = visited) }
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("接下来……", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.readableAccent(),
                            modifier = Modifier.weight(1f))
                        AppTextButton(onClick = { treeOpen = true }) { Text("分支图") }
                    }
                    ui.visibleChoices.forEachIndexed { i, choice ->
                        Button(
                            onClick = { vm.chooseAuthored(i) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) { io.wenyou.textquest.ui.common.RawText(choice.text) }
                    }
                } else if (ui.pendingAiChoices.isNotEmpty()) {
                    Text("你的选择：", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.readableAccent(),
                        modifier = Modifier.padding(horizontal = 16.dp))
                    ui.pendingAiChoices.forEachIndexed { i, choice ->
                        FilledTonalButton(
                            onClick = { vm.chooseAi(i) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) { io.wenyou.textquest.ui.common.RawText(choice.text) }
                    }
                    if (ui.aiTargetExit) {
                        AppOutlinedButton(
                            onClick = { vm.aiExitToMainline() },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                        ) { Text("（结束这段，回到主线）") }
                    }
                } else {
                    val isAiNode = node?.kind == NodeKind.AI
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isAiNode) {
                            Text("AI 未给出选项。", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilledTonalButton(onClick = { vm.continueAi() }, modifier = Modifier.weight(1f)) {
                                    Icon(AppIcons.Refresh, null); Spacer(Modifier.width(6.dp)); Text("继续生成")
                                }
                                if (ui.aiTargetExit) {
                                    AppOutlinedButton(onClick = { vm.aiExitToMainline() }, modifier = Modifier.weight(1f)) {
                                        Text("回到主线")
                                    }
                                }
                            }
                        }
                    }
                }
                Surface(Modifier.fillMaxWidth(), color = Color.Transparent) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("存档后随时可在主页继续", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.weight(1f))
                        AppTextButton(onClick = { vm.restart() }) { Text("重开本局") }
                    }
                }
            }
        }
        PlayStage.DM_INPUT -> {
            DmInput(ui, vm)
        }
        PlayStage.STOPPED -> {
            StoppedPanel(ui, vm, nav)
        }
    }
}

@Composable
private fun DmInput(ui: PlayUi, vm: PlayViewModel) {
    var text by rememberSaveable { mutableStateOf("") }
    Surface(Modifier.fillMaxWidth(), color = if (LocalGlassEnabled.current) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (ui.pendingAiChoices.isNotEmpty()) "推荐回复 · 点一下直接采用" else "写下你的行动，或让导演继续",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                AppTextButton(onClick = { vm.dmSend("继续") }) { Text("让导演继续") }
            }
            // Full-width rows so long suggestions stay readable instead of being cut off in a sideways strip.
            ui.pendingAiChoices.forEach { c ->
                Surface(onClick = { vm.dmSend(c.text) }, shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                    io.wenyou.textquest.ui.common.RawText(c.text, Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("输入你想做的事 / 说的话……（Enter 发送，Shift+Enter 换行）") },
                    // Desktop: Enter sends, Shift+Enter starts a new line.
                    modifier = Modifier.weight(1f).onPreviewKeyEvent { e ->
                        val send = e.type == KeyEventType.KeyDown && e.key == Key.Enter && !e.isShiftPressed
                        if (send && text.isNotBlank()) { vm.dmSend(text); text = "" }
                        send
                    },
                    maxLines = 3,
                    minLines = 1,
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.readableAccent(),
                        cursorColor = MaterialTheme.colorScheme.readableAccent(),
                        unfocusedBorderColor = if (LocalThemeStyle.current == ThemeStyle.APPLE) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline,
                        focusedContainerColor = if (LocalThemeStyle.current == ThemeStyle.APPLE) MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.75f) else Color.Transparent,
                        unfocusedContainerColor = if (LocalThemeStyle.current == ThemeStyle.APPLE) MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.75f) else Color.Transparent)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            vm.dmSend(text)
                            text = ""
                        }
                    },
                    enabled = text.isNotBlank()
                ) {
                    Icon(AppIcons.Send, "发送", tint = MaterialTheme.colorScheme.readableAccent())
                }
            }
        }
    }
}

@Composable
private fun PaceDialog(ui: PlayUi, vm: PlayViewModel, onDismiss: () -> Unit) {
    val current = io.wenyou.textquest.data.model.ScenePace.of(ui.session?.pace.orEmpty())
    AlertDialog(onDismissRequest = onDismiss, title = { Text("推进节奏") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("决定 AI 每一轮把剧情往前推多远，随存档保存，可随时更改。", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            io.wenyou.textquest.data.model.ScenePace.entries.forEach { pace ->
                Surface(onClick = { vm.setPace(pace); onDismiss() }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
                    color = if (pace == current) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(pace.label, style = MaterialTheme.typography.titleSmall)
                        Text(pace.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }, confirmButton = { AppTextButton(onClick = onDismiss) { Text("关闭") } })
}

/** Developer mode: talk to the director outside the story; requests become memos that later turns follow. */
@Composable
private fun DirectorChatDialog(ui: PlayUi, vm: PlayViewModel, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val notes = ui.session?.directorNotes.orEmpty()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("与导演对话") }, text = {
        Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("跳出剧情直接和导演交流：问问构思与伏笔，或提出后续剧情的要求。要求会记为导演备忘，之后每一轮都会参考。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ui.directorChat.forEach { m ->
                Surface(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
                    color = if (m.fromPlayer) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(if (m.fromPlayer) "你" else "导演", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        SelectionContainer { io.wenyou.textquest.ui.common.RawText(m.text, style = MaterialTheme.typography.bodyMedium) }
                        if (m.note.isNotBlank()) io.wenyou.textquest.ui.common.AppText("已记为备忘：${m.note}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.readableAccent())
                    }
                }
            }
            if (ui.directorChatBusy) Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)); Text("导演正在回复……")
            }
            if (ui.directorChatError.isNotBlank()) io.wenyou.textquest.ui.common.AppText(ui.directorChatError,
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(text, { text = it.take(1000) }, modifier = Modifier.fillMaxWidth().testTag("director-chat-input"),
                placeholder = { Text("例如：接下来让陆晚先发现线索") }, maxLines = 4)
            if (notes.isNotEmpty()) {
                Text("导演备忘（随存档保存）", style = MaterialTheme.typography.titleSmall)
                notes.forEachIndexed { i, note ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        io.wenyou.textquest.ui.common.RawText("· $note", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        AppTextButton(onClick = { vm.removeDirectorNote(i) }) { Text("删除") }
                    }
                }
            }
        }
    }, confirmButton = {
        Button(enabled = text.isNotBlank() && !ui.directorChatBusy, onClick = { vm.sendDirectorChat(text); text = "" }) { Text("发送") }
    }, dismissButton = { AppTextButton(onClick = onDismiss) { Text("关闭") } })
}

/** Review, edit or export the recap, then continue the story in a fresh conversation built on it. */
@Composable
private fun ChapterDialog(draft: ChapterDraft, vm: PlayViewModel, title: String) {
    val context = platformContext()
    var text by remember(draft.summary) { mutableStateOf(draft.summary) }
    AlertDialog(onDismissRequest = { if (!draft.busy) vm.dismissChapter() }, title = { Text("总结并开启新篇章") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("对话太长、AI 开始遗忘或提示超出上下文时，可以把至今的剧情整理成前情提要，在新对话里继续。人物状态、关系和标记会保留，当前进度另存一份。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            when {
                draft.busy -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp)); Text("正在整理剧情……")
                }
                else -> {
                    if (draft.error.isNotBlank()) io.wenyou.textquest.ui.common.AppText(draft.error, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(text, { text = it.take(io.wenyou.textquest.data.ai.AiDirector.RECAP_LIMIT) }, modifier = Modifier.fillMaxWidth(),
                        label = { Text("前情提要（可修改）") }, minLines = 6)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppOutlinedButton(enabled = text.isNotBlank(), onClick = {
                            copyText(context, "前情提要", text)
                            showMessage(context, "已复制前情提要")
                        }) { Text("复制") }
                        AppOutlinedButton(enabled = text.isNotBlank(), onClick = {
                            shareText(context, "$title · 前情提要", "《$title》前情提要\n\n$text", "导出前情提要")
                        }) { Text("导出") }
                        if (draft.error.isNotBlank() || text.isBlank()) AppOutlinedButton(onClick = { vm.draftChapter() }) { Text("重新生成") }
                    }
                }
            }
        }
    }, confirmButton = {
        Button(enabled = !draft.busy && text.isNotBlank(), onClick = { vm.startChapter(text) }) { Text("开启新篇章") }
    }, dismissButton = { AppTextButton(enabled = !draft.busy, onClick = { vm.dismissChapter() }) { Text("取消") } })
}

/** AI scenes need a service; send the player straight to adding one instead of describing where it is. */
@Composable
private fun MissingProviderCard(nav: NavHostController) {
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("还没有可用的 AI 服务", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("AI 导演需要接入一家 AI 服务才能继续。推荐 DeepSeek：注册后创建一个 API Key 填进去即可，按用量计费。",
                style = MaterialTheme.typography.bodySmall)
            Button(onClick = { nav.navigate(io.wenyou.textquest.ui.R.providerEdit("new")) }) { Text("添加 AI 服务") }
        }
    }
}

@Composable
private fun StoppedPanel(ui: PlayUi, vm: PlayViewModel, nav: NavHostController) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.fillMaxWidth().padding(12.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            io.wenyou.textquest.ui.common.AppText(ui.stoppedTitle.ifBlank { "这一局结束了" },
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            io.wenyou.textquest.ui.common.AppText(ui.stoppedMessage, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (ui.providerMissing) Button(onClick = { nav.navigate(io.wenyou.textquest.ui.R.providerEdit("new")) }, modifier = Modifier.fillMaxWidth()) {
                    Text("添加 AI 服务")
                }
                Button(onClick = { vm.restart() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(AppIcons.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text("再来一次")
                }
                if (ui.aiTargetExit || ui.story?.nodes?.get(ui.nodeId)?.kind == NodeKind.AI) FilledTonalButton(onClick = { vm.retryAi() },
                    modifier = Modifier.fillMaxWidth()) {
                    Text("重试")
                }
                AppOutlinedButton(onClick = { nav.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                    Text("返回")
                }
            }
            Spacer(Modifier.height(4.dp))
            val export = rememberTranscriptExport(ui)
            Row(Modifier.align(Alignment.CenterHorizontally)) {
                AppTextButton(onClick = { vm.saveNow() }) { Text("保留这份存档") }
                AppTextButton(onClick = export) { Text("导出对局文本") }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 角色状态抽屉
// ---------------------------------------------------------------------------

/** Saves the journey as a .txt document wherever the player chooses. */
@Composable
private fun rememberTranscriptExport(ui: PlayUi): () -> Unit {
    val context = platformContext()
    val scope = rememberCoroutineScope()
    val launch = rememberCreateDocument("text/plain") { uri ->
        val story = ui.story
        val session = ui.session
        if (uri != null && story != null && session != null) scope.launch {
            val text = Transcript.format(story.title, session, ui.characters, System.currentTimeMillis())
            val saved = withContext(Dispatchers.IO) {
                writeBytes(context, uri, text.toByteArray())
            }
            showMessage(context, if (saved) "已导出对局文本" else "导出失败，请换个位置重试")
        }
    }
    return { ui.story?.let { launch(Transcript.fileName(it.title)) } }
}

@Composable
private fun ConsoleDrawer(ui: PlayUi, vm: PlayViewModel, directorChat: Boolean, onPace: () -> Unit, onChapter: () -> Unit,
    onDirector: () -> Unit, onAchievements: () -> Unit, onProvider: () -> Unit, onUsage: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxHeight().width(320.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            // The drawer draws behind the system bars (edge to edge); keep its content clear of them.
            Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
                .padding(16.dp).verticalScroll(rememberScrollState()).testTag("console"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("控制台", style = MaterialTheme.typography.titleLarge)
            val hasSession = ui.session != null
            val pace = io.wenyou.textquest.data.model.ScenePace.of(ui.session?.pace.orEmpty())
            val export = rememberTranscriptExport(ui)
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                ConsoleOption("推进节奏：${pace.label}", hasSession, onPace)
                if (ui.aiMode) ConsoleOption("总结并开启新篇章",
                    hasSession && ui.stage != PlayStage.AI_WORKING && ui.stage != PlayStage.ROLE_SELECT, onChapter)
                if (directorChat) ConsoleOption("与导演对话", hasSession, onDirector)
                ConsoleOption("🏆 成就馆", true, onAchievements)
                ConsoleOption("切换 AI 服务", ui.providers.isNotEmpty(), onProvider)
                ConsoleOption("生成用量与费用统计", true, onUsage)
                ConsoleOption("导出对局文本", !ui.session?.history.isNullOrEmpty(), export)
            }
            Text("剧情记忆", style = MaterialTheme.typography.titleMedium)
            io.wenyou.textquest.ui.common.AppText(ui.session?.memory?.ifBlank { "AI 续写后会自动记录关键事件，随存档保存。" } ?: "暂无剧情记忆", style = MaterialTheme.typography.bodySmall)
            Text("人物关系与状态", style = MaterialTheme.typography.titleMedium)
            Text("点「调整」可以手动修改人物状态，之后的剧情会按新状态继续。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ui.characters.forEach { c ->
                CharacterStateCard(ui, c, editable = hasSession && ui.stage != PlayStage.AI_WORKING) { vm.setCharacterState(c.id, it) }
            }
        }
    }
}

@Composable
private fun ConsoleOption(label: String, enabled: Boolean, onClick: () -> Unit) {
    Text(label, style = MaterialTheme.typography.bodyLarge,
        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = .38f),
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharacterStateCard(ui: PlayUi, c: io.wenyou.textquest.data.model.CharacterData, editable: Boolean,
    onSave: (io.wenyou.textquest.data.model.CharacterState) -> Unit) {
    val st = ui.session?.characterStates?.get(c.id) ?: io.wenyou.textquest.data.model.CharacterState()
    // Edits stay a draft until saved, so a slider dragged while scrolling changes nothing.
    var draft by remember(c.id) { mutableStateOf<io.wenyou.textquest.data.model.CharacterState?>(null) }
    var flagsText by remember(c.id) { mutableStateOf("") }
    val editing = draft
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.testTag("state-${c.id}")
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                io.wenyou.textquest.ui.common.RawText("${c.emoji} ${c.name}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (editing == null) AppTextButton(onClick = { draft = st; flagsText = st.flags.joinToString("、") }, enabled = editable,
                    modifier = Modifier.testTag("adjust-${c.id}")) { Text("调整") }
            }
            if (editing == null) {
                Text("对玩家的好感、信任及当前状态", style = MaterialTheme.typography.labelSmall)
                CharacterMetrics.defs.forEach { d ->
                    val v = st.metrics[d.key] ?: return@forEach
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${d.icon} ${d.label}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        io.wenyou.textquest.ui.common.RawText(GameEngine.formatNumber(CharacterMetrics.clamp(v)), style = MaterialTheme.typography.bodySmall)
                    }
                    LinearProgressIndicator(
                        progress = { (CharacterMetrics.clamp(v) / 100.0).toFloat() },
                        modifier = Modifier.fillMaxWidth().height(6.dp).padding(top = 2.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                }
                if (st.metrics.isEmpty()) Text("尚未记录状态变化", style = MaterialTheme.typography.bodySmall)
                if (st.lastChangeReason.isNotBlank()) Text("变化原因：${st.lastChangeReason}", style = MaterialTheme.typography.bodySmall)
                st.relationships.forEach { (id, relation) ->
                    val target = ui.characters.firstOrNull { it.id == id }
                    if (target != null) Text("对${target.name}：$relation", style = MaterialTheme.typography.bodySmall)
                }
                if (st.flags.isNotEmpty())
                    Text("标记：${st.flags.joinToString("、")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (st.description.isNotBlank())
                    Text("穿着/外观：${st.description}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                // Atmosphere-only values are offered for adult stories, as in the story editor.
                val offered = CharacterMetrics.defs.filter { it.key != "arousal" || ui.story?.adult == true || it.key in editing.metrics }
                offered.filter { it.key in editing.metrics }.forEach { d ->
                    val v = editing.metrics.getValue(d.key)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${d.icon} ${d.label}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        io.wenyou.textquest.ui.common.RawText(GameEngine.formatNumber(v), style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { draft = editing.copy(metrics = editing.metrics - d.key) }) {
                            Icon(AppIcons.Close, "移除${d.label}")
                        }
                    }
                    Slider(value = v.toFloat(), onValueChange = { draft = editing.copy(metrics = editing.metrics + (d.key to Math.round(it).toDouble())) },
                        valueRange = 0f..100f, modifier = Modifier.testTag("slider-${c.id}-${d.key}"))
                }
                val missing = offered.filter { it.key !in editing.metrics }
                if (missing.isNotEmpty()) {
                    Text("添加状态", style = MaterialTheme.typography.labelSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        missing.forEach { d ->
                            io.wenyou.textquest.ui.common.FilterTag("＋${d.label}", false,
                                { draft = editing.copy(metrics = editing.metrics + (d.key to 50.0)) })
                        }
                    }
                }
                io.wenyou.textquest.ui.common.AppField(value = flagsText, onValueChange = { flagsText = it },
                    label = "标记", placeholder = "用顿号或逗号分隔，例如：成年、已和好", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                io.wenyou.textquest.ui.common.AppField(value = editing.description, onValueChange = { draft = editing.copy(description = it) },
                    label = "穿着/外观", modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    AppTextButton(onClick = { draft = null }) { Text("取消") }
                    AppTextButton(onClick = {
                        onSave(editing.copy(flags = flagsText.split('、', ',', '，', '\n').toSet(), lastChangeReason = "玩家手动调整"))
                        draft = null
                    }, modifier = Modifier.testTag("save-${c.id}")) { Text("保存") }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 思考区（默认折叠，点击展开查看 AI 导演思考过程）
// ---------------------------------------------------------------------------

@Composable
private fun ThinkingBlock(reasoning: String) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
            ) {
                Text("🧠 AI 思考过程", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                io.wenyou.textquest.ui.common.AppText(if (expanded) "收起" else "展开",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.readableAccent())
            }
            if (expanded && reasoning.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                io.wenyou.textquest.ui.common.RawText(reasoning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
