package io.wenyou.textquest.ui.screens
import io.wenyou.textquest.data.model.isAutoSaveName
import io.wenyou.textquest.ui.common.AppIcons
import io.wenyou.textquest.ui.theme.distributedAccent
import io.wenyou.textquest.ui.theme.accentForeground
import io.wenyou.textquest.ui.theme.LocalAccentPalette

import androidx.compose.material3.ButtonDefaults


import io.wenyou.textquest.ui.common.AppTextButton

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import io.wenyou.textquest.ui.common.AppIcon as Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import io.wenyou.textquest.ui.common.AppText as Text
import androidx.compose.material3.HorizontalDivider
import io.wenyou.textquest.platform.appName
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.wenyou.textquest.ui.theme.WenYouTheme
import io.wenyou.textquest.ui.theme.ThemeMode
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import io.wenyou.textquest.WenYouApp
import io.wenyou.textquest.data.model.SaveSlot
import io.wenyou.textquest.ui.HubScaffold
import io.wenyou.textquest.ui.R
import io.wenyou.textquest.ui.common.EmojiBadge
import io.wenyou.textquest.ui.common.Pill
import io.wenyou.textquest.ui.common.TonalCard
import io.wenyou.textquest.ui.common.SectionHeader
import io.wenyou.textquest.ui.theme.avatarColor
import io.wenyou.textquest.ui.vm.HomeCard
import io.wenyou.textquest.ui.vm.LibraryViewModel
import io.wenyou.textquest.ui.vm.Vms

@Composable
fun HomeScreen(container: WenYouApp.AppContainer, nav: NavHostController) {
    val vm: LibraryViewModel = viewModel(factory = Vms.factory { LibraryViewModel(container) })
    val cards by vm.homeCards.collectAsStateWithLifecycle()
    val stories by vm.stories.collectAsStateWithLifecycle()
    val providers by vm.providers.collectAsStateWithLifecycle()
    var achievementsOpen by rememberSaveable { mutableStateOf(false) }
    val achievements by container.library.achievements.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<HomeCard?>(null) }

    val current = cards.firstOrNull { it.story != null }
    val otherSaves = cards.filter { it !== current }

    HubScaffold(topBar = {}, nav = nav) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 24.dp, bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                HomeWelcome(
                    title = appName(),
                    journeyTitle = current?.story?.title ?: stories.firstOrNull()?.title,
                    hasSave = current != null,
                    onContinue = {
                        if (current != null) nav.navigate(R.play(current.slot.state.storyId, current.slot.id))
                        else if (stories.isNotEmpty()) nav.navigate(R.play(stories.first().id))
                        else nav.navigate(R.STORIES)
                    }
                )
            }

            if (providers.isEmpty()) {
                item {
                    MissingProviderCard(onClick = { nav.navigate(R.PROVIDERS) })
                }
            }

            item {
                AppTextButton(onClick = { achievementsOpen = true }) {
                    Text("成就馆 · ${achievements.count { it.unlockedAt > 0L }} / ${io.wenyou.textquest.data.engine.Achievement.entries.size}")
                }
            }

            if (cards.isEmpty()) {
                item {
                    Text("暂无存档，开始剧情后可保存进度。", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
                }
            } else if (otherSaves.isNotEmpty()) {
                item { SectionHeader("其他存档") }
                itemsIndexed(otherSaves, key = { _, card -> card.slot.id }) { index, card ->
                    ContinueCard(
                        card,
                        accentIndex = index + 3,
                        onClick = { nav.navigate(R.play(card.slot.state.storyId, card.slot.id)) },
                        onDelete = { pendingDelete = card }
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        SectionHeader("剧情库 · ${stories.size}", Modifier.weight(1f))
                        AppTextButton(onClick = { nav.navigate(R.STORIES) }) { Text("全部剧情") }
                    }
                    if (stories.isEmpty()) {
                        Text("点击底栏「创建」开始创作。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp))
                    }
                    stories.take(3).forEachIndexed { index, s ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        MiniStoryRow(
                            title = s.title,
                            subtitle = s.subtitle,
                            emoji = s.coverEmoji,
                            color = distributedAccent(index + 3, avatarColor(s.colorIndex)),
                            onClick = { nav.navigate(R.storyEdit(s.id)) }
                        )
                    }
                }
            }
        }
    }

    if (achievementsOpen) AchievementsDialog(container.library, onDismiss = { achievementsOpen = false })

    pendingDelete?.let { card ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除存档？") },
            text = { Text("「${card.story?.title ?: card.slot.name}」的这份存档（${card.stepText}）将无法恢复。") },
            confirmButton = {
                AppTextButton(onClick = {
                    vm.deleteSave(card.slot.id)
                    pendingDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                AppTextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun HomeWelcome(
    title: String, journeyTitle: String?, hasSave: Boolean,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
            io.wenyou.textquest.ui.common.RawText(title, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text("让你的故事继续", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TonalCard {
            io.wenyou.textquest.ui.common.AppText(if (hasSave) "正在续写" else "故事，从这里开始", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            io.wenyou.textquest.ui.common.AppText(journeyTitle ?: "开启第一段旅程", style = MaterialTheme.typography.headlineSmall,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onContinue, colors = ButtonDefaults.buttonColors(
                containerColor = distributedAccent(0, MaterialTheme.colorScheme.primary),
                contentColor = if (LocalAccentPalette.current.isNotEmpty()) accentForeground(distributedAccent(0, MaterialTheme.colorScheme.primary)) else MaterialTheme.colorScheme.onPrimary), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(AppIcons.PlayArrow, null)
                Spacer(Modifier.width(6.dp))
                io.wenyou.textquest.ui.common.AppText(if (hasSave) "继续旅程" else "开始剧情")
            }
        }
    }
}

@Composable
private fun MissingProviderCard(onClick: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(AppIcons.Build, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("还没有接入 AI 服务", style = MaterialTheme.typography.titleMedium)
                Text("点此配置 AI；分支剧本可离线游玩。",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContinueCard(card: HomeCard, accentIndex: Int, onClick: () -> Unit, onDelete: () -> Unit) {
    val story = card.story
    val color = distributedAccent(accentIndex, avatarColor(story?.colorIndex ?: 0))
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(52.dp).height(52.dp)
                    .clickable(onClick = onClick)
            ) {
                EmojiBadge(story?.coverEmoji ?: "📖", color, fontSize = 24.sp, size = 52.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).clickable(onClick = onClick)) {
                val custom = card.slot.name.takeUnless(::isAutoSaveName)
                io.wenyou.textquest.ui.common.RawText(custom ?: story?.title ?: card.slot.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (custom != null || story == null) io.wenyou.textquest.ui.common.AppText(story?.title ?: "（剧情已删除）",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Pill(card.stepText)
                    if (story?.mode != null && story.mode.label.isNotEmpty()) {
                        Pill(story.mode.label, container = MaterialTheme.colorScheme.tertiaryContainer, accentIndex = accentIndex)
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(AppIcons.Delete, "删除", tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun MiniStoryRow(
    title: String, subtitle: String, emoji: String, color: Color,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EmojiBadge(emoji, if (LocalAccentPalette.current.isNotEmpty()) color else color.copy(alpha = 0.35f), size = 40.dp, fontSize = 20.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            io.wenyou.textquest.ui.common.RawText(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            if (subtitle.isNotBlank())
                io.wenyou.textquest.ui.common.RawText(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
