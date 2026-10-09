package io.wenyou.textquest.ui.screens
import io.wenyou.textquest.ui.common.AppIcons
import io.wenyou.textquest.ui.theme.distributedAccent
import io.wenyou.textquest.ui.theme.accentForeground
import io.wenyou.textquest.ui.theme.LocalAccentPalette

import androidx.compose.material3.FilterChipDefaults


import io.wenyou.textquest.ui.theme.readableAccent

import io.wenyou.textquest.ui.common.AppTextButton

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import io.wenyou.textquest.ui.common.AppIcon as Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import io.wenyou.textquest.ui.common.AppText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import io.wenyou.textquest.WenYouApp
import io.wenyou.textquest.data.model.ChoiceData
import io.wenyou.textquest.data.model.CompareOp
import io.wenyou.textquest.data.model.Cond
import io.wenyou.textquest.data.model.CondType
import io.wenyou.textquest.data.model.Effect
import io.wenyou.textquest.data.model.EffectType
import io.wenyou.textquest.data.model.NodeKind
import io.wenyou.textquest.data.model.StoryMode
import io.wenyou.textquest.data.model.StoryNode
import io.wenyou.textquest.ui.common.StoryBranchTreeDialog
import kotlinx.coroutines.launch
import io.wenyou.textquest.ui.common.AppDropdown
import io.wenyou.textquest.ui.common.AppField
import io.wenyou.textquest.ui.common.ColorDots
import io.wenyou.textquest.ui.common.Pill
import io.wenyou.textquest.ui.common.SectionHeader
import io.wenyou.textquest.ui.common.SwitchRow
import io.wenyou.textquest.ui.common.TonalCard
import io.wenyou.textquest.ui.theme.AvatarPalette
import io.wenyou.textquest.ui.vm.StoryEditorViewModel
import io.wenyou.textquest.ui.vm.Vms

private val KIND_META = mapOf(
    NodeKind.NARRATION to "📖",
    NodeKind.AI to "✨",
    NodeKind.ENDING to "🏁"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StoryEditScreen(container: WenYouApp.AppContainer, nav: NavHostController, storyId: String) {
    val vm: StoryEditorViewModel = viewModel(
        factory = Vms.factory { StoryEditorViewModel(if (storyId == "new") null else storyId, container) }
    )
    val ui by vm.ui.collectAsStateWithLifecycle()
    val story = ui.story
    var revisionOpen by remember { mutableStateOf(false) }
    var treeOpen by remember { mutableStateOf(false) }
    val progress by container.library.progress.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    if (treeOpen && story != null) StoryBranchTreeDialog(story,
        onEdit = { id ->
            vm.select(id)
            treeOpen = false
            scope.launch { listState.animateScrollToItem((listState.layoutInfo.totalItemsCount - 3).coerceAtLeast(0)) }
        }, onDismiss = { treeOpen = false },
        visited = progress.firstOrNull { it.storyId == story.id }?.visitedNodes.orEmpty())

    if (revisionOpen && story != null) AiRevisionDialog(container,
        io.wenyou.textquest.data.model.AppBundle(stories = listOf(story),
            characters = ui.characters.filter { it.id in story.characterIds }),
        onApply = vm::applyRevision, onDismiss = { revisionOpen = false })

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { io.wenyou.textquest.ui.common.RawText(story?.title?.ifBlank { "未命名剧情" } ?: "剧情编辑器", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { nav.navigateUp() }) {
                        Icon(AppIcons.ArrowBack, "返回")
                    }
                },
                actions = {
                    AppTextButton(onClick = { treeOpen = true }, enabled = story != null) { Text("分支图") }
                    IconButton(onClick = { vm.save() }) {
                        Icon(AppIcons.Check, "保存剧情")
                    }
                }
            )
        }
    ) { padding ->
        if (story == null) {
            Text("剧情不存在", Modifier.padding(padding))
            return@Scaffold
        }
        val chars = ui.characters
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (ui.message.isNotBlank()) {
                item {
                    TonalCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                        io.wenyou.textquest.ui.common.RawText(ui.message, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }

            item { AppTextButton(onClick = { revisionOpen = true }) { Text("一句话修改") } }
            item { SectionHeader("基本信息") }
            item {
                TonalCard {
                    AppField(value = story.title, onValueChange = { vm.setTitle(it) }, label = "标题", singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    AppField(value = story.subtitle, onValueChange = { vm.setSubtitle(it) }, label = "一句话简介", singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppField(value = story.coverEmoji, onValueChange = { vm.setEmoji(it.take(4)) },
                            label = "封面 Emoji", singleLine = true, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(10.dp))
                        AppField(value = story.genre, onValueChange = { vm.setGenre(it) },
                            label = "类型标签", singleLine = true, modifier = Modifier.weight(1.4f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("剧情封面色", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    ColorDots(colors = AvatarPalette, selected = story.colorIndex, onSelect = { vm.setColor(it) })
                    Spacer(Modifier.height(10.dp))
                    AppDropdown(
                        label = "运行模式",
                        options = StoryMode.entries.map { it.label to it },
                        selected = story.mode,
                        onSelect = { vm.setMode(it) }
                    )
                    if (story.mode == StoryMode.AI_DIRECTOR) {
                        Spacer(Modifier.height(6.dp))
                        Text("AI 导演模式：开场后由 AI 自由接续玩家输入；下面的「世界观/导演要求」尤其重要。",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.readableAccent(MaterialTheme.colorScheme.tertiary))
                    }
                    Spacer(Modifier.height(10.dp))
                    AppDropdown(
                        label = "起始节点",
                        options = story.nodes.keys.sorted().map { displayNodeOption(it, vm) to it },
                        selected = story.startNodeId,
                        onSelect = { vm.setStartNode(it) }
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("内容分类", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    SwitchRow(
                        title = "成人向内容（18+）",
                        subtitle = "标记后归入「18+」分类，并受「成人内容」开关约束",
                        checked = story.adult,
                        onCheckedChange = { vm.setAdult(it) }
                    )
                    SwitchRow(
                        title = "LGBT 向内容",
                        subtitle = "标记后归入「LGBT」分类，并受「内容开关」约束",
                        checked = story.lgbt,
                        onCheckedChange = { vm.setLgbt(it) }
                    )
                }
            }

            item { SectionHeader("AI 叙事设定（场景生成/导演模式共用）") }
            item {
                TonalCard {
                    AppField(value = story.ai.worldSummary, onValueChange = { vm.setWorld(it) },
                        label = "世界观 / 故事大纲", minLines = 3,
                        placeholder = "描述世界、初始局面与核心悬念，越具体 AI 越稳。")
                    Spacer(Modifier.height(8.dp))
                    AppField(value = story.ai.tone, onValueChange = { vm.setTone(it) },
                        label = "叙事风格", minLines = 2,
                        placeholder = "例如：克制的悬疑感；多用对话推进；雨声氛围。")
                    Spacer(Modifier.height(8.dp))
                    AppField(value = story.ai.directorExtra, onValueChange = { vm.setDirectorExtra(it) },
                        label = "额外导演要求（AI 导演模式）", minLines = 2)
                }
            }

            item { SectionHeader("登场角色（可选）") }
            item {
                TonalCard {
                    if (chars.isEmpty()) {
                        Text("还没有角色。先去「角色」页创建，或在纯分支剧本中把角色名直接写进文本。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            chars.forEachIndexed { index, c ->
                                val checked = c.id in story.characterIds
                                FilterChip(
                                    selected = checked,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = distributedAccent(index, MaterialTheme.colorScheme.secondaryContainer),
                                        selectedLabelColor = if (LocalAccentPalette.current.isNotEmpty()) accentForeground(distributedAccent(index, MaterialTheme.colorScheme.secondaryContainer)) else MaterialTheme.colorScheme.onSecondaryContainer),
                                    onClick = {
                                        val ids = if (checked) story.characterIds - c.id
                                        else story.characterIds + c.id
                                        vm.setCharacterIds(ids)
                                    },
                                    label = { Text("${c.emoji} ${c.name}", maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                    modifier = Modifier.widthIn(max = 220.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { SectionHeader("初始变量与标记") }
            item {
                TonalCard {
                    VariableRows(variables = story.initialVariables,
                        onChangeAdd = { name -> vm.setInitVar(name, 0.0) },
                        onChangeValue = { name, v -> vm.setInitVar(name, v) },
                        onRename = { old, new -> vm.renameInitVar(old, new) },
                        onDelete = { name -> vm.removeInitVar(name) })
                    Spacer(Modifier.height(12.dp))
                    Text("初始标记（flags）", style = MaterialTheme.typography.labelLarge)
                    FlagRows(flags = story.initialFlags,
                        onAdd = { name -> vm.toggleInitFlag(name) },
                        onRemove = { name -> vm.toggleInitFlag(name) })
                }
            }

            item { SectionHeader("节点图") }
            item {
                TonalCard {
                    story.nodes.keys.sorted().forEach { id ->
                        val node = story.nodes[id]
                        val selected = id == ui.selectedNodeId
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHighest
                            ),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                io.wenyou.textquest.ui.common.RawText(KIND_META[node?.kind] ?: "•", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    io.wenyou.textquest.ui.common.RawText(node?.title?.ifBlank { id } ?: id,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold)
                                    val preview = when (node?.kind) {
                                        NodeKind.AI -> "AI：${node.prompt.take(36)}"
                                        else -> node?.text.orEmpty().replace('\n', ' ').take(40)
                                    }
                                    if (preview.isNotBlank())
                                        io.wenyou.textquest.ui.common.RawText(preview, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                                if (!selected) {
                                    AppTextButton(onClick = { vm.select(id) }) { Text("编辑") }
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Button(onClick = { vm.addNode() }, modifier = Modifier.weight(1f)) {
                            Icon(AppIcons.Add, null); Spacer(Modifier.width(6.dp)); Text("新增节点")
                        }
                    }
                }
            }

            val node = story.nodes[ui.selectedNodeId]
            if (node != null) {
                item { SectionHeader("编辑节点：${node.title.ifBlank { node.id }}") }
                item { NodeEditor(vm = vm, node = node, allNodeIds = story.nodes.keys.sorted()) }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ---------------------------------------------------------------------------
// 节点编辑器
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodeEditor(vm: StoryEditorViewModel, node: StoryNode, allNodeIds: List<String>) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val chars = ui.characters
    TonalCard {
        AppDropdown(
            label = "节点类型",
            options = NodeKind.entries.map { "${KIND_META[it]} ${it.label}" to it },
            selected = node.kind,
            onSelect = { vm.setKind(it) }
        )
        Spacer(Modifier.height(8.dp))
        AppField(value = node.title, onValueChange = { vm.setNodeTitle(it) }, label = "节点标题（可留空）", singleLine = true)

        if (node.kind == NodeKind.AI) {
            Spacer(Modifier.height(8.dp))
            AppField(value = node.prompt, onValueChange = { vm.setPrompt(it) },
                label = "AI 生成指令（必填）", minLines = 4,
                placeholder = "告诉 AI 这一幕要发生什么、角色想试探/隐瞒什么、情绪基调……")
            Spacer(Modifier.height(8.dp))
            AppDropdown(
                label = "结束后回到主线节点（可选）",
                options = listOf("（不设置：AI 可一直续写）" to "") +
                    allNodeIds.filter { it != node.id }.map { displayNodeOption(it, vm) to it },
                selected = node.endTarget,
                onSelect = { vm.setEndTarget(it) }
            )
            Spacer(Modifier.height(6.dp))
            Text("AI 会生成正文与 2-4 个动态选项；模型输出中的 [to:节点id] 或「回到主线」按钮可结束本段。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Spacer(Modifier.height(8.dp))
            AppDropdown(
                label = "说话角色（可留空=旁白）",
                options = listOf("（旁白）" to "") +
                    chars.map { "${it.emoji} ${it.name}" to it.id },
                selected = node.speakerId,
                onSelect = { vm.setSpeaker(it) }
            )
            Spacer(Modifier.height(8.dp))
            AppField(value = node.text, onValueChange = { vm.setNodeText(it) },
                label = "正文（支持 \${变量名} 插值与换行；角色说话可写成「名字：台词」）",
                minLines = 6,
                placeholder = "深夜的咖啡馆……")
            if (node.kind == NodeKind.NARRATION) {
                Spacer(Modifier.height(8.dp))
                AppDropdown(
                    label = "自动跳转（仅当本节点没有选项时生效）",
                    options = listOf("（不自动跳转）" to "") +
                        allNodeIds.filter { it != node.id }.map { displayNodeOption(it, vm) to it },
                    selected = node.endTarget,
                    onSelect = { vm.setEndTarget(it) }
                )
            }
            if (node.kind == NodeKind.ENDING) {
                Spacer(Modifier.height(6.dp))
                Text("结局节点：显示后本局结束，可被多个选项指向。",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.readableAccent(MaterialTheme.colorScheme.tertiary))
            }
        }

        if (node.kind != NodeKind.AI && node.kind != NodeKind.ENDING) {
            Spacer(Modifier.height(10.dp))
            EffectRows(title = "进入本节点时执行的效果", effects = node.onEnter,
                onChange = { vm.setNodeEnterEffects(it) })
        }

        if (node.kind == NodeKind.NARRATION) {
            Spacer(Modifier.height(10.dp))
            Text("分支选项（${node.choices.size}）", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            node.choices.forEachIndexed { i, choice ->
                ChoiceEditor(
                    index = i,
                    choice = choice,
                    allNodeIds = allNodeIds,
                    nodeId = node.id,
                    vm = vm
                )
            }
            AppTextButton(onClick = { vm.addChoice() }) {
                Icon(AppIcons.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("添加选项")
            }
        }

        Spacer(Modifier.height(10.dp))
        AppTextButton(onClick = { vm.removeNode(node.id) },
            enabled = vm.ui.value.story?.nodes?.size ?: 0 > 1) {
            Icon(AppIcons.Delete, null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(6.dp))
            Text("删除该节点", color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun displayNodeOption(id: String, vm: StoryEditorViewModel): String {
    val title = vm.ui.value.story?.nodes?.get(id)?.title.orEmpty()
    return if (title.isBlank()) id else "$id · $title"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceEditor(
    index: Int,
    choice: ChoiceData,
    allNodeIds: List<String>,
    nodeId: String,
    vm: StoryEditorViewModel
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill("选项 ${index + 1}")
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { vm.removeChoice(index) }) {
                    Icon(AppIcons.Close, "删除选项", tint = MaterialTheme.colorScheme.outline)
                }
            }
            AppField(
                value = choice.text,
                onValueChange = { newText ->
                    vm.updateChoice(index) { c -> c.copy(text = newText) }
                },
                label = "选项文案", minLines = 2,
                placeholder = "「推开那扇门……」"
            )
            Spacer(Modifier.height(8.dp))
            AppDropdown(
                label = "前往节点",
                options = listOf("（停留：回到本节点）" to "@self") +
                    allNodeIds.filter { it != nodeId }.map { displayNodeOption(it, vm) to it },
                selected = choice.next.ifBlank { "@self" },
                onSelect = { vm.updateChoice(index) { c -> c.copy(next = it) } }
            )
            Spacer(Modifier.height(4.dp))
            CondRows(title = "显示条件（全部满足才出现）", conds = choice.conditions,
                onChange = { vm.setChoiceConditions(index, it) })
            EffectRows(title = "选择后执行的效果", effects = choice.effects,
                onChange = { vm.setChoiceEffects(index, it) })
        }
    }
}

// ---------------------------------------------------------------------------
// 条件 / 效果行编辑器
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CondRows(title: String, conds: List<Cond>, onChange: (List<Cond>) -> Unit) {
    io.wenyou.textquest.ui.common.RawText(title, style = MaterialTheme.typography.labelLarge)
    conds.forEachIndexed { i, cond ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppDropdown(
                label = "类型",
                options = CondType.entries.map { it.label to it },
                selected = cond.type,
                onSelect = { onChange(conds.replace(i, cond.copy(type = it))) },
                modifier = Modifier.weight(1.1f)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = cond.name,
                onValueChange = { onChange(conds.replace(i, cond.copy(name = it))) },
                label = { io.wenyou.textquest.ui.common.RawText(if (cond.type == CondType.VAR) "变量名" else "标记名") },
                singleLine = true,
                modifier = Modifier.weight(1.5f)
            )
            IconButton(onClick = { onChange(conds.minusIndex(i)) }) {
                Icon(AppIcons.Close, "删除条件", tint = MaterialTheme.colorScheme.outline)
            }
        }
        if (cond.type == CondType.VAR) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppDropdown(
                    label = "比较",
                    options = CompareOp.entries.map { it.label to it },
                    selected = cond.op,
                    onSelect = { onChange(conds.replace(i, cond.copy(op = it))) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                NumField(
                    value = cond.value,
                    onChange = { onChange(conds.replace(i, cond.copy(value = it))) },
                    label = "阈值",
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(48.dp))
            }
        }
    }
    AppTextButton(onClick = { onChange(conds + Cond(type = CondType.VAR, name = "intimacy", op = CompareOp.GTE, value = 1.0)) }) {
        Icon(AppIcons.Add, null); Spacer(Modifier.width(4.dp)); Text("添加条件")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EffectRows(title: String, effects: List<Effect>, onChange: (List<Effect>) -> Unit) {
    io.wenyou.textquest.ui.common.RawText(title, style = MaterialTheme.typography.labelLarge)
    effects.forEachIndexed { i, effect ->
        Column(Modifier.padding(vertical = 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppDropdown(
                    label = "效果",
                    options = EffectType.entries.map { it.label to it },
                    selected = effect.type,
                    onSelect = { onChange(effects.replace(i, effect.copy(type = it))) },
                    modifier = Modifier.weight(1.4f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = effect.name,
                    onValueChange = { onChange(effects.replace(i, effect.copy(name = it))) },
                    label = { io.wenyou.textquest.ui.common.RawText(if (effect.type == EffectType.ROLL) "结果变量" else "变量/标记名") },
                    singleLine = true,
                    modifier = Modifier.weight(1.4f)
                )
                IconButton(onClick = { onChange(effects.minusIndex(i)) }) {
                    Icon(AppIcons.Close, "删除效果", tint = MaterialTheme.colorScheme.outline)
                }
            }
            when (effect.type) {
                EffectType.SET_VAR, EffectType.ADD_VAR -> NumField(
                    value = effect.value,
                    onChange = { onChange(effects.replace(i, effect.copy(value = it))) },
                    label = if (effect.type == EffectType.ADD_VAR) "增量" else "值",
                    modifier = Modifier.fillMaxWidth()
                )
                EffectType.RANDOM_VAR -> Row {
                    NumField(value = effect.from,
                        onChange = { onChange(effects.replace(i, effect.copy(from = it))) },
                        label = "最小值", modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    NumField(value = effect.to,
                        onChange = { onChange(effects.replace(i, effect.copy(to = it))) },
                        label = "最大值", modifier = Modifier.weight(1f))
                }
                EffectType.ROLL -> NumField(value = effect.to,
                    onChange = { onChange(effects.replace(i, effect.copy(to = it))) },
                    label = "骰子面数 dN（示例 6）", modifier = Modifier.fillMaxWidth())
                EffectType.SET_FLAG, EffectType.CLEAR_FLAG -> Unit
            }
        }
    }
    AppTextButton(onClick = { onChange(effects + Effect(type = EffectType.SET_FLAG, name = "")) }) {
        Icon(AppIcons.Add, null); Spacer(Modifier.width(4.dp)); Text("添加效果")
    }
}

// ---------------------------------------------------------------------------
// 变量 / 标记编辑
// ---------------------------------------------------------------------------

@Composable
private fun VariableRows(
    variables: Map<String, Double>,
    onChangeAdd: (String) -> Unit,
    onChangeValue: (String, Double) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    Text("初始数值变量", style = MaterialTheme.typography.labelLarge)
    var newName by remember { mutableStateOf("") }
    variables.forEach { (name, v) ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = name,
                onValueChange = { raw -> onRename(name, raw) },
                label = { Text("变量名") },
                singleLine = true,
                modifier = Modifier.weight(1.4f)
            )
            Spacer(Modifier.width(8.dp))
            NumField(value = v, onChange = { onChangeValue(name, it) },
                label = "初始值", modifier = Modifier.weight(1f))
            IconButton(onClick = { onDelete(name) }) {
                Icon(AppIcons.Close, "删除", tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            label = { Text("新变量名") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = {
            val n = newName.trim()
            if (n.isNotEmpty()) {
                onChangeAdd(n)
                newName = ""
            }
        }) { Icon(AppIcons.Add, "添加变量") }
    }
}

@Composable
private fun FlagRows(flags: Set<String>, onAdd: (String) -> Unit, onRemove: (String) -> Unit) {
    var newFlag by remember { mutableStateOf("") }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        flags.forEach { f ->
            AssistChip(
                onClick = { onRemove(f) },
                label = { io.wenyou.textquest.ui.common.RawText(f) },
                trailingIcon = { Icon(AppIcons.Close, null, Modifier.width(16.dp).height(16.dp)) }
            )
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = newFlag,
            onValueChange = { newFlag = it },
            label = { Text("新标记名（点添加；再次点名称可从列表移除）") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = {
            val n = newFlag.trim()
            if (n.isNotEmpty()) {
                onAdd(n)
                newFlag = ""
            }
        }) { Icon(AppIcons.Add, "添加标记") }
    }
}

// ---------------------------------------------------------------------------
// 小工具
// ---------------------------------------------------------------------------

@Composable
private fun NumField(
    value: Double,
    onChange: (Double) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var draft by remember(value) { mutableStateOf(formatNum(value)) }
    OutlinedTextField(
        value = draft,
        onValueChange = { raw ->
            draft = raw
            raw.toDoubleOrNull()?.let(onChange)
        },
        label = { io.wenyou.textquest.ui.common.RawText(label) },
        singleLine = true,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

private fun formatNum(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()

private fun <T> List<T>.replace(index: Int, item: T): List<T> =
    mapIndexed { i, t -> if (i == index) item else t }

private fun <T> List<T>.minusIndex(index: Int): List<T> =
    filterIndexed { i, _ -> i != index }
