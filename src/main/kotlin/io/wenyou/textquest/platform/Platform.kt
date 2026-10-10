package io.wenyou.textquest.platform

import android.content.Context
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.wenyou.textquest.BuildConfig
import io.wenyou.textquest.data.repo.ShareInbox
import kotlinx.coroutines.flow.MutableSharedFlow
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.UIManager

/*
 * What the screens need from the desktop: files, clipboard, links, messages and Skia shaders.
 * Names match the Android app's equivalents so ported screens stay close to their Android originals.
 */

/** A document the user picked or created. */
typealias PlatformFile = File

/** Installed from Flathub: the software centre updates the app, so it never checks or prompts on its own. */
val inFlatpak: Boolean = System.getenv("FLATPAK_ID") != null

/** Set once at startup (Main.kt): where the app keeps its data. */
lateinit var appContext: Context

/** Short messages shown by the window's snackbar (the desktop's toast). */
val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)

@Composable fun platformContext(): Context = appContext

/** Shows [messages] one at a time. */
@Composable
fun MessageHost(modifier: Modifier = Modifier) {
    val state = remember { SnackbarHostState() }
    LaunchedEffect(state) { messages.collect { state.showSnackbar(it) } }
    SnackbarHost(state, modifier)
}

@Composable fun appName(): String = BuildConfig.APP_NAME

fun appLabel(context: Context): String = BuildConfig.APP_NAME

fun showMessage(context: Context, text: String) { messages.tryEmit(io.wenyou.textquest.ui.common.tr(text)) }

private val linux = System.getProperty("os.name").lowercase().contains("linux")

/** xdg-open on Linux (java.awt.Desktop is unreliable outside GNOME), java.awt.Desktop elsewhere. */
private fun open(target: String): Boolean = runCatching {
    if (linux) ProcessBuilder("xdg-open", target).start()
    else if (target.startsWith("http")) java.awt.Desktop.getDesktop().browse(java.net.URI(target))
    else java.awt.Desktop.getDesktop().open(File(target))
}.isSuccess

fun openLink(context: Context, url: String): Boolean = open(url)

fun openDownloads(context: Context): Boolean = open(downloadsDir().path)

/** The user's download folder (XDG on Linux), falling back to ~/Downloads. */
fun downloadsDir(): File {
    val xdg = if (linux) runCatching {
        ProcessBuilder("xdg-user-dir", "DOWNLOAD").start().inputStream.bufferedReader().readText().trim()
    }.getOrNull() else null
    return File(xdg?.takeIf { it.isNotEmpty() } ?: "${System.getProperty("user.home")}/Downloads")
}

fun copyText(context: Context, label: String, text: String) {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
}

/** No system share sheet on the desktop: the text goes to the clipboard, ready to paste into any chat. */
fun shareText(context: Context, subject: String, text: String, chooserTitle: String) {
    copyText(context, subject, text)
    showMessage(context, "已复制，可粘贴到聊天或邮件中发送")
}

/** Saves [text] as [fileName] wherever the user chooses, to send as an attachment. */
fun shareFile(context: Context, fileName: String, text: String, chooserTitle: String) {
    val file = saveDialog(chooserTitle, fileName) ?: return
    val ok = runCatching { file.writeText(text) }.isSuccess
    showMessage(context, if (ok) "已保存到 ${file.path}" else "保存失败")
}

/** Offers a share code found in newly copied clipboard text (each copied text once). */
fun checkClipboardForShare(context: Context, inbox: ShareInbox) {
    val text = runCatching {
        Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String
    }.getOrNull() ?: return
    val stamp = text.hashCode().toLong() or (1L shl 32)
    if (stamp == inbox.lastClipStamp) return
    inbox.lastClipStamp = stamp
    inbox.offer(text, fromClipboard = true)
}

fun openAsset(context: Context, name: String): InputStream =
    Thread.currentThread().contextClassLoader.getResourceAsStream(name) ?: throw FileNotFoundException(name)

fun openInput(context: Context, file: PlatformFile): InputStream? = runCatching { file.inputStream() }.getOrNull()

fun writeBytes(context: Context, file: PlatformFile, bytes: ByteArray): Boolean = runCatching { file.writeBytes(bytes) }.isSuccess

/** Swing's chooser follows the app's (Chinese) locale; the AWT/GTK dialog would follow the system language. */
private fun chooser(title: String): JFileChooser {
    systemLookAndFeel
    return JFileChooser().apply { dialogTitle = title }
}

private val systemLookAndFeel by lazy { runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) } }

private fun saveDialog(title: String, suggested: String): File? {
    val dialog = chooser(title).apply { selectedFile = File(downloadsDir(), suggested) }
    if (dialog.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return null
    val file = dialog.selectedFile
    val replace = !file.exists() || JOptionPane.showConfirmDialog(null, "「${file.name}」已存在，要替换吗？", title,
        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION
    return file.takeIf { replace }
}

private fun openDialog(title: String, multiple: Boolean): List<File> {
    val dialog = chooser(title).apply { isMultiSelectionEnabled = multiple }
    if (dialog.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return emptyList()
    return if (multiple) dialog.selectedFiles.toList() else listOfNotNull(dialog.selectedFile)
}

/** Save dialog; [onResult] gets null when cancelled. Launch with the suggested file name. */
@Composable
fun rememberCreateDocument(mime: String, onResult: (PlatformFile?) -> Unit): (String) -> Unit {
    val callback by rememberUpdatedState(onResult)
    return { name -> callback(saveDialog("保存", name)) }
}

/** Open dialog; [onResult] gets null when cancelled. The MIME types are not enforced; importers validate content. */
@Composable
fun rememberOpenDocument(onResult: (PlatformFile?) -> Unit): (Array<String>) -> Unit {
    val callback by rememberUpdatedState(onResult)
    return { callback(openDialog("打开", multiple = false).firstOrNull()) }
}

@Composable
fun rememberPickImages(onResult: (List<PlatformFile>) -> Unit): () -> Unit {
    val callback by rememberUpdatedState(onResult)
    return { callback(openDialog("选择图片", multiple = true)) }
}

/** Folder picker; [onResult] gets the folder path, or null. */
@Composable
fun rememberPickDirectory(onResult: (String?) -> Unit): () -> Unit {
    val callback by rememberUpdatedState(onResult)
    return {
        val chooser = chooser("选择文件夹").apply { fileSelectionMode = JFileChooser.DIRECTORIES_ONLY }
        callback(if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile.path else null)
    }
}

fun loadImage(file: File): ImageBitmap? = runCatching { Image.makeFromEncoded(file.readBytes()).toComposeImageBitmap() }.getOrNull()

private val icon by lazy { Image.makeFromEncoded(openAsset(appContext, "icon.png").readBytes()).toComposeImageBitmap() }

@Composable fun appIconPainter(): Painter = BitmapPainter(icon)

/** The liquid-glass AGSL shaders run unchanged as Skia SkSL. Null if the GPU backend rejects one. */
fun glassShader(source: String): GlassShader? = runCatching { GlassShader(source) }.getOrNull()

class GlassShader(source: String) {
    private val builder = RuntimeShaderBuilder(RuntimeEffect.makeForShader(source))

    fun uniform(name: String, vararg values: Float) = when (values.size) {
        1 -> builder.uniform(name, values[0])
        2 -> builder.uniform(name, values[0], values[1])
        3 -> builder.uniform(name, values[0], values[1], values[2])
        else -> builder.uniform(name, values[0], values[1], values[2], values[3])
    }

    /** The shader reads the layer as [child]; with [blur] > 0 it reads the blurred layer. */
    fun effect(child: String, blur: Float = 0f): RenderEffect {
        val input = if (blur > 0f) ImageFilter.makeBlur(blur, blur, FilterTileMode.CLAMP) else null
        return ImageFilter.makeRuntimeShader(builder, child, input).asComposeRenderEffect()
    }
}
