package io.wenyou.textquest

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import io.wenyou.textquest.platform.appContext
import io.wenyou.textquest.platform.appIconPainter
import io.wenyou.textquest.platform.openAsset
import io.wenyou.textquest.ui.WenYouAppRoot
import io.wenyou.textquest.ui.windowKeyHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/** $XDG_DATA_HOME/<app> (~/.local/share/<app>) on Linux, %APPDATA%\<app> on Windows. */
private fun dataDirectory(): File {
    val name = BuildConfig.APP_FILE_PREFIX.lowercase()
    val windows = System.getenv("APPDATA")?.takeIf { System.getProperty("os.name").startsWith("Windows") }
    val base = windows ?: System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() } ?: "${System.getProperty("user.home")}/.local/share"
    return File(base, name).apply { mkdirs() }
}

/** 崩溃日志兜底：未捕获异常写入数据目录的 crash.log，以及设置中选择的目录。 */
private fun installCrashLogger(container: WenYouApp.AppContainer) {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        runCatching {
            val trace = StringWriter().also { PrintWriter(it).use(throwable::printStackTrace) }
            val text = "timeMillis=${System.currentTimeMillis()}\nversion=${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})\n" +
                "thread=${thread?.name}\n$trace"
            CrashLog.write(appContext, text, container.settings.crashDirUri)
        }
        previous?.uncaughtException(thread, throwable) ?: throw throwable
    }
}

/** A .wenyou file or share text passed on the command line (file managers open shared files this way). */
private fun offerArguments(args: Array<String>, container: WenYouApp.AppContainer) {
    for (arg in args) {
        val file = File(arg)
        val text = if (file.isFile && file.length() <= 20L * 1024 * 1024) runCatching { file.readText() }.getOrNull() else arg
        if (text != null && container.shareInbox.offer(text)) return
    }
}

private val windowPrefs by lazy { appContext.getSharedPreferences("window", Context.MODE_PRIVATE) }

/** The window reopens where and how big the player left it. */
private fun saveWindow(state: WindowState) {
    val edit = windowPrefs.edit().putBoolean("maximized", state.placement == WindowPlacement.Maximized)
    if (state.placement == WindowPlacement.Floating) {
        edit.putInt("width", state.size.width.value.toInt()).putInt("height", state.size.height.value.toInt())
        (state.position as? WindowPosition.Absolute)?.let { edit.putInt("x", it.x.value.toInt().coerceAtLeast(0)).putInt("y", it.y.value.toInt().coerceAtLeast(0)) }
    }
    edit.apply()
}

fun main(args: Array<String>) {
    appContext = Context(dataDirectory())
    val container = WenYouApp.AppContainer(appContext)
    installCrashLogger(container)
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { container.seedLibrary { openAsset(appContext, it) } }
    offerArguments(args, container)
    application {
        val window = rememberWindowState(placement = if (windowPrefs.getBoolean("maximized", false)) WindowPlacement.Maximized else WindowPlacement.Floating,
            position = windowPrefs.getInt("x", -1).takeIf { it >= 0 }?.let { WindowPosition(it.dp, windowPrefs.getInt("y", 0).dp) } ?: WindowPosition(Alignment.Center),
            size = DpSize(windowPrefs.getInt("width", 1120).dp, windowPrefs.getInt("height", 780).dp))
        Window(onCloseRequest = { saveWindow(window); exitApplication() }, state = window, title = BuildConfig.APP_NAME, icon = appIconPainter(),
            onKeyEvent = { windowKeyHandler(it) }) {
            LaunchedEffect(Unit) { this@Window.window.minimumSize = java.awt.Dimension(400, 560) }
            val owner = remember { object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() } }
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                WenYouAppRoot(container, showStartup = true)
            }
        }
    }
}
