package io.wenyou.textquest.ui.screens

import io.wenyou.textquest.ui.common.AppIcons
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import io.wenyou.textquest.ui.common.AppIcon as Icon
import io.wenyou.textquest.ui.common.AppText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.wenyou.textquest.data.engine.Achievement
import io.wenyou.textquest.ui.common.EasterEggTitle
import io.wenyou.textquest.data.model.AchievementRecord
import io.wenyou.textquest.data.repo.LocalLibrary
import java.text.DateFormat
import java.util.Date

@Composable
fun AchievementsDialog(library: LocalLibrary, onDismiss: () -> Unit) {
    val records by library.achievements.collectAsStateWithLifecycle()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AchievementsContent(records, onDismiss)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AchievementsContent(records: List<AchievementRecord>, onDismiss: () -> Unit) {
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = {
            val secret = "🏅 隐藏奖杯：好奇心万岁\n你找到了奖杯柜后的秘密隔间。里面没有积分，只有一句话：愿你永远对下一个故事保持好奇。"
            EasterEggTitle("成就馆", MaterialTheme.typography.titleLarge, secret, secret)
        },
            navigationIcon = { IconButton(onClick = onDismiss) { Icon(AppIcons.Close, "关闭成就馆") } }) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("achievements-list"), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("已解锁 ${records.count { it.unlockedAt > 0L }} / ${Achievement.entries.size}", style = MaterialTheme.typography.titleLarge)
                Text("进度保存在本机，支持整包备份；重开和删除存档不会撤销成就。", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(Achievement.entries, key = { it.name }) { achievement ->
                val record = records.firstOrNull { it.id == achievement.name }
                val progress = (record?.progress ?: 0).coerceIn(0, achievement.target)
                val unlocked = (record?.unlockedAt ?: 0L) > 0L
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${if (unlocked) "🏆" else "🔒"} ${achievement.title}", style = MaterialTheme.typography.titleMedium)
                        io.wenyou.textquest.ui.common.AppText(achievement.description, style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(progress = { progress.toFloat() / achievement.target }, modifier = Modifier.fillMaxWidth())
                        io.wenyou.textquest.ui.common.AppText(if (unlocked) "已解锁 · ${DateFormat.getDateInstance().format(Date(record!!.unlockedAt))}"
                            else "未解锁 · $progress / ${achievement.target}", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
