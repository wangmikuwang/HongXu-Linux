package io.wenyou.textquest.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.wenyou.textquest.BuildConfig
import io.wenyou.textquest.data.AppUpdates
import io.wenyou.textquest.data.UpdatePolicy
import io.wenyou.textquest.data.isNewerVersion
import io.wenyou.textquest.data.model.AppJson
import io.wenyou.textquest.data.requiresAppUpdate
import io.wenyou.textquest.platform.inFlatpak
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

data class LinuxRelease(val version: String, val notes: String)

data class AppUpdateState(
    val busy: Boolean = false, val release: LinuxRelease? = null, val message: String = "",
    val policy: UpdatePolicy = UpdatePolicy(), val promptVisible: Boolean = false
) {
    val required get() = !inFlatpak && requiresAppUpdate(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME, policy)
}

/** Finds newer releases; installing is done by the user's package manager from the release page. */
class AppUpdateViewModel : ViewModel() {
    private val client = OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build()
    private val updates = AppUpdates(client)
    private val state = MutableStateFlow(AppUpdateState())
    val ui = state.asStateFlow()
    private var checked = false

    /** Checks once per launch. */
    fun resume() { if (!checked) check(automatic = true) }

    fun check(automatic: Boolean = false) {
        if (inFlatpak || state.value.busy) return
        checked = true
        state.value = state.value.copy(busy = true, message = "正在检查更新…")
        viewModelScope.launch {
            try { updates.policy()?.let { state.value = state.value.copy(policy = it) } }
            catch (e: CancellationException) { throw e } catch (_: Exception) { /* Keep the built-in policy. */ }
            try {
                val release = withContext(Dispatchers.IO) { latest() }?.takeIf { isNewerVersion(it.version, BuildConfig.VERSION_NAME) }
                state.value = state.value.copy(busy = false, release = release, promptVisible = automatic && release != null && !state.value.required,
                    message = if (release != null) "发现新版本 ${release.version}，可打开发布页面下载 Linux 安装包。" else "当前已是最新版本")
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { state.value = state.value.copy(busy = false, message = "检查失败，请检查网络或稍后重试，也可打开发布页面。") }
        }
    }

    fun dismissPrompt() { state.value = state.value.copy(promptVisible = false) }
    fun showOpenError() { state.value = state.value.copy(message = "无法打开浏览器，请手动访问 $RELEASE_PAGE") }

    /** This edition's latest GitHub release (pre-releases on the test branch are skipped); null before the first release. */
    private fun latest(): LinuxRelease? {
        val text = get("https://api.github.com/repos/${BuildConfig.UPDATE_REPOSITORY}/releases/latest") ?: return null
        val release = AppJson.parseToJsonElement(text).jsonObject
        if (release["draft"]?.jsonPrimitive?.booleanOrNull == true || release["prerelease"]?.jsonPrimitive?.booleanOrNull == true) return null
        return LinuxRelease(release.getValue("tag_name").jsonPrimitive.content.removePrefix("v"), release["body"]?.jsonPrimitive?.contentOrNull.orEmpty().take(8_000))
    }

    /** Body text, or null for 404 (no release yet). */
    private fun get(url: String): String? = client.newCall(Request.Builder().url(url)
        .header("Accept", "application/vnd.github+json").header("User-Agent", "${BuildConfig.APP_FILE_PREFIX}-linux-update").build())
        .execute().use { if (it.code == 404) null else if (!it.isSuccessful) throw IOException("HTTP ${it.code}") else it.body!!.string() }

    companion object {
        val RELEASE_PAGE = "https://github.com/${BuildConfig.UPDATE_REPOSITORY}/releases/latest"
    }
}
