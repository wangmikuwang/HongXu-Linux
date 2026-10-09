package io.wenyou.textquest.data

import android.content.Context
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID

/** Imports are validated in a temporary file before publishing their new preference. */
object AppearanceFiles {
    fun fontDirectory(context: Context) = File(context.filesDir, "appearance-fonts").apply { mkdirs() }

    fun importFont(context: Context, source: File): Pair<String, String> {
        val name = source.name
        val extension = name.substringAfterLast('.', "").lowercase()
        require(extension in listOf("ttf", "otf", "ttc")) { "请选择 .ttf、.otf 或 .ttc 字体" }
        require(source.length() <= 32 * 1024 * 1024) { "字体不能超过 32 MB" }
        val file = File(fontDirectory(context), "font-${UUID.randomUUID()}.$extension")
        try {
            source.copyTo(file)
            RandomAccessFile(file, "r").use { font ->
                require(font.length() >= 12) { "字体文件无效" }
                val signature = font.readInt()
                require(signature in listOf(0x00010000, 0x4F54544F, 0x74727565, 0x74746366)) { "字体文件无效" }
                val offset = if (signature == 0x74746366) {
                    font.readInt()
                    require(font.readInt() > 0 && font.length() >= 16) { "字体集合无效" }
                    font.readInt().toLong() and 0xffffffffL
                } else 0L
                require(offset <= font.length() - 12) { "字体文件不完整" }
                font.seek(offset + 4)
                val tables = font.readUnsignedShort()
                require(tables > 0 && offset + 12L + tables * 16L <= font.length()) { "字体目录无效" }
                repeat(tables) { index ->
                    font.seek(offset + 12L + index * 16L + 8L)
                    val start = font.readInt().toLong() and 0xffffffffL
                    val length = font.readInt().toLong() and 0xffffffffL
                    require(start <= font.length() && length <= font.length() - start) { "字体文件不完整" }
                }
            }
            requireNotNull(org.jetbrains.skia.FontMgr.default.makeFromFile(file.path)) { "字体文件无效" }
            return file.name to name
        } catch (failure: Exception) { file.delete(); throw failure }
    }

    fun removeFont(context: Context, name: String) {
        if (name.matches(Regex("font-[a-zA-Z0-9-]+\\.(ttf|otf|ttc)"))) File(fontDirectory(context), name).delete()
    }

    /** Stores a JPEG copy no larger than 1440 px on its long side. */
    fun importWallpaper(context: Context, source: File): String {
        val image = runCatching { Image.makeFromEncoded(source.readBytes()) }.getOrNull() ?: error("无法识别图片")
        require(image.width > 0 && image.height > 0) { "无法识别图片" }
        val scale = minOf(1f, 1440f / maxOf(image.width, image.height))
        val width = (image.width * scale).toInt().coerceAtLeast(1)
        val height = (image.height * scale).toInt().coerceAtLeast(1)
        val surface = Surface.makeRasterN32Premul(width, height)
        surface.canvas.drawImageRect(image, Rect.makeWH(width.toFloat(), height.toFloat()))
        val jpeg = surface.makeImageSnapshot().encodeToData(EncodedImageFormat.JPEG, 90) ?: error("无法读取图片")
        val folder = File(context.filesDir, "appearance-wallpapers").apply { mkdirs() }
        val name = "wallpaper-${UUID.randomUUID()}.jpg"
        File(folder, name).writeBytes(jpeg.bytes)
        return name
    }

    fun wallpaper(context: Context, name: String): File? = name.takeIf {
        it.matches(Regex("wallpaper-[a-zA-Z0-9-]+\\.jpg"))
    }?.let { File(context.filesDir, "appearance-wallpapers/$it").takeIf(File::isFile) }
}
