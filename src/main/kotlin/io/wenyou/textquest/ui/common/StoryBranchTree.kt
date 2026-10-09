package io.wenyou.textquest.ui.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import io.wenyou.textquest.ui.common.AppIcon as Icon
import io.wenyou.textquest.ui.common.AppText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.wenyou.textquest.data.model.*

internal enum class BranchKind { NODE, CYCLE, MERGE, MISSING, DYNAMIC }
internal data class BranchRow(val nodeId: String?, val depth: Int, val label: String, val kind: BranchKind,
    val last: Boolean = true, val rails: Long = 0, val expandable: Boolean = false)

/** Each node expands once. Iterative traversal handles deeply nested, cyclic and converging graphs. */
internal fun storyBranches(story: Story): List<BranchRow> {
    data class Visit(val id: String?, val depth: Int, val label: String, val last: Boolean = true, val rails: Long = 0, val exit: Boolean = false)
    val rows = mutableListOf<BranchRow>()
    val visited = mutableSetOf<String>()
    val ancestors = mutableSetOf<String>()
    fun children(id: String, node: StoryNode): List<Pair<String?, String>> = when {
        story.mode == StoryMode.AI_DIRECTOR -> listOf(null to "AI 导演自由续写（动态分支）")
        node.kind == NodeKind.ENDING -> emptyList()
        node.kind == NodeKind.AI -> listOf(null to "AI 生成选项（动态分支）") +
            if (node.endTarget.isNotBlank() && node.endTarget != id) listOf(node.endTarget to "回到主线") else emptyList()
        else -> node.choices.map { choice ->
            val target = choice.next.ifBlank { id }.let { if (it == "@self") id else it }
            target to (choice.text.ifBlank { "未命名选项" } + if (choice.conditions.isNotEmpty()) " · 有条件" else "")
        } + if (node.endTarget.isNotBlank() && node.endTarget != id && node.choices.all { it.conditions.isNotEmpty() })
            listOf(node.endTarget to if (node.choices.isEmpty()) "自动跳转" else "无可用选项时自动跳转") else emptyList()
    }
    fun walk(root: String, label: String) {
        val stack = ArrayDeque<Visit>()
        stack.addLast(Visit(root, 0, label))
        while (stack.isNotEmpty()) {
            val v = stack.removeLast()
            val id = v.id
            if (v.exit) { ancestors.remove(id); continue }
            val node = story.nodes[id]
            val kind = when {
                id == null -> BranchKind.DYNAMIC
                node == null -> BranchKind.MISSING
                id in ancestors -> BranchKind.CYCLE
                id in visited -> BranchKind.MERGE
                else -> BranchKind.NODE
            }
            val edges = if (kind == BranchKind.NODE) children(id!!, node!!) else emptyList()
            rows += BranchRow(id, v.depth, v.label, kind, v.last, v.rails, edges.isNotEmpty())
            if (kind != BranchKind.NODE) continue
            visited += id!!; ancestors += id
            stack.addLast(v.copy(exit = true))
            // ponytail: connector rails stop at 12 visible levels; deeper nodes retain a depth label.
            val rails = v.rails or if (!v.last && v.depth in 1..12) (1L shl v.depth) else 0L
            for (i in edges.indices.reversed()) {
                stack.addLast(Visit(edges[i].first, v.depth + 1, edges[i].second, i == edges.lastIndex, rails))
            }
        }
    }
    walk(story.startNodeId, "起点")
    for (id in story.nodes.keys) if (id !in visited) walk(id, "未从起点连接")
    return rows
}

internal fun visibleBranches(rows: List<BranchRow>, collapsed: Set<String>): List<BranchRow> {
    var hiddenBelow: Int? = null
    return rows.filter { row ->
        if (hiddenBelow != null && row.depth > hiddenBelow!!) false
        else {
            hiddenBelow = if (row.kind == BranchKind.NODE && row.nodeId in collapsed) row.depth else null
            true
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/**
 * [onEdit] null opens a read-only map for play, highlighting and scrolling to [currentNodeId].
 * [visited] marks nodes the player has reached in any journey and unlocked endings.
 */
fun StoryBranchTreeDialog(story: Story, onEdit: ((String) -> Unit)?, onDismiss: () -> Unit, currentNodeId: String? = null,
    visited: Set<String> = emptySet()) {
    val rows = remember(story) { storyBranches(story) }
    var collapsed by remember(story.id) { mutableStateOf(emptySet<String>()) }
    val visible = remember(rows, collapsed) { visibleBranches(rows, collapsed) }
    val listState = rememberLazyListState(visible.indexOfFirst { it.kind == BranchKind.NODE && it.nodeId == currentNodeId }.coerceAtLeast(0))
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), topBar = {
            CenterAlignedTopAppBar(title = { Text("剧情分支图") }, navigationIcon = {
                AppTextButton(onClick = onDismiss) { Text("返回") }
            }, actions = { AppTextButton(onClick = { collapsed = emptySet() }) { Text("全部展开") } })
        }) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                io.wenyou.textquest.ui.common.RawText(story.title, Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleMedium)
                Text(if (onEdit != null) "点击节点编辑；条件分支展示所有可能出口。左右滑动查看深层分支。"
                    else "已标出当前位置；条件分支展示所有可能出口。左右滑动查看深层分支。",
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall)
                val reached = story.nodes.keys.count { it in visited }
                if (reached > 0) {
                    val endings = story.nodes.values.filter { it.kind == NodeKind.ENDING }
                    Text("已到达 $reached / ${story.nodes.size} 个节点 · 已解锁结局 ${endings.count { it.id in visited }} / ${endings.size}",
                        Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp).testTag("branch-progress"),
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                    LazyColumn(state = listState, modifier = Modifier.width((340 + (visible.maxOfOrNull { it.depth } ?: 0).coerceAtMost(12) * 20).dp),
                        contentPadding = PaddingValues(12.dp)) {
                        itemsIndexed(visible) { _, row ->
                            BranchNode(story, row, row.nodeId in collapsed, current = row.kind == BranchKind.NODE && row.nodeId == currentNodeId,
                                reached = row.kind == BranchKind.NODE && row.nodeId in visited,
                                onToggle = { row.nodeId?.let { collapsed = if (it in collapsed) collapsed - it else collapsed + it } },
                                onEdit = onEdit)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BranchNode(story: Story, row: BranchRow, collapsed: Boolean, current: Boolean, reached: Boolean, onToggle: () -> Unit, onEdit: ((String) -> Unit)?) {
    val node = story.nodes[row.nodeId]
    val colors = MaterialTheme.colorScheme
    val depth = row.depth.coerceAtMost(12)
    val marker = when (row.kind) {
        BranchKind.CYCLE -> "↻ 循环回到"
        BranchKind.MERGE -> "↗ 汇合至"
        BranchKind.MISSING -> "⚠ 节点不存在"
        BranchKind.DYNAMIC -> "✨ 动态生成"
        BranchKind.NODE -> node?.kind?.label.orEmpty()
    }
    Row(Modifier.fillMaxWidth().drawBehind {
        val step = 20.dp.toPx()
        for (d in 1..12) if (row.rails and (1L shl d) != 0L && d < depth)
            drawLine(colors.outlineVariant, Offset(d * step - step / 2, 0f), Offset(d * step - step / 2, size.height), 2.dp.toPx())
        if (depth > 0) {
            val x = depth * step - step / 2
            drawLine(colors.outlineVariant, Offset(x, 0f), Offset(x, if (row.last) size.height / 2 else size.height), 2.dp.toPx())
            drawLine(colors.outlineVariant, Offset(x, size.height / 2), Offset(depth * step, size.height / 2), 2.dp.toPx())
        }
    }.padding(start = (depth * 20).dp, top = 4.dp, bottom = 4.dp)) {
        val cardColors = CardDefaults.cardColors(containerColor = when {
            row.kind == BranchKind.MISSING -> colors.errorContainer
            current -> colors.primaryContainer
            else -> colors.surfaceContainerHigh
        })
        val content: @Composable ColumnScope.() -> Unit = {
            Column(Modifier.padding(12.dp)) {
                io.wenyou.textquest.ui.common.RawText(row.label, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                io.wenyou.textquest.ui.common.RawText(node?.title?.ifBlank { row.nodeId.orEmpty() } ?: row.nodeId ?: "游玩时生成",
                    style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val progress = when {
                    current -> " · 当前位置"
                    reached && node?.kind == NodeKind.ENDING -> " · 🏆 已解锁"
                    reached -> " · ✓ 已到达"
                    else -> ""
                }
                Text("$marker$progress${if (row.depth > 12) " · 第${row.depth}层" else ""}", style = MaterialTheme.typography.bodySmall,
                    color = when {
                        row.kind == BranchKind.MISSING -> colors.error
                        current -> colors.onPrimaryContainer
                        else -> colors.onSurfaceVariant
                    })
                if (row.expandable) AppTextButton(onClick = onToggle) { io.wenyou.textquest.ui.common.RawText(if (collapsed) "展开分支" else "折叠分支") }
            }
        }
        if (onEdit != null) Card(onClick = { row.nodeId?.takeIf { it in story.nodes }?.let(onEdit) },
            enabled = node != null, colors = cardColors, modifier = Modifier.width(300.dp), content = content)
        else Card(colors = cardColors, modifier = Modifier.width(300.dp), content = content)
    }
}
