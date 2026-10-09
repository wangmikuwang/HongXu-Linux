package io.wenyou.textquest.ui.common

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import io.wenyou.textquest.BuildConfig
import io.wenyou.textquest.data.repo.ShareCode
import io.wenyou.textquest.platform.PlatformFile
import io.wenyou.textquest.platform.openAsset
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Surface
import org.jetbrains.skia.TextLine
import java.io.File

/** Share QR codes: the same codes, poster layout and multi-code reading as the Android app, drawn with Skia. */
object QrCode {
    private fun matrix(content: String, size: Int): BitMatrix? {
        if (content.isBlank()) return null
        // 容错等级 M + 加大留白：对“手机拍屏幕”产生的摩尔纹更有韧性
        val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8", EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 4)
        return runCatching { QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints) }.getOrNull()
    }

    private fun image(content: String, size: Int): Image? {
        val m = matrix(content, size) ?: return null
        val pixels = ByteArray(size * size * 4)
        for (y in 0 until size) for (x in 0 until size) {
            val v = if (m.get(x, y)) 0 else 255
            val i = (y * size + x) * 4
            pixels[i] = v.toByte(); pixels[i + 1] = v.toByte(); pixels[i + 2] = v.toByte(); pixels[i + 3] = -1
        }
        val bitmap = Bitmap().apply { allocPixels(ImageInfo.makeN32Premul(size, size)); installPixels(pixels) }
        return Image.makeFromBitmap(bitmap)
    }

    fun encodeImage(content: String, size: Int = 640): ImageBitmap? = image(content, size)?.toComposeImageBitmap()

    /** 识别一张或一套分片二维码图片（含一张图里有多个码的分享海报），并返回完整分享码。 */
    fun decodeShareImages(context: Context, files: List<PlatformFile>): String? {
        val texts = files.flatMap { file -> runCatching { decodeAll(Image.makeFromEncoded(file.readBytes())) }.getOrNull().orEmpty() }
        return ShareCode.assembleQrTexts(texts)
    }

    /** Every QR code in one image; falls back to the single-code reader. Text is never trimmed (Base45 may end in a space). */
    private fun decodeAll(image: Image): List<String> {
        val bitmap = Bitmap.makeFromImage(image)
        val w = bitmap.width; val h = bitmap.height
        val pixels = IntArray(w * h) { i -> bitmap.getColor(i % w, i / w) }
        val binary = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(w, h, pixels)))
        val hints = mapOf(DecodeHintType.CHARACTER_SET to "UTF-8", DecodeHintType.TRY_HARDER to true)
        val found = runCatching { QRCodeMultiReader().decodeMultiple(binary, hints).map { it.text } }.getOrNull().orEmpty()
        return found.ifEmpty {
            listOfNotNull(runCatching { MultiFormatReader().decode(binary, hints + (DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE))).text }.getOrNull())
        }
    }

    private val typeface by lazy { FontMgr.default.makeFromData(org.jetbrains.skia.Data.makeFromBytes(openAsset(io.wenyou.textquest.platform.appContext, "bundled_kai_regular.ttf").readBytes())) }

    /** One shareable image: the title, then every page of the QR book in a numbered grid that image import reads at once. */
    private fun poster(title: String, appName: String, pages: List<String>): Image? {
        if (pages.isEmpty()) return null
        val cols = when { pages.size == 1 -> 1; pages.size <= 4 -> 2; else -> 3 }
        val rows = (pages.size + cols - 1) / cols
        val cell = 440; val gap = 40; val pad = 56; val label = 52
        val gridWidth = cols * cell + (cols - 1) * gap
        val width = maxOf(gridWidth + pad * 2, 760)
        val hintFont = Font(typeface, 30f)
        val hintLines = wrap("共 ${pages.size} 张 · 保存这张图，在「$appName」中选择「导入 → 相册识别」即可一次导入", hintFont, (width - pad * 2).toFloat())
        val header = pad + 64 + 16 + hintLines.size * 40 + 40
        val height = header + rows * (cell + label) + (rows - 1) * gap + pad
        val surface = Surface.makeRasterN32Premul(width, height)
        val canvas: Canvas = surface.canvas
        canvas.clear(Color.WHITE)
        val titlePaint = Paint().apply { color = 0xFF1D1B22.toInt() }
        val greyPaint = Paint().apply { color = 0xFF625E6C.toInt() }
        val titleFont = Font(typeface, 56f).apply { isEmboldened = true }
        canvas.drawTextLine(TextLine.make(ellipsize(title, titleFont, (width - pad * 2).toFloat()), titleFont), pad.toFloat(), (pad + 56).toFloat(), titlePaint)
        hintLines.forEachIndexed { i, line -> canvas.drawTextLine(TextLine.make(line, hintFont), pad.toFloat(), (pad + 64 + 16 + 30 + i * 40).toFloat(), greyPaint) }
        val labelFont = Font(typeface, 28f)
        val left = (width - gridWidth) / 2
        pages.forEachIndexed { i, page ->
            val qr = image(page, cell) ?: return null
            val x = left + (i % cols) * (cell + gap)
            val y = header + (i / cols) * (cell + label + gap)
            canvas.drawImage(qr, x.toFloat(), y.toFloat())
            if (pages.size > 1) {
                val text = TextLine.make("第 ${i + 1} 张", labelFont)
                canvas.drawTextLine(text, x + (cell - text.width) / 2, (y + cell + 38).toFloat(), greyPaint)
            }
        }
        return surface.makeImageSnapshot()
    }

    private fun wrap(text: String, font: Font, width: Float): List<String> {
        val lines = mutableListOf<String>()
        var line = ""
        for (ch in text) {
            if (line.isNotEmpty() && font.measureTextWidth(line + ch) > width) { lines += line; line = "" }
            line += ch
        }
        return lines + line
    }

    private fun ellipsize(text: String, font: Font, width: Float): String {
        if (font.measureTextWidth(text) <= width) return text
        var cut = text
        while (cut.isNotEmpty() && font.measureTextWidth("$cut…") > width) cut = cut.dropLast(1)
        return "$cut…"
    }

    /** Saves the poster to Pictures/<app>; returns the folder, or null. */
    fun savePoster(context: Context, title: String, appName: String, pages: List<String>, fileTitle: String): String? = runCatching {
        val png = poster(title, appName, pages)?.encodeToData(EncodedImageFormat.PNG) ?: return null
        val safeTitle = fileTitle.replace(Regex("[^\\w\\u4e00-\\u9fa5-]"), "_").take(40).ifBlank { "share_qr" }
        val dir = File(picturesDir(), BuildConfig.APP_FILE_PREFIX).apply { mkdirs() }
        File(dir, "$safeTitle.png").writeBytes(png.bytes)
        dir.path
    }.getOrNull()

    private fun picturesDir(): File {
        val xdg = runCatching { ProcessBuilder("xdg-user-dir", "PICTURES").start().inputStream.bufferedReader().readText().trim() }.getOrNull()
        return File(xdg?.takeIf { it.isNotEmpty() } ?: "${System.getProperty("user.home")}/Pictures")
    }
}
