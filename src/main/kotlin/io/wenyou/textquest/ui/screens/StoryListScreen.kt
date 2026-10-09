package io.wenyou.textquest.ui.screens
import io.wenyou.textquest.platform.appName
import io.wenyou.textquest.platform.platformContext
import io.wenyou.textquest.platform.rememberPickImages
import io.wenyou.textquest.platform.shareText
import io.wenyou.textquest.platform.showMessage
import io.wenyou.textquest.ui.common.ShareActions
import io.wenyou.textquest.ui.common.SearchField
import io.wenyou.textquest.ui.common.AppField
import io.wenyou.textquest.data.model.isAutoSaveName
import androidx.compose.runtime.saveable.rememberSaveable
import io.wenyou.textquest.ui.common.AppIcons
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import io.wenyou.textquest.ui.theme.readableAccent

import io.wenyou.textquest.ui.common.AppTextButton

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import io.wenyou.textquest.ui.common.FilterTag
import io.wenyou.textquest.ui.common.AppIcon as Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import io.wenyou.textquest.ui.common.AppText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.wenyou.textquest.WenYouApp
import io.wenyou.textquest.data.model.ContentClass
import io.wenyou.textquest.data.model.NodeKind
import io.wenyou.textquest.data.model.SaveSlot
import io.wenyou.textquest.data.model.Story
import io.wenyou.textquest.data.model.StoryMode
import io.wenyou.textquest.data.repo.ShareCode
import io.wenyou.textquest.ui.HubScaffold
import io.wenyou.textquest.ui.R
import io.wenyou.textquest.ui.common.EmojiBadge
import io.wenyou.textquest.ui.common.Pill
import io.wenyou.textquest.ui.common.QrCode
import io.wenyou.textquest.ui.theme.avatarColor
import io.wenyou.textquest.ui.vm.LibraryViewModel
import io.wenyou.textquest.ui.vm.StoryContentFilter
import io.wenyou.textquest.ui.vm.StoryModeFilter
import io.wenyou.textquest.ui.vm.Vms

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryListScreen(container: WenYouApp.AppContainer, nav: NavHostController) {
    val vm: LibraryViewModel = viewModel(factory = Vms.factory { LibraryViewModel(container) })
    val stories by vm.stories.collectAsStateWithLifecycle()
    val totalStories by vm.totalStories.collectAsStateWithLifecycle()
    val filters by vm.filters.collectAsStateWithLifecycle()
    val allSaves by vm.saves.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(stories, query) {
        stories.filter { s -> query.isBlank() || listOf(s.title, s.subtitle, s.genre).any { it.contains(query.trim(), ignoreCase = true) } }
    }
    var pendingDelete by remember { mutableStateOf<Story?>(null) }
    var managesSaves by remember { mutableStateOf<Story?>(null) }
    // 分享：先选「分享码 or 二维码」，再进对应界面
    var sharePicker by remember { mutableStateOf<Story?>(null) }
    var shareCodeStory by remember { mutableStateOf<Story?>(null) }
    var shareQrStory by remember { mutableStateOf<Story?>(null) }
    // 导入：先选「粘贴分享码 or 扫码识别」
    var importPicker by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf(false) }
    val context = platformContext()
    val scope = rememberCoroutineScope()
    // 单张码选一张，多片码一次选中整套图片
    val pickAlbum = rememberPickImages { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val text = withContext(Dispatchers.IO) { QrCode.decodeShareImages(context, uris) }
                if (text.isNullOrBlank() || !container.shareInbox.offer(text)) {
                    showMessage(context, "未识别到完整分享码，请选择分享海报或同一套的全部二维码")
                }
            }
        }
    }

    HubScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("剧情库") },
                actions = {
                    AppTextButton(onClick = { importPicker = true }) { Text("导入") }
                }
            )
        },
        nav = nav
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { SearchField(query, { query = it }, "搜索剧情名、简介或题材", Modifier.testTag("story-search")) }
                item {
                    FilterChipRow(
                        options = StoryModeFilter.entries,
                        selected = filters.modeFilter,
                        label = { it.label },
                        onSelect = { vm.setModeFilter(it) }
                    )
                }
                item {
                    FilterChipRow(
                        options = StoryContentFilter.entries,
                        selected = filters.contentFilter,
                        label = { it.label },
                        onSelect = { vm.setContentFilter(it) }
                    )
                }
                if (stories.isNotEmpty() && shown.isEmpty()) {
                    item {
                        FilterEmptyState(title = "没有找到「${query.trim()}」", body = "换个关键词试试，或清除搜索。",
                            showReset = true, onReset = { query = "" })
                    }
                } else if (stories.isEmpty()) {
                    item {
                        FilterEmptyState(
                            title = if (totalStories > 0) "该分类下暂无剧情" else "还没有任何剧情",
                            body = if (totalStories > 0) "试试切换上方分类，或清除筛选查看全部。" else "点底栏「创建」编一个故事，或用内置示例练手。",
                            showReset = totalStories > 0,
                            onReset = {
                                vm.setModeFilter(StoryModeFilter.ALL)
                                vm.setContentFilter(StoryContentFilter.ALL)
                            }
                        )
                    }
                } else {
                    items(shown, key = { it.id }) { story ->
                        StoryCard(story,
                            onEdit = { nav.navigate(R.storyEdit(story.id)) },
                            onPlay = { nav.navigate(R.play(story.id)) },
                            onSaves = { managesSaves = story },
                            onShare = { sharePicker = story },
                            onDelete = { pendingDelete = story })
                    }
                }
            }

        }
    }

    pendingDelete?.let { story ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除剧情？") },
            text = { Text("「${story.title}」及其所有存档都会被删除。") },
            confirmButton = {
                AppTextButton(onClick = {
                    vm.deleteStory(story.id)
                    pendingDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                AppTextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }

    managesSaves?.let { story ->
        SavesDialog(
            story = story,
            saves = allSaves.filter { it.state.storyId == story.id }.sortedByDescending { it.updatedAt },
            onLoad = { slot -> nav.navigate(R.play(story.id, slot.id)) },
            onDelete = { slot -> vm.deleteSave(slot.id) },
            onRename = { slot, name -> vm.renameSave(slot, name, story.title) },
            onDismiss = { managesSaves = null }
        )
    }

    sharePicker?.let { story ->
        SharePickDialog(
            title = story.title,
            onLink = { shareOut(context, vm.shareCodeFor(story.id)) { ShareActions.sendLink(context, "剧情", story.title, it) }; sharePicker = null },
            onFile = { shareOut(context, vm.shareCodeFor(story.id)) { ShareActions.sendFile(context, story.title, it) }; sharePicker = null },
            onCode = { shareCodeStory = story; sharePicker = null },
            onQr = { shareQrStory = story; sharePicker = null },
            onDismiss = { sharePicker = null }
        )
    }

    shareCodeStory?.let { story ->
        ShareTextDialog(
            title = story.title,
            code = vm.shareCodeFor(story.id),
            onDismiss = { shareCodeStory = null }
        )
    }

    shareQrStory?.let { story ->
        ShareQrDialog(
            title = story.title,
            code = vm.shareCodeFor(story.id),
            onDismiss = { shareQrStory = null }
        )
    }

    if (importPicker) {
        ImportPickDialog(
            onText = { importText = true; importPicker = false },
            onAlbum = { importPicker = false; pickAlbum() },
            onDismiss = { importPicker = false }
        )
    }

    if (importText) {
        ImportTextDialog(
            onDismiss = { importText = false },
            onImport = { code, cb -> if (container.shareInbox.offer(code)) importText = false else cb("没有找到有效的分享码，请检查是否完整") }
        )
    }

}

/** Runs a share action, or explains why content too large for a share code cannot be shared this way. */
internal fun shareOut(context: android.content.Context, code: String, action: (String) -> Unit) {
    if (code.isBlank()) showMessage(context, "内容超过分享上限，请使用设置中的整包导出")
    else action(code)
}

/** 分享方式：链接、文件、分享码文字或二维码；对方用任一方式都能一步导入。 */
@Composable
fun SharePickDialog(title: String, onLink: () -> Unit, onFile: () -> Unit, onCode: () -> Unit, onQr: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("分享「$title」") },
        text = {
            Column {
                ShareOption("🔗", "发送链接", "对方点开链接即可导入（推荐）", onLink)
                ShareOption("📄", "发送文件", "对方点开文件，选择用本应用打开", onFile)
                ShareOption("🔤", "复制分享码", "对方复制后打开应用，会自动识别", onCode)
                ShareOption("🔳", "二维码", "面对面扫码，或保存成一张分享海报", onQr)
            }
        },
        confirmButton = { AppTextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ShareOption(icon: String, title: String, body: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        io.wenyou.textquest.ui.common.RawText(icon, fontSize = 22.sp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** 分享码（文本）弹窗：复制或系统分享。 */
@Composable
fun ShareTextDialog(title: String, code: String, onDismiss: () -> Unit) {
    if (code.isBlank()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("无法生成分享码") },
            text = { Text("内容不存在或超过 8 MiB，请使用设置中的整包导出。") },
            confirmButton = { AppTextButton(onClick = onDismiss) { Text("关闭") } }
        )
        return
    }
    val clipboard = LocalClipboardManager.current
    val context = platformContext()
    var copied by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("分享码 · $title") },
        text = {
            Column {
                Text(
                    "把下面的分享码发给朋友：对方复制后打开应用会自动识别，也可在「导入 → 粘贴」导入。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = {},
                    readOnly = true,
                    minLines = 3,
                    maxLines = 7,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                io.wenyou.textquest.ui.common.RawText(
                    if (copied) "已复制到剪贴板" else "可复制，或直接调用系统分享。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Row {
                AppTextButton(onClick = {
                    clipboard.setText(AnnotatedString(code))
                    copied = true
                }) { Text("复制") }
                AppTextButton(onClick = {
                    shareText(context, title, code, "分享「$title」")
                }) { Text("分享") }
                AppTextButton(onClick = onDismiss) { Text("关闭") }
            }
        }
    )
}

/** 高对比白色圆角卡片里的二维码，观感贴近设备配对页。 */
@Composable
private fun QrCard(bitmap: androidx.compose.ui.graphics.ImageBitmap, dp: Int, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        modifier = modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = "二维码",
            modifier = Modifier.padding(12.dp).size(dp.dp)
        )
    }
}

/** 模拟“正在连接”的脉冲提示。 */
@Composable
private fun ConnectingIndicator(label: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse)
    )
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha)))
        Spacer(Modifier.width(8.dp))
        io.wenyou.textquest.ui.common.RawText(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.readableAccent())
    }
}

/** 二维码弹窗：内容按页编码（字母数字模式，单页也带页头），多页自动轮播；可保存为一张包含全部页的分享海报。 */
@Composable
fun ShareQrDialog(title: String, code: String, onDismiss: () -> Unit) {
    val pages = remember(code) { ShareCode.qrPages(code) }
    if (pages.isEmpty()) {
        ShareTextDialog(title, code, onDismiss)
        return
    }
    val clipboard = LocalClipboardManager.current
    val context = platformContext()
    val appName = appName()
    var copied by remember { mutableStateOf(false) }
    var idx by remember(pages) { mutableStateOf(0) }
    if (pages.size > 1) LaunchedEffect(pages.size) {
        while (true) {
            delay(3000)
            idx = (idx + 1) % pages.size
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("二维码 · $title") },
        text = {
            Column {
                ConnectingIndicator(if (pages.size > 1) "共 ${pages.size} 张，自动轮播" else "对方扫码即可导入")
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    if (pages.size > 1) IconButton(onClick = { idx = (idx - 1 + pages.size) % pages.size }) {
                        Icon(AppIcons.KeyboardArrowLeft, "上一张", tint = MaterialTheme.colorScheme.readableAccent())
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        val qr = remember(pages[idx]) { QrCode.encodeImage(pages[idx], 640) }
                        if (pages.size > 1) Text("第 ${idx + 1}/${pages.size} 张", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (qr != null) QrCard(qr, 260, Modifier.padding(top = 6.dp))
                    }
                    if (pages.size > 1) IconButton(onClick = { idx = (idx + 1) % pages.size }) {
                        Icon(AppIcons.KeyboardArrowRight, "下一张", tint = MaterialTheme.colorScheme.readableAccent())
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (pages.size > 1) "对方在「导入 → 相机扫码」持续对准屏幕即可自动拼接；不在身边就「保存海报」，对方用「相册识别」选这一张图导入。"
                    else "对方在「导入 → 相机扫码」对准即可；也可以「保存海报」发给对方，用「相册识别」导入。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (copied) {
                    Spacer(Modifier.height(4.dp))
                    io.wenyou.textquest.ui.common.RawText("已复制分享码文字", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        },
        confirmButton = {
            Row {
                AppTextButton(onClick = {
                    clipboard.setText(AnnotatedString(code))
                    copied = true
                }) { Text("复制") }
                AppTextButton(onClick = {
                    val location = QrCode.savePoster(context, title, appName, pages, "${title}_分享海报")
                    showMessage(context, if (location != null) "分享海报已保存到 $location" else "保存失败")
                }) { Text("保存海报") }
                AppTextButton(onClick = onDismiss) { Text("关闭") }
            }
        }
    )
}

/** 导入方式选择：粘贴分享码 / 图片识别。 */
@Composable
fun ImportPickDialog(onText: () -> Unit, onAlbum: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入分享") },
        text = { Text("粘贴别人发来的消息、链接或分享码，或选择分享海报 / 二维码图片。导入前会先预览。") },
        confirmButton = {
            Row {
                AppTextButton(onClick = onText) { Text("粘贴") }
                AppTextButton(onClick = onAlbum) { Text("相册识别") }
                AppTextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}

/** 粘贴分享码文本导入（只补不覆盖）。 */
@Composable
fun ImportTextDialog(onDismiss: () -> Unit, onImport: (String, (String) -> Unit) -> Unit) {
    var text by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入分享码") },
        text = {
            Column {
                Text(
                    "粘贴对方发来的整段消息、链接或分享码都可以。导入前会先预览，已有内容不会被覆盖。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("分享码") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                if (result.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    io.wenyou.textquest.ui.common.RawText(result, style = MaterialTheme.typography.bodySmall,
                        color = if (result.startsWith("导入成功")) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Row {
                AppTextButton(onClick = {
                    if (text.isNotBlank()) onImport(text) { result = it }
                }) { Text("导入") }
                AppTextButton(onClick = onDismiss) { Text("关闭") }
            }
        }
    )
}

/** 横向滚动的过滤 Chip 行（全部 + 各分类）。 */
@Composable
private fun <T> FilterChipRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
    ) {
        options.forEachIndexed { index, opt ->
            FilterTag(
                accentIndex = index,
                modifier = if (label(opt) == "LGBT") Modifier.testTag("pride-filter") else Modifier,
                selected = opt == selected,
                onClick = { onSelect(opt) },
                label = label(opt)
            )
        }
    }
}

/** 全库为空 / 分类筛选后无内容 的占位与「清除筛选」入口。 */
@Composable
private fun FilterEmptyState(title: String, body: String, showReset: Boolean, onReset: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        io.wenyou.textquest.ui.common.RawText(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        io.wenyou.textquest.ui.common.RawText(body, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        if (showReset) {
            AppTextButton(onClick = onReset) { Text("清除筛选") }
        }
    }
}

/** 列出某剧情的所有存档：可读取或删除。 */
@Composable
private fun SavesDialog(
    story: Story,
    saves: List<SaveSlot>,
    onLoad: (SaveSlot) -> Unit,
    onDelete: (SaveSlot) -> Unit,
    onRename: (SaveSlot, String) -> Unit,
    onDismiss: () -> Unit
) {
    var renaming by remember { mutableStateOf<SaveSlot?>(null) }
    renaming?.let { slot ->
        var name by remember(slot.id) { mutableStateOf(slot.name.takeUnless(::isAutoSaveName).orEmpty()) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("重命名存档") },
            text = { AppField(name, { name = it }, "存档名", singleLine = true, supporting = "留空则使用自动名称") },
            confirmButton = { AppTextButton(onClick = { onRename(slot, name); renaming = null }) { Text("保存") } },
            dismissButton = { AppTextButton(onClick = { renaming = null }) { Text("取消") } }
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("存档 · ${story.title}") },
        text = {
            if (saves.isEmpty()) {
                Text("还没有存档。对局页右上角「✓」可保存当前进度。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    saves.forEach { slot ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).clickable { onLoad(slot) }) {
                                val steps = "${slot.state.history.size} 步 · ${LibraryViewModel.formatWhen(slot.updatedAt)}"
                                if (isAutoSaveName(slot.name)) Text(steps, style = MaterialTheme.typography.bodyLarge)
                                else {
                                    io.wenyou.textquest.ui.common.RawText(slot.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    Text(steps, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { renaming = slot }) {
                                Icon(AppIcons.Edit, "重命名存档", tint = MaterialTheme.colorScheme.readableAccent())
                            }
                            IconButton(onClick = { onDelete(slot) }) {
                                Icon(AppIcons.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { AppTextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun StoryCard(story: Story, onEdit: () -> Unit, onPlay: () -> Unit, onSaves: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    val color = avatarColor(story.colorIndex)
    val introduction = story.cardIntroduction()
    var menuOpen by remember { mutableStateOf(false) }
    val modeText = if (story.mode == StoryMode.AI_DIRECTOR) "AI 导演" else "分支剧本"
    Card(
        modifier = Modifier.testTag("story-card-${story.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                EmojiBadge(story.coverEmoji, color, size = 54.dp)
                Spacer(Modifier.width(12.dp))
                Column(
                    Modifier.weight(1f).padding(top = 2.dp).clickable(onClick = onEdit)
                ) {
                    io.wenyou.textquest.ui.common.RawText(story.title, style = MaterialTheme.typography.titleLarge, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                    if (story.subtitle.isNotBlank() && story.subtitle.trim() != introduction)
                        io.wenyou.textquest.ui.common.RawText(story.subtitle, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(AppIcons.MoreVert, "更多", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("读取存档") }, onClick = { menuOpen = false; onSaves() })
                        DropdownMenuItem(text = { Text("分享") }, onClick = { menuOpen = false; onShare() })
                        DropdownMenuItem(text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                            onClick = { menuOpen = false; onDelete() })
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (introduction.isNotBlank()) {
                io.wenyou.textquest.ui.common.RawText(introduction, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().testTag("story-intro-${story.id}"))
                Spacer(Modifier.height(10.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().testTag("story-markers-${story.id}"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Pill(modeText, container = MaterialTheme.colorScheme.secondaryContainer)
                        if (story.lgbt) Pill(ContentClass.LGBT.label,
                            container = MaterialTheme.colorScheme.tertiaryContainer, accentIndex = 2)
                        if (story.adult) Pill(ContentClass.ADULT.label,
                            container = MaterialTheme.colorScheme.primaryContainer, accentIndex = 1)
                    }
                    io.wenyou.textquest.ui.common.RawText(buildList {
                        if (story.genre.isNotBlank()) add(story.genre)
                        add("${story.nodes.size} 场景")
                        if (story.characterIds.isNotEmpty()) add("${story.characterIds.size} 位人物")
                    }.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = onPlay) {
                    Icon(AppIcons.PlayArrow, "游玩")
                }
            }
        }
    }
}
