package io.wenyou.textquest.ui.theme

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import org.jetbrains.skia.Bitmap
import org.junit.Assert.assertTrue
import org.junit.Test

/** Emoji in the app font draw in colour even where the system has no emoji font (checked on a bare Debian). */
class EmojiRenderTest {
    @Test fun emojiDrawInColour() {
        val scene = ImageComposeScene(400, 120, Density(1f)) {
            RegisterEmojiFallback()
            BasicText("☕🌫🍷📖", style = TextStyle(fontFamily = DefaultAppFont, fontSize = 64.sp))
        }
        val bitmap = try { Bitmap.makeFromImage(scene.render()) } finally { scene.close() }
        var coloured = 0
        for (y in 0 until bitmap.height step 2) for (x in 0 until bitmap.width step 2) {
            val c = bitmap.getColor(x, y)
            val r = (c shr 16) and 0xFF; val g = (c shr 8) and 0xFF; val b = c and 0xFF
            if (maxOf(r, g, b) - minOf(r, g, b) > 60) coloured++
        }
        assertTrue("emoji rendered without colour ($coloured coloured samples)", coloured > 200)
    }
}
