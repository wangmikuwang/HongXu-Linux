package io.wenyou.textquest.ui.screens
import io.wenyou.textquest.platform.platformContext
import io.wenyou.textquest.platform.rememberPickImages
import io.wenyou.textquest.platform.showMessage
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import io.wenyou.textquest.ui.common.ShareActions
import io.wenyou.textquest.ui.common.SearchField
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.testTag
import io.wenyou.textquest.ui.common.AppIcons
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import io.wenyou.textquest.ui.theme.readableAccent

import io.wenyou.textquest.ui.common.AppTextButton

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import io.wenyou.textquest.ui.common.FilterTag
import io.wenyou.textquest.ui.common.AppIcon as Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import io.wenyou.textquest.ui.common.AppText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import io.wenyou.textquest.WenYouApp
import io.wenyou.textquest.data.model.CharacterData
import io.wenyou.textquest.data.model.SexualOrientation
import io.wenyou.textquest.data.model.Story
import io.wenyou.textquest.ui.HubScaffold
import io.wenyou.textquest.ui.R
import io.wenyou.textquest.ui.common.EmojiBadge
import io.wenyou.textquest.ui.common.Pill
import io.wenyou.textquest.ui.common.QrCode
import io.wenyou.textquest.ui.common.TonalCard
import io.wenyou.textquest.ui.theme.avatarColor
import io.wenyou.textquest.ui.vm.LibraryViewModel
import io.wenyou.textquest.ui.vm.Vms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharactersScreen(container: WenYouApp.AppContainer, nav: NavHostController) {
    val vm: LibraryViewModel = viewModel(factory = Vms.factory { LibraryViewModel(container) })
    val characters by vm.characters.collectAsStateWithLifecycle()
    val stories by vm.stories.collectAsStateWithLifecycle()
    val totalCharacters by vm.totalCharacters.collectAsStateWithLifecycle()
    val filters by vm.filters.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<CharacterData?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(characters, query) {
        characters.filter { c -> query.isBlank() || listOf(c.name, c.tagline, c.personality).any { it.contains(query.trim(), ignoreCase = true) } }
    }
    var sharePicker by remember { mutableStateOf<CharacterData?>(null) }
    var shareCodeChar by remember { mutableStateOf<CharacterData?>(null) }
    var shareQrChar by remember { mutableStateOf<CharacterData?>(null) }
    var importPicker by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf(false) }
    val context = platformContext()
    val scope = rememberCoroutineScope()
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
                title = { Text("角色") },
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { SearchField(query, { query = it }, "搜索角色名、身份或性格", Modifier.testTag("character-search")) }
                item {
                    OrientationFilterRow(
                        selected = filters.orientationFilter,
                        onSelect = { vm.setOrientationFilter(it) }
                    )
                }
                if (characters.isNotEmpty() && shown.isEmpty()) {
                    item {
                        CharacterEmptyState(title = "没有找到「${query.trim()}」", body = "换个关键词试试，或清除搜索。",
                            showReset = true, onReset = { query = "" })
                    }
                } else if (characters.isEmpty()) {
                    item {
                        CharacterEmptyState(
                            title = if (totalCharacters > 0) "该分类下暂无角色" else "还没有角色",
                            body = if (totalCharacters > 0) "试试切换上方性取向，或清除筛选查看全部。" else "性格、说话方式与背景会注入 AI；分支剧本也可直接引用角色来展示台词。",
                            showReset = totalCharacters > 0,
                            onReset = { vm.setOrientationFilter(null) }
                        )
                    }
                } else {
                    items(shown, key = { it.id }) { c ->
                        CharacterCard(c, stories.filter { c.id in it.characterIds },
                            onPlay = { nav.navigate(R.play(it.id)) },
                            onEdit = { nav.navigate(R.charEdit(c.id)) },
                            onShare = { sharePicker = c },
                            onDelete = { pendingDelete = c })
                    }
                }
            }

        }
    }

    pendingDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除角色？") },
            text = { Text("「${c.name}」将被删除（已使用它的剧情不受影响）。") },
            confirmButton = {
                AppTextButton(onClick = {
                    vm.deleteCharacter(c.id)
                    pendingDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                AppTextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }

    sharePicker?.let { c ->
        SharePickDialog(
            title = c.name,
            onLink = { shareOut(context, vm.shareCodeForCharacter(c.id)) { ShareActions.sendLink(context, "角色", c.name, it) }; sharePicker = null },
            onFile = { shareOut(context, vm.shareCodeForCharacter(c.id)) { ShareActions.sendFile(context, c.name, it) }; sharePicker = null },
            onCode = { shareCodeChar = c; sharePicker = null },
            onQr = { shareQrChar = c; sharePicker = null },
            onDismiss = { sharePicker = null }
        )
    }
    shareCodeChar?.let { c ->
        ShareTextDialog(
            title = c.name,
            code = vm.shareCodeForCharacter(c.id),
            onDismiss = { shareCodeChar = null }
        )
    }
    shareQrChar?.let { c ->
        ShareQrDialog(
            title = c.name,
            code = vm.shareCodeForCharacter(c.id),
            onDismiss = { shareQrChar = null }
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

/** 性取向过滤 Chip 行（全部 + 各取向）。 */
@Composable
private fun OrientationFilterRow(
    selected: SexualOrientation?,
    onSelect: (SexualOrientation?) -> Unit
) {
    val options: List<SexualOrientation?> = listOf(null) + SexualOrientation.entries
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
    ) {
        options.forEachIndexed { index, opt ->
            FilterTag(
                accentIndex = index,
                selected = opt == selected,
                onClick = { onSelect(opt) },
                label = opt?.label ?: "全部"
            )
        }
    }
}

/** 全库为空 / 性取向筛选后无内容 的占位与「清除筛选」入口。 */
@Composable
private fun CharacterEmptyState(title: String, body: String, showReset: Boolean, onReset: () -> Unit) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharacterCard(
    c: CharacterData,
    stories: List<Story>,
    onPlay: (Story) -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBadge(c.emoji, avatarColor(c.colorIndex), size = 54.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f).padding(top = 2.dp)) {
                    io.wenyou.textquest.ui.common.RawText(c.name, style = MaterialTheme.typography.titleLarge, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                    if (c.tagline.isNotBlank())
                        io.wenyou.textquest.ui.common.RawText(c.tagline, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    if (c.personality.isNotBlank())
                        Text("性格：${c.personality}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                // Same ⋯ menu as story cards, so delete is never one stray tap away.
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(AppIcons.MoreVert, "更多", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("编辑") }, onClick = { menuOpen = false; onEdit() })
                        DropdownMenuItem(text = { Text("分享") }, onClick = { menuOpen = false; onShare() })
                        DropdownMenuItem(text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                            onClick = { menuOpen = false; onDelete() })
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (c.orientation != SexualOrientation.UNKNOWN) Pill(c.orientation.label, container = MaterialTheme.colorScheme.secondaryContainer)
                if (c.lgbt) Pill("LGBT", container = MaterialTheme.colorScheme.tertiaryContainer, accentIndex = 1)
                if (c.adult) Pill("18+", container = MaterialTheme.colorScheme.primaryContainer, accentIndex = 2)
            }
            Spacer(Modifier.height(10.dp))
            Text("参演剧情 · ${stories.size}", style = MaterialTheme.typography.labelLarge)
            if (stories.isEmpty()) {
                Text("暂无", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    stories.forEach { story ->
                        AssistChip(
                            onClick = { onPlay(story) },
                            label = { io.wenyou.textquest.ui.common.RawText(story.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = { Icon(AppIcons.PlayArrow, "开始剧情") }
                        )
                    }
                }
            }
        }
    }
}
