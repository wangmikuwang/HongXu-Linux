@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package io.wenyou.textquest.ui.screens

import io.wenyou.textquest.ui.common.AppIcons
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import io.wenyou.textquest.ui.common.AppIcon as Icon
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import io.wenyou.textquest.WenYouApp
import io.wenyou.textquest.data.AppearanceFiles
import io.wenyou.textquest.ui.common.AppDropdown
import io.wenyou.textquest.ui.common.AppText as Text
import io.wenyou.textquest.ui.common.SectionHeader
import io.wenyou.textquest.ui.common.TonalCard
import io.wenyou.textquest.ui.common.WallpaperPreview
import io.wenyou.textquest.ui.theme.*
import io.wenyou.textquest.platform.platformContext
import io.wenyou.textquest.platform.rememberOpenDocument
import io.wenyou.textquest.ui.theme.contrastRatio
import io.wenyou.textquest.ui.theme.parseHexColor
import io.wenyou.textquest.ui.theme.toHsv
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Wiring owns imports and persistence; AppearanceContent receives values and event callbacks. */
@Composable
fun AppearanceScreen(container: WenYouApp.AppContainer, nav: NavHostController) {
    val prefs by container.settings.state.collectAsStateWithLifecycle()
    val context = platformContext()
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("") }
    val fontPicker = rememberOpenDocument { uri ->
        if (uri != null) scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { AppearanceFiles.importFont(context, uri) } }
            result.onSuccess { (file, name) ->
                val old = container.settings.state.value.appearance.fontFile
                container.settings.updateAppearance { it.copy(fontFile = file, fontName = name) }
                withContext(Dispatchers.IO) { AppearanceFiles.removeFont(context, old) }
                message = "字体已应用；不包含的字形由系统字体补足"
            }.onFailure { message = it.message ?: "导入字体失败" }
        }
    }
    val wallpaperPicker = rememberOpenDocument { uri ->
        if (uri != null) scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { AppearanceFiles.importWallpaper(context, uri) } }
            result.onSuccess { name -> container.settings.updateAppearance { it.copy(splashWallpaper = name) }; message = "开屏壁纸已保存" }
                .onFailure { message = it.message ?: "导入图片失败" }
        }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("外观与主题") }, navigationIcon = {
            IconButton(onClick = { nav.popBackStack() }) { Icon(AppIcons.ArrowBack, "返回") }
        })
    }) { padding ->
        AppearanceContent(prefs.appearance, prefs.themeStyle, prefs.themeMode, prefs.dynamicColor, prefs.prideTheme != null,
            message,
            container.settings::updateAppearance, container.settings::setThemeStyle, container.settings::setThemeMode, container.settings::setDynamicColor,
            { fontPicker(arrayOf("*/*")) }, {
                val old = container.settings.state.value.appearance.fontFile
                container.settings.updateAppearance { it.copy(fontFile = "", fontName = "") }
                scope.launch(Dispatchers.IO) { AppearanceFiles.removeFont(context, old) }
            }, { wallpaperPicker(arrayOf("image/*")) }, Modifier.padding(padding))
    }
}

@Composable
fun AppearanceContent(prefs: AppearancePrefs, style: ThemeStyle, mode: ThemeMode, dynamic: Boolean, prideActive: Boolean,
    message: String,
    update: ((AppearancePrefs) -> AppearancePrefs) -> Unit, setStyle: (ThemeStyle) -> Unit, setMode: (ThemeMode) -> Unit,
    setDynamic: (Boolean) -> Unit, importFont: () -> Unit, resetFont: () -> Unit, importWallpaper: () -> Unit,
    modifier: Modifier = Modifier) {
    var colorTarget by remember { mutableStateOf<String?>(null) }
    var paletteExpanded by rememberSaveable { mutableStateOf(false) }
    var showWallpapers by remember { mutableStateOf(false) }
    LazyColumn(modifier.fillMaxSize().testTag("appearance-list"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (message.isNotEmpty()) item { Text(message, color = MaterialTheme.colorScheme.primary) }
        item { SectionHeader("显示模式") }
        item { TonalCard {
            Choice("界面风格", listOf("Material You" to ThemeStyle.MATERIAL, "Miuix" to ThemeStyle.MIUIX, "液态玻璃" to ThemeStyle.APPLE), style, setStyle)
            Text(when (style) { ThemeStyle.MATERIAL -> "原生 Material 控件与层次"; ThemeStyle.MIUIX -> "清晰圆角、蓝色强调与分组背景"; ThemeStyle.APPLE -> "悬浮玻璃底栏、整项胶囊与通透材质" }, style = MaterialTheme.typography.bodySmall)
            Toggle("液态玻璃", prefs.glassEnabled, { value -> update { it.copy(glassEnabled = value) } })
            Choice("列表条目样式", listOf("跟随预设" to "follow", "统一圆角" to "rounded"), prefs.listStyle, { value -> update { it.copy(listStyle = value) } })
            Choice("图标样式", listOf("单色图标" to "mono", "彩色图标" to "color"), prefs.iconStyle, { value -> update { it.copy(iconStyle = value) } })
            Choice("选项弹窗样式", listOf("跟随选项弹出" to "anchor", "居中弹窗" to "dialog"), prefs.popupStyle, { value -> update { it.copy(popupStyle = value) } })
        } }
        item { SectionHeader("主题与色彩") }
        item { TonalCard {
            Choice("主题模式", listOf("跟随系统" to ThemeMode.SYSTEM, "浅色" to ThemeMode.LIGHT, "深色" to ThemeMode.DARK), mode, setMode)
            if (mode != ThemeMode.LIGHT) Choice("深色风格", listOf("默认" to false, "纯黑" to true), prefs.amoled, { value -> update { it.copy(amoled = value) } })
            if (!prideActive) {
                val sources = listOf("品牌配色" to "brand", "自定义" to "custom")
                val source = if (prefs.colorSource == "brand" && dynamic && !prefs.glassEnabled && style != ThemeStyle.APPLE) "wallpaper" else prefs.colorSource
                Choice("主题颜色来源", sources, source.takeIf { selected -> sources.any { it.second == selected } } ?: "brand",
                    { value -> setDynamic(value == "wallpaper"); update { it.copy(colorSource = value) } })
                Text("配色预设", fontWeight = FontWeight.SemiBold)
                val dark = MaterialTheme.colorScheme.background.luminance() < .5f
                val current = MaterialTheme.colorScheme
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorPresets.forEach { preset ->
                        val colors = remember(preset, dark, prefs.colorSpec) { customColors(current, prefs.copy(seed = preset.seed, paletteStyle = preset.style), dark) }
                        val selected = prefs.colorSource == "custom" && prefs.seed == preset.seed && prefs.paletteStyle == preset.style
                        Surface(Modifier.width(76.dp).clickable { setDynamic(false); update { it.copy(seed = preset.seed, paletteStyle = preset.style, colorSource = "custom") } },
                            shape = RoundedCornerShape(16.dp), color = colors.surfaceContainerHigh,
                            border = if (selected) BorderStroke(2.dp, colors.primary) else null) {
                            Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    listOf(colors.primary, colors.secondary, colors.tertiary).forEach { Box(Modifier.size(16.dp).background(it, CircleShape)) }
                                }
                                Text(preset.name, color = colors.onSurface, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                Preference("自定义主题颜色", prefs.seed, { colorTarget = "seed" })
                Toggle("高级配色", prefs.advancedColors, { value -> update { it.copy(advancedColors = value) } })
                if (prefs.advancedColors) {
                    for ((name, key) in listOf("浅色模式" to "light", "深色模式" to "dark")) {
                        Text(name, fontWeight = FontWeight.SemiBold)
                        val defaults = if (key == "light") listOf("#F2F2F7", "#1C1C1E", "#56565E", prefs.seed) else listOf("#000000", "#F5F5F7", "#CACAD0", prefs.seed)
                        val roles = listOf("背景" to "background", "主要文字" to "text", "次要文字" to "secondary", "控件" to "control")
                        roles.forEachIndexed { index, (label, role) -> Preference(label, prefs.roleColors["${key}_$role"] ?: defaults[index], { colorTarget = "${key}_$role" }) }
                        val background = prefs.roleColors["${key}_background"] ?: defaults[0]
                        val ratios = listOf("text", "secondary").mapIndexed { index, role -> contrastRatio(parseHexColor(prefs.roleColors["${key}_$role"] ?: defaults[index + 1]), parseHexColor(background)) }
                        if (ratios.any { it < 4.5 }) Text("文字与背景对比不足，建议调整颜色", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Choice("色彩风格", listOf("柔和" to "tonal", "鲜艳" to "vibrant", "表现力" to "expressive", "中性" to "neutral", "单色" to "mono", "忠实" to "fidelity", "内容" to "content", "彩虹" to "rainbow", "果色" to "fruit"), prefs.paletteStyle,
                    { value -> setDynamic(false); update { it.copy(paletteStyle = value, colorSource = "custom") } })
                Choice("色彩标准", listOf("2021" to "2021", "2025" to "2025"), prefs.colorSpec,
                    { value -> setDynamic(false); update { it.copy(colorSpec = value, colorSource = "custom") } })
                Preference("主题色", if (paletteExpanded) "收起" else "展开", { paletteExpanded = !paletteExpanded })
                if (paletteExpanded) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("#0066CC", "#FF6688", "#7E57C2", "#00A99D", "#197B38", "#EF6C00", "#795548", "#607D8B", "#AD1457", "#F9A825").forEach { hex ->
                            Surface(Modifier.size(48.dp).clickable { setDynamic(false); update { it.copy(seed = hex, colorSource = "custom") } }, shape = RoundedCornerShape(50), color = parseHexColor(hex)) {
                                if (prefs.seed == hex) Box(contentAlignment = Alignment.Center) { Text("✓", color = if (parseHexColor(hex).luminance() > .4) Color.Black else Color.White) }
                            }
                        }
                    }
                }
            }
            val scheme = MaterialTheme.colorScheme
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(scheme.primary, scheme.secondary, scheme.tertiary, scheme.surfaceContainer).forEach { color -> Box(Modifier.weight(1f).height(30.dp).background(color, RoundedCornerShape(8.dp))) }
            }
        } }
        item { SectionHeader("字体与密度") }
        item { TonalCard {
            Choice("字体大小", listOf("小" to .9f, "标准" to 1f, "大" to 1.15f, "更大" to 1.3f), prefs.fontScale, { value -> update { it.copy(fontScale = value) } })
            Toggle("字体加粗", prefs.fontBold, { value -> update { it.copy(fontBold = value) } })
            Choice("全局字重", listOf("默认" to 0, "轻" to 300, "常规" to 400, "中等" to 500, "粗" to 700), prefs.fontWeight, { value -> update { it.copy(fontWeight = value) } })
            Preference("应用字体", prefs.fontName.ifBlank { "内置楷体" }, importFont)
            if (prefs.fontFile.isNotEmpty()) Preference("恢复默认字体", "内置楷体", resetFont)
            Choice("界面缩放", listOf("紧凑" to .9f, "标准" to 1f, "宽松" to 1.1f), prefs.uiScale, { value -> update { it.copy(uiScale = value) } })
            Toggle("显示大小微调", prefs.displayScale > 0, { value -> update { it.copy(displayScale = if (value) 100 else 0) } })
            if (prefs.displayScale > 0) Choice("显示缩放", (80..120 step 5).map { "$it%" to it }, prefs.displayScale, { value -> update { it.copy(displayScale = value) } })
            Text("字体比例保留系统字号；界面缩放只影响本应用，不修改系统设置。", style = MaterialTheme.typography.bodySmall)
        } }
        item { SectionHeader("开屏") }
        item { TonalCard {
            Toggle("使用开屏壁纸", prefs.splashEnabled, { value -> update { it.copy(splashEnabled = value) } })
            Toggle("随机展示开屏壁纸", prefs.splashRandom, { value -> update { it.copy(splashRandom = value) } })
            if (prefs.splashEnabled && prefs.splashRandom) {
                Text("随机池预览")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("ink", "paper", "dawn", "stars").forEach { WallpaperPreview(it, Modifier.weight(1f).height(70.dp)) } }
            }
            Toggle("开屏图标遮罩动画", prefs.splashAnimation, { value -> update { it.copy(splashAnimation = value) } })
            if (prefs.splashEnabled) {
                WallpaperPreview(prefs.splashWallpaper, Modifier.fillMaxWidth().height(140.dp))
                Preference("选择开屏壁纸", "内置壁纸或相册图片", { showWallpapers = true })
            }
        } }
        item { SectionHeader("底部栏") }
        item { TonalCard {
            Choice("标签显示", listOf("图标与文字" to "both", "仅图标" to "icons", "仅文字" to "text"), prefs.dockLabels, { value -> update { it.copy(dockLabels = value) } })
            Choice("玻璃材质", listOf("通透" to "clear", "均衡" to "balanced", "磨砂" to "frosted"), prefs.glassMaterial, { value -> update { it.copy(glassMaterial = value) } })
        } }
        item { SectionHeader("语言") }
        item { TonalCard {
            Choice("应用语言", listOf("跟随系统" to "system", "简体中文" to "zh-CN", "繁體中文" to "zh-TW", "English" to "en"), prefs.language, { value -> update { it.copy(language = value) } })
            Text("界面标签即时切换；剧情、角色与 AI 内容保留原文。", style = MaterialTheme.typography.bodySmall)
        } }
    }
    colorTarget?.let { target ->
        ColorPickerDialog(if (target == "seed") prefs.seed else prefs.roleColors[target] ?: "#0066CC", { colorTarget = null }) { hex ->
            if (target == "seed") { setDynamic(false); update { it.copy(seed = hex, colorSource = "custom") } }
            else update { it.copy(roleColors = it.roleColors + (target to hex)) }
            colorTarget = null
        }
    }
    if (showWallpapers) ModalBottomSheet(onDismissRequest = { showWallpapers = false }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("选择开屏壁纸", style = MaterialTheme.typography.titleLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("墨色" to "ink", "纸页" to "paper", "晨光" to "dawn", "星空" to "stars").forEach { (name, key) ->
                    Column(Modifier.width(140.dp).clickable { update { it.copy(splashWallpaper = key) }; showWallpapers = false }) {
                        WallpaperPreview(key, Modifier.fillMaxWidth().height(90.dp)); Text(name)
                    }
                }
            }
            Button(onClick = { importWallpaper(); showWallpapers = false }) { Text("选择图片") }
        }
    }
}

@Composable
private fun <T> Choice(title: String, options: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit) {
    AppDropdown(title, options, selected, onSelect, Modifier.padding(vertical = 6.dp))
}

@Composable
private fun Toggle(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f)); Switch(checked, onChange)
    }
}

@Composable
private fun Preference(title: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp).heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title); Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Icon(AppIcons.ArrowForward, null)
    }
}

@Composable
private fun ColorPickerDialog(initial: String, onDismiss: () -> Unit, onApply: (String) -> Unit) {
    val hsv = remember(initial) { parseHexColor(initial).toHsv() }
    var hue by remember(initial) { mutableFloatStateOf(hsv[0]) }
    var saturation by remember(initial) { mutableFloatStateOf(hsv[1]) }
    var brightness by remember(initial) { mutableFloatStateOf(hsv[2]) }
    var hex by remember(initial) { mutableStateOf(initial) }
    fun sync() { hex = "#%06X".format(Color.hsv(hue, saturation, brightness).toArgb() and 0xFFFFFF) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("自定义主题颜色") }, text = {
        Column {
            Box(Modifier.fillMaxWidth().height(50.dp).background(Color.hsv(hue, saturation, brightness), RoundedCornerShape(12.dp)))
            Text("色相"); Slider(hue, { hue = it; sync() }, valueRange = 0f..360f)
            Text("饱和度"); Slider(saturation, { saturation = it; sync() })
            Text("亮度"); Slider(brightness, { brightness = it; sync() })
            OutlinedTextField(hex, { raw -> hex = raw; normalizeHex(raw)?.let { valid -> val next = parseHexColor(valid).toHsv(); hue = next[0]; saturation = next[1]; brightness = next[2] } }, label = { Text("#RRGGBB") }, singleLine = true, isError = normalizeHex(hex) == null)
        }
    }, confirmButton = { TextButton(onClick = { normalizeHex(hex)?.let(onApply) }, enabled = normalizeHex(hex) != null) { Text("确定") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}
