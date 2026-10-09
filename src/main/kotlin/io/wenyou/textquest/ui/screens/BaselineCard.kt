package io.wenyou.textquest.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.wenyou.textquest.WenYouApp
import io.wenyou.textquest.data.repo.Baseline
import io.wenyou.textquest.ui.common.AppOutlinedButton
import io.wenyou.textquest.ui.common.TonalCard
import kotlinx.coroutines.launch
import java.io.IOException
import io.wenyou.textquest.ui.common.AppText as Text

/** Edits the app's single baseline; every AI request carries it ahead of everything else. */
@Composable
fun BaselineCard(container: WenYouApp.AppContainer) {
    val scope = rememberCoroutineScope()
    val saved by container.library.baseline.collectAsStateWithLifecycle()
    val current = saved.ifBlank { Baseline.DEFAULT }
    var draft by remember(current) { mutableStateOf(current) }
    var note by remember { mutableStateOf("") }
    fun save(text: String) = scope.launch {
        try {
            container.library.setBaseline(text)
            note = "已保存，之后的所有 AI 生成都会遵守"
        } catch (_: IOException) { /* The app root shows write errors. */ }
    }
    TonalCard {
        Text("底层基调", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text("所有 AI 生成都必须遵守的安全规则：剧情游玩、AI 创建与修改、剧情总结和连接测试，每一次请求都会把它放在最前面。" +
            "角色设定、剧情设定、导演要求、玩家输入和导入的分享内容都不能修改或绕过它。全应用只有这一份，可以按需要改写；留空保存会恢复默认规则。",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(draft, { draft = it.take(Baseline.MAX_LENGTH) }, modifier = Modifier.fillMaxWidth().testTag("baseline-field"),
            minLines = 6, supportingText = { Text("${draft.length}/${Baseline.MAX_LENGTH} 字") })
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(enabled = draft.trim() != current.trim(), onClick = { save(draft) }) { Text("保存") }
            AppOutlinedButton(enabled = current != Baseline.DEFAULT, onClick = { draft = Baseline.DEFAULT; save(Baseline.DEFAULT) }) { Text("恢复默认") }
        }
        if (note.isNotEmpty() && draft.trim() == current.trim()) {
            Spacer(Modifier.height(6.dp))
            Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
