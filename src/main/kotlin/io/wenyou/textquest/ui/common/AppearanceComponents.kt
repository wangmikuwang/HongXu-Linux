package io.wenyou.textquest.ui.common

import io.wenyou.textquest.ui.theme.readableAccent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import io.wenyou.textquest.ui.theme.resolveFontWeight
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.wenyou.textquest.ui.theme.LocalAppearance

@Composable
fun AppText(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified, fontStyle: FontStyle? = null, fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null, letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null, textAlign: TextAlign? = null, lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip, softWrap: Boolean = true, maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1, onTextLayout: (TextLayoutResult) -> Unit = {}, style: TextStyle = LocalTextStyle.current) {
    val prefs = LocalAppearance.current
    androidx.compose.material3.Text(uiLabel(text, prefs.language), modifier, color, fontSize, fontStyle,
        prefs.resolveFontWeight(fontWeight ?: style.fontWeight), fontFamily, letterSpacing,
        textDecoration, textAlign, lineHeight, overflow, softWrap, maxLines, minLines, onTextLayout, style)
}

/** User-authored content never participates in interface translation. */
@Composable
fun RawText(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified, fontStyle: FontStyle? = null, fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null, letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null, textAlign: TextAlign? = null, lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip, softWrap: Boolean = true, maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1, onTextLayout: (TextLayoutResult) -> Unit = {}, style: TextStyle = LocalTextStyle.current) {
    val prefs = LocalAppearance.current
    androidx.compose.material3.Text(text, modifier, color, fontSize, fontStyle,
        prefs.resolveFontWeight(fontWeight ?: style.fontWeight), fontFamily, letterSpacing,
        textDecoration, textAlign, lineHeight, overflow, softWrap, maxLines, minLines, onTextLayout, style)
}

/** Icon preferences affect glyph color, never add a second background or shrink the glyph. */
@Composable
fun AppIcon(imageVector: ImageVector, contentDescription: String?, modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified) {
    val appearance = LocalAppearance.current
    val inherited = LocalContentColor.current
    val scheme = androidx.compose.material3.MaterialTheme.colorScheme
    val neutral = inherited == scheme.onSurface || inherited == scheme.onSurfaceVariant
    val resolved = if (tint != Color.Unspecified) tint
        else if (appearance.iconStyle == "color" && neutral && imageVector != AppIcons.ArrowBack)
            androidx.compose.material3.MaterialTheme.colorScheme.readableAccent()
        else LocalContentColor.current
    androidx.compose.material3.Icon(imageVector, contentDescription?.let { uiLabel(it, appearance.language) }, modifier, resolved)
}

@Composable
fun AppIcon(painter: Painter, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
    val appearance = LocalAppearance.current
    val inherited = LocalContentColor.current
    val scheme = androidx.compose.material3.MaterialTheme.colorScheme
    val neutral = inherited == scheme.onSurface || inherited == scheme.onSurfaceVariant
    val resolved = if (tint != Color.Unspecified) tint else if (appearance.iconStyle == "color" && neutral)
        androidx.compose.material3.MaterialTheme.colorScheme.readableAccent() else LocalContentColor.current
    androidx.compose.material3.Icon(painter, contentDescription?.let { uiLabel(it, appearance.language) }, modifier, resolved)
}

/** Only UI labels are translated; narrative, imported data, API output and text input are untouched. */
internal fun uiLabel(text: String, language: String): String {
    val resolved = if (language == "system") java.util.Locale.getDefault().toLanguageTag() else language
    if (resolved.startsWith("en")) return EnglishLabels[text] ?: text
    if (resolved.startsWith("zh-TW") || resolved.startsWith("zh-HK") || resolved.startsWith("zh-Hant")) {
        if (text !in EnglishLabels) return text
        return text.map { TraditionalCharacters[it] ?: it }.joinToString("")
    }
    return text
}

private val TraditionalCharacters = "设置观与题显图标样式选项弹窗统默认浅深颜来源自义高级风格标准字体大小权重应用恢复界面缩放细调开屏壁纸随机展示遮罩动画语言跟随简体个栏标签清晰均衡磨砂返回关闭保存取消确定剧情角色主页服务开始编辑新建导入导出备份生成实时通知成人内容规则底层基调管理诊断关关于版本检查更新下载安装当前旅程成就馆全部数据本地".zip(
    "設置觀與題顯圖標樣式選項彈窗統默認淺深顏來源自義高級風格標準字體大小權重應用恢復界面縮放細調開屏壁紙隨機展示遮罩動畫語言跟隨簡體個欄標籤清晰均衡磨砂返回關閉保存取消確定劇情角色主頁服務開始編輯新建導入導出備份生成實時通知成人內容規則底層基調管理診斷關關於版本檢查更新下載安裝當前旅程成就館全部數據本地").toMap()

private val EnglishLabels = mapOf(
    "主页" to "Home", "剧情" to "Stories", "角色" to "Cast", "AI 服务" to "AI", "设置" to "Settings", "返回" to "Back",
    "外观与主题" to "Appearance & theme", "显示模式" to "Display", "界面风格" to "Interface style", "屏幕帧率" to "Frame rate",
    "安卓液态玻璃" to "Liquid Glass", "列表条目样式" to "List style", "图标样式" to "Icon style", "选项弹窗样式" to "Selection popup",
    "主题与色彩" to "Theme & color", "主题模式" to "Theme mode", "深色风格" to "Dark style", "主题颜色来源" to "Color source",
    "自定义主题颜色" to "Custom color", "高级配色" to "Advanced colors", "色彩风格" to "Palette style", "色彩标准" to "Color specification",
    "主题色" to "Accent color", "字体与密度" to "Font & density", "字体大小" to "Font size", "全局字重" to "Font weight", "字体加粗" to "Bold text",
    "应用字体" to "App font", "恢复默认字体" to "Reset font", "界面缩放" to "UI scale", "显示大小微调" to "Display fine tuning",
    "显示缩放" to "Display scale", "开屏与图标" to "Startup & icon", "应用图标" to "App icon", "图标外观" to "Icon shell",
    "使用开屏壁纸" to "Startup wallpaper", "随机展示开屏壁纸" to "Random wallpaper", "选择开屏壁纸" to "Choose wallpaper",
    "随机池预览" to "Wallpaper pool", "开屏图标遮罩动画" to "Startup icon animation", "语言" to "Language", "应用语言" to "App language",
    "底部栏" to "Bottom dock", "标签显示" to "Dock labels", "玻璃材质" to "Glass material", "通透" to "Clear", "均衡" to "Balanced", "磨砂" to "Frosted",
    "图标与文字" to "Icons & text", "仅图标" to "Icons", "仅文字" to "Text", "跟随系统" to "System", "浅色" to "Light", "深色" to "Dark",
    "默认" to "Default", "纯黑" to "AMOLED", "品牌配色" to "Brand", "壁纸取色" to "Wallpaper", "自定义" to "Custom",
    "跟随预设" to "Preset", "统一圆角" to "Rounded", "彩色图标" to "Colored", "单色图标" to "Monochrome",
    "跟随选项弹出" to "Anchored popup", "居中弹窗" to "Centered dialog", "液态玻璃" to "Liquid Glass", "标准" to "Standard",
    "小" to "Small", "大" to "Large", "更大" to "Extra large", "轻" to "Light", "常规" to "Regular", "中等" to "Medium", "粗" to "Bold",
    "紧凑" to "Compact", "宽松" to "Comfortable", "自动（系统）" to "Automatic", "系统默认字体" to "System font",
    "简体中文" to "简体中文", "繁體中文" to "繁體中文", "English" to "English", "关闭" to "Close", "取消" to "Cancel", "确定" to "Apply",
    "保存" to "Save", "选择图片" to "Choose image", "墨色" to "Ink", "明亮" to "Light", "暗夜" to "Dark", "纸页" to "Paper", "晨光" to "Dawn", "星空" to "Stars",
    "背景" to "Background", "主要文字" to "Primary text", "次要文字" to "Secondary text", "控件" to "Controls", "浅色模式" to "Light mode", "深色模式" to "Dark mode",
    "开始剧情" to "Start story", "新建剧情" to "New story", "创建" to "Create", "手动创建" to "Manual creation", "选择创建方式" to "Choose how to create",
    "编写世界、开场和剧情分支" to "Write the world, opening and story branches", "设定人物、性格和背景" to "Define a character, personality and background",
    "AI 一句话创建" to "AI creation", "用一句话生成剧情与人物，保存前可预览和修改" to "Generate a story and cast, then review and edit before saving", "AI 创建" to "AI creation", "剧情库" to "Story library", "全部剧情" to "All stories",
    "让你的故事继续" to "Let your story continue", "故事，从这里开始" to "Your story starts here", "角色库" to "Character library", "新建角色" to "New character",
    "AI 与生成" to "AI & generation", "默认服务" to "Default provider", "系统通知设置" to "System notifications", "生成实时通知" to "Generation notifications",
    "成人内容" to "Adult content", "数据备份" to "Backup", "导出备份" to "Export backup", "导入备份" to "Import backup", "角色规则" to "Character rules",
    "底层基调" to "Base rules", "新建底层基调" to "New base rule", "管理底层基调" to "Manage rules", "诊断与关于" to "Diagnostics & about", "版本" to "Version",
    "检查更新" to "Check updates", "下载更新" to "Download update", "立即安装" to "Install now", "游玩" to "Play", "编辑" to "Edit", "删除" to "Delete",
    "旁白" to "Narration", "AI 思考过程" to "AI reasoning", "收起" to "Collapse", "展开" to "Expand", "发送" to "Send", "自由身份" to "Free identity",
    "请先选择扮演身份" to "Choose your role", "生成用量与费用统计" to "Usage & cost", "让导演继续（不输入直接推进）" to "Let the director continue"
)
