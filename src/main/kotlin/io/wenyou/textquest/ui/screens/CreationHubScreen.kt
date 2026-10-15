@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package io.wenyou.textquest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.wenyou.textquest.WenYouApp
import io.wenyou.textquest.ui.HubScaffold
import io.wenyou.textquest.ui.R
import io.wenyou.textquest.ui.common.AppText as Text
import io.wenyou.textquest.ui.common.AppIcon as Icon
import io.wenyou.textquest.ui.common.AppIcons
import io.wenyou.textquest.ui.common.AppOutlinedButton
import io.wenyou.textquest.ui.common.TonalCard
import androidx.compose.material3.Button

@Composable
fun CreationHubScreen(container: WenYouApp.AppContainer, nav: NavHostController) {
    var aiOpen by rememberSaveable { mutableStateOf(false) }
    HubScaffold(topBar = { CenterAlignedTopAppBar(title = { Text("创建") }) }, nav = nav) { padding ->
        CreationHubContent({ nav.navigate(R.storyEdit("new")) }, { nav.navigate(R.charEdit("new")) },
            { aiOpen = true }, Modifier.padding(padding))
    }
    if (aiOpen) CreationDialog(container, nav, { aiOpen = false })
}

@Composable
internal fun CreationHubContent(onStory: () -> Unit, onCharacter: () -> Unit, onAi: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("creation-hub"),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        io.wenyou.textquest.ui.common.SectionHeader("选择创建方式")
        TonalCard {
            Text("手动创建", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            Text("编写世界、开场和剧情分支", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            AppOutlinedButton(onClick = onStory, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("create-story")) {
                Icon(AppIcons.List, null); Spacer(Modifier.width(8.dp)); Text("新建剧情")
            }
            Spacer(Modifier.height(16.dp))
            Text("设定人物、性格和背景", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            AppOutlinedButton(onClick = onCharacter, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("create-character")) {
                Icon(AppIcons.Person, null); Spacer(Modifier.width(8.dp)); Text("新建角色")
            }
        }
        TonalCard {
            Text("AI 一句话创建", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("用一句话生成剧情与人物，保存前可预览和修改", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAi, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("create-ai")) { Text("AI 创建") }
        }
    }
}
