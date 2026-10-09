package io.wenyou.textquest

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
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

fun main(args: Array<String>) {
    appContext = Context(dataDirectory())
    val container = WenYouApp.AppContainer(appContext)
    installCrashLogger(container)
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { container.seedLibrary { openAsset(appContext, it) } }
    offerArguments(args, container)
    application {
        val window = rememberWindowState(width = 480.dp, height = 900.dp)
        Window(onCloseRequest = ::exitApplication, state = window, title = BuildConfig.APP_NAME, icon = appIconPainter()) {
            val owner = remember { object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() } }
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                WenYouAppRoot(container, showStartup = true)
            }
        }
    }
}
