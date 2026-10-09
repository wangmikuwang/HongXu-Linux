package io.wenyou.textquest.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import io.wenyou.textquest.ui.common.AppIcon as Icon
import io.wenyou.textquest.ui.common.AppText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.wenyou.textquest.data.llm.UsageTracker
import io.wenyou.textquest.data.llm.UsageRecord
import kotlinx.coroutines.delay
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date

private fun UsageRecord.label(): String = "$service · $model · $status · ${elapsedMs / 1000.0} 秒\n输入 ${tokens.input ?: "未知"} / 输出 ${tokens.output ?: "未知"} tokens · 缓存命中 ${tokens.cached} / 未命中 ${tokens.uncached ?: "未知"} / 写入 ${tokens.cacheWrite}\n" +
    (estimatedCost?.let { "估算费用 $currency ${String.format(Locale.ROOT, "%.6f", it)}" + (estimatedCostUpper?.let { upper -> "–${String.format(Locale.ROOT, "%.6f", upper)}" } ?: "") + if (pricingNote.isNotBlank()) "\n$pricingNote" else "" } ?: "费用未知（缺少用量或单价，取消/失败可能仍计费）")

@Composable
fun UsagePanel(tracker: UsageTracker, showLast: Boolean = true, showStats: Boolean = true) {
    val active by tracker.active.collectAsStateWithLifecycle()
    val records by tracker.records.collectAsStateWithLifecycle()
    val now = generationClock(active.isNotEmpty())
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        active.values.forEach { p ->
            Text("${p.phase} · ${((now - p.startNanos).coerceAtLeast(0) / 1_000_000_000)} 秒 · 已接收 ${p.characters} 字符", style = MaterialTheme.typography.bodySmall)
        }
        if (active.isEmpty() && showLast) records.lastOrNull()?.let {
            Text("最近一次请求\n${it.label()}", style = MaterialTheme.typography.bodySmall)
        }
        if (showStats) AppTextButton(onClick = { open = true }) { Text("生成用量与费用统计") }
    }
    if (open) UsageDialog(tracker) { open = false }
}

@Composable
fun UsageDialog(tracker: UsageTracker, onDismiss: () -> Unit) {
    val records by tracker.records.collectAsStateWithLifecycle()
    run {
        val error by tracker.persistenceError.collectAsStateWithLifecycle()
        AlertDialog(onDismissRequest = onDismiss, title = { Text("生成用量与费用统计") },
            text = {
                Column(Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("本机最近 ${records.size} 次请求（最多 100 次），包含连接测试。tokens 取自服务返回，字符数不是 tokens。费用按官方价格快照或手动单价估算，实际账单以服务商为准。")
                    Text("已报告输入 ${records.sumOf { it.tokens.input ?: 0 }} / 输出 ${records.sumOf { it.tokens.output ?: 0 }} tokens；${records.count { it.tokens.input == null || it.tokens.output == null }} 次用量不完整。")
                    records.filter { it.estimatedCost != null }.groupBy { it.currency }.forEach { (currency, list) ->
                        Text("已估算 ${list.size} 次：$currency ${String.format(Locale.ROOT, "%.6f", list.sumOf { it.estimatedCost!! })}" + if (list.any { it.estimatedCostUpper != null }) "–${String.format(Locale.ROOT, "%.6f", list.sumOf { it.estimatedCostUpper ?: it.estimatedCost!! })}" else "")
                    }
                    Text("${records.count { it.estimatedCost == null }} 次费用未知；未收录的模型或第三方服务，请到 AI 服务编辑页填写单价。")
                    if (error.isNotBlank()) io.wenyou.textquest.ui.common.RawText(error, color = MaterialTheme.colorScheme.error)
                    records.asReversed().forEach { r ->
                        io.wenyou.textquest.ui.common.RawText(SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(r.time)) + "\n" + r.label(), style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider()
                    }
                }
            }, confirmButton = { AppTextButton(onClick = onDismiss) { Text("关闭") } })
    }
}

/** The UI timer sleeps while the activity is stopped; generation has its own lifetime. */
@Composable
internal fun generationClock(running: Boolean, nanoTime: () -> Long = System::nanoTime): Long {
    var now by remember { mutableLongStateOf(nanoTime()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, running) {
        if (running) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { now = nanoTime(); delay(1000) }
        }
    }
    return now
}
