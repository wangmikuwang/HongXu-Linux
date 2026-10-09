package io.wenyou.textquest.data.repo

import android.content.Context
import android.content.SharedPreferences
import io.wenyou.textquest.ui.theme.PrideTheme
import io.wenyou.textquest.ui.theme.ThemeMode
import io.wenyou.textquest.ui.theme.ThemeStyle
import io.wenyou.textquest.ui.theme.AppearancePrefs
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 外观/内容偏好（由根主题与设置页共享观察）。 */
data class UiPrefs(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val defaultProviderId: String? = null,
    val showLgbt: Boolean = true,
    val adultContent: Boolean = true,
    /** 是否已通过「连点版本号 10 次」解锁内容开关（α 版此项默认隐藏）。 */
    val contentUnlocked: Boolean = false,
    val generationNotifications: Boolean = false,
    val themeStyle: ThemeStyle = ThemeStyle.MATERIAL,
    val prideThemesUnlocked: Boolean = false,
    val prideThemesEnabled: Boolean = false,
    val prideTheme: PrideTheme? = null,
    val appearance: AppearancePrefs = AppearancePrefs()
)

/** 轻量应用设置（SharedPreferences），变更同步发布到 [state] 供主题实时响应。 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("wenyou_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<UiPrefs> = _state.asStateFlow()

    private fun load(): UiPrefs {
        val unlocked = prefs.getBoolean("pride_unlocked", false) ||
            prefs.getInt("pride_unlock_version", -1) >= 0 || prefs.getInt("pride_tag_taps", 0) >= 15
        val enabled = unlocked && prefs.getBoolean("pride_enabled", true)
        prefs.edit().putBoolean("pride_unlocked", unlocked)
            .remove("pride_tag_taps").remove("pride_unlock_version").apply()
        return UiPrefs(
        appearance = runCatching { prefs.getString("appearance_v1", null)?.let { appearanceJson.decodeFromString<AppearancePrefs>(it).normalized() } ?: AppearancePrefs(glassEnabled = ThemeStyle.fromStored(prefs.getString(KEY_STYLE, null)) == ThemeStyle.APPLE) }.getOrDefault(AppearancePrefs()),
        generationNotifications = prefs.getBoolean("generation_notifications", false),
        themeStyle = ThemeStyle.fromStored(prefs.getString(KEY_STYLE, null)),
        themeMode = themeOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)),
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC, true),
        defaultProviderId = prefs.getString(KEY_PROVIDER, null),
        showLgbt = prefs.getBoolean(KEY_SHOW_LGBT, true),
        adultContent = prefs.getBoolean(KEY_ADULT, true),
        prideThemesUnlocked = unlocked,
        prideThemesEnabled = enabled,
        prideTheme = if (enabled) PrideTheme.fromStored(prefs.getString("pride_theme", null)) else null,
        contentUnlocked = prefs.getBoolean(KEY_CONTENT_UNLOCKED, false)
        )
    }

    /** 旧版本可能写入过未知枚举值（主题改名/清理残留），损坏时回退跟随系统。 */
    private fun themeOf(raw: String?): ThemeMode = try {
        ThemeMode.valueOf(raw ?: "")
    } catch (_: Throwable) {
        ThemeMode.SYSTEM
    }

    fun setGenerationNotifications(on: Boolean) {
        prefs.edit().putBoolean("generation_notifications", on).apply()
        _state.value = _state.value.copy(generationNotifications = on)
    }

    @Synchronized
    fun updateAppearance(change: (AppearancePrefs) -> AppearancePrefs) {
        val next = change(_state.value.appearance).normalized()
        prefs.edit().putString("appearance_v1", appearanceJson.encodeToString(next)).apply()
        _state.value = _state.value.copy(appearance = next)
    }

    fun unlockPrideThemes() {
        prefs.edit().putBoolean("pride_unlocked", true).putBoolean("pride_enabled", true).apply()
        _state.value = _state.value.copy(prideThemesUnlocked = true, prideThemesEnabled = true)
    }

    fun setPrideThemesEnabled(on: Boolean) {
        if (!_state.value.prideThemesUnlocked) return
        prefs.edit().putBoolean("pride_enabled", on).apply()
        _state.value = _state.value.copy(prideThemesEnabled = on,
            prideTheme = if (on) PrideTheme.fromStored(prefs.getString("pride_theme", null)) else null)
    }

    fun setPrideTheme(theme: PrideTheme?) {
        if (theme != null && !_state.value.prideThemesEnabled) return
        prefs.edit().putString("pride_theme", theme?.name).apply()
        _state.value = _state.value.copy(prideTheme = theme)
    }

    fun setThemeStyle(style: ThemeStyle) {
        prefs.edit().putString(KEY_STYLE, style.name).apply()
        _state.value = _state.value.copy(themeStyle = style)
        updateAppearance { it.copy(glassEnabled = style == ThemeStyle.APPLE) }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _state.value = _state.value.copy(themeMode = mode)
    }

    fun setDynamicColor(on: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC, on).apply()
        _state.value = _state.value.copy(dynamicColor = on)
    }

    fun setDefaultProvider(id: String?) {
        prefs.edit().putString(KEY_PROVIDER, id).apply()
        _state.value = _state.value.copy(defaultProviderId = id)
    }

    fun setShowLgbt(on: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_LGBT, on).apply()
        _state.value = _state.value.copy(showLgbt = on)
    }

    fun setAdultContent(on: Boolean) {
        prefs.edit().putBoolean(KEY_ADULT, on).apply()
        _state.value = _state.value.copy(adultContent = on)
    }

    fun setContentUnlocked(on: Boolean) {
        prefs.edit().putBoolean(KEY_CONTENT_UNLOCKED, on).apply()
        _state.value = _state.value.copy(contentUnlocked = on)
    }

    // ---- 兼容旧读取点 ----
    val defaultProviderId: String?
        get() = prefs.getString(KEY_PROVIDER, null)

    var seeded: Boolean
        get() = prefs.getBoolean(KEY_SEEDED, false)
        set(value) = prefs.edit().putBoolean(KEY_SEEDED, value).apply()

    /** 记录已成功合并过的预设资源文件名，避免每次启动重复全量导入，防止用户删除的内置内容被重新写回。 */
    fun appliedPresetFiles(): Set<String> =
        prefs.getStringSet(KEY_PRESET_FILES, emptySet())?.toSet() ?: emptySet()

    fun markPresetFileApplied(name: String) {
        val next = appliedPresetFiles() + name
        prefs.edit().putStringSet(KEY_PRESET_FILES, next).apply()
    }

    /** 已为内置角色补齐过一次「初始状态」（非破坏性；避免每次启动重复扫描）。 */
    var presetEnrichDone: Boolean
        get() = prefs.getBoolean(KEY_PRESET_ENRICH, false)
        set(value) = prefs.edit().putBoolean(KEY_PRESET_ENRICH, value).apply()

    /** 已为内置角色补齐过一次「性取向」（非破坏性）。 */
    var presetOrientDone: Boolean
        get() = prefs.getBoolean(KEY_PRESET_ORIENT, false)
        set(value) = prefs.edit().putBoolean(KEY_PRESET_ORIENT, value).apply()

    /** 崩溃日志保存目录（SAF 授权的 Documents tree URI；空 = 未选择）。 */
    var crashDirUri: String?
        get() = prefs.getString(KEY_CRASH_DIR, null)
        set(value) = prefs.edit().putString(KEY_CRASH_DIR, value).apply()


    private companion object {
        val appearanceJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        const val KEY_STYLE = "theme_style"
        const val KEY_THEME = "theme_mode"
        const val KEY_DYNAMIC = "dynamic_color"
        const val KEY_PROVIDER = "default_provider"
        const val KEY_SEEDED = "seeded_v1"
        const val KEY_PRESET_FILES = "preset_files_applied_v2"
        const val KEY_PRESET_ENRICH = "preset_enrich_initial_v1"
        const val KEY_PRESET_ORIENT = "preset_enrich_orient_v1"
        const val KEY_CRASH_DIR = "crash_dir_uri"
        const val KEY_SHOW_LGBT = "show_lgbt"
        const val KEY_ADULT = "adult_content"
        const val KEY_CONTENT_UNLOCKED = "content_unlocked_v1"
    }
}
