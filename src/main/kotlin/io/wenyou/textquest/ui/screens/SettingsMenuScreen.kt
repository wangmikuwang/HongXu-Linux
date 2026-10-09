package io.wenyou.textquest.ui.screens

import io.wenyou.textquest.ui.common.SearchField
import io.wenyou.textquest.ui.common.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.wenyou.textquest.ui.HubScaffold
import io.wenyou.textquest.ui.R
import io.wenyou.textquest.ui.common.AppText
import io.wenyou.textquest.ui.common.uiLabel
import io.wenyou.textquest.ui.theme.LocalAppearance

internal data class SettingsSection(val id: String, val title: String, val summary: String,
    val icon: ImageVector, val color: Color, val keywords: String)

internal val settingsSections = listOf(
    SettingsSection("appearance", "外观与主题", "调整界面风格、颜色、字体、大小和图标", AppIcons.Star, Color(0xFFFF416C), "液态玻璃 帧率 深色 语言 壁纸 字重 缩放"),
    SettingsSection("ai", "AI 服务与生成", "管理服务、默认模型、后台生成与通知", AppIcons.Refresh, Color(0xFF30BF60), "API 接口 密钥 模型 默认服务 通知 续航 后台 费用"),
    SettingsSection("content", "内容偏好", "管理预置剧情与人物的内容范围", AppIcons.Person, Color(0xFFFFA000), "剧情 人物 成人"),
    SettingsSection("rules", "底层基调", "所有 AI 生成都必须遵守的安全规则", AppIcons.Build, Color(0xFF2789EF), "底层基调 规则 安全"),
    SettingsSection("backup", "存储与备份", "导入、导出剧情、人物、服务与存档", AppIcons.Create, Color(0xFFAA55DC), "数据 导出 导入 迁移 恢复"),
    SettingsSection("system", "系统与关于", "检查更新、管理日志并查看应用信息", AppIcons.Info, Color(0xFF4CC5DF), "版本 更新 下载 安装 崩溃 日志 关于")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsMenuScreen(nav: NavHostController) {
    HubScaffold(topBar = {
        TopAppBar(title = { AppText("设置", fontWeight = FontWeight.SemiBold) }, navigationIcon = {
            IconButton(onClick = { nav.navigateUp() }) { Icon(AppIcons.ArrowBack, "返回") }
        })
    }, nav = nav) { padding ->
        SettingsMenuContent({ section -> nav.navigate(if (section == "appearance") R.APPEARANCE else R.settingsDetail(section)) }, Modifier.padding(padding))
    }
}

@Composable
internal fun SettingsMenuContent(onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    val language = LocalAppearance.current.language
    val visible = settingsSections.filter {
        query.isBlank() || (it.title + " " + it.summary + " " + it.keywords + " " + uiLabel(it.title, language)).contains(query.trim(), ignoreCase = true)
    }
    LazyColumn(modifier.fillMaxSize().testTag("settings-menu"), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        item {
            SearchField(query, { query = it }, "搜索设置功能", Modifier.testTag("settings-search"))
            Spacer(Modifier.height(18.dp))
        }
        items(visible, key = { it.id }) { section ->
            Row(Modifier.fillMaxWidth().testTag("settings-${section.id}").clickable { onOpen(section.id) }
                .padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(section.color, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Icon(section.icon, null, Modifier.size(27.dp), tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    AppText(section.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    AppText(section.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                Icon(AppIcons.KeyboardArrowRight, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(Modifier.padding(start = 60.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .25f))
        }
        if (visible.isEmpty()) item { AppText("没有找到相关设置", Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
