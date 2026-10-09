package io.wenyou.textquest.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/** Replacements for android.graphics.Color / ColorUtils must give the same answers. */
class ColorMathTest {
    @Test
    fun parsesLikeAndroid() {
        assertEquals(0xFF0066CC.toInt(), parseHexColor("#0066CC").toArgb())
        assertEquals(0x800066CC.toInt(), parseHexColor("#800066CC").toArgb())
    }

    @Test
    fun hsvRoundTrips() {
        assertArrayEquals(floatArrayOf(210f, 1f, 0.8f), parseHexColor("#0066CC").toHsv(), 0.01f)
        assertArrayEquals(floatArrayOf(0f, 0f, 0f), Color.Black.toHsv(), 0f)
        for (hex in listOf("#0066CC", "#FF6688", "#7E57C2", "#F9A825")) {
            val (h, s, v) = parseHexColor(hex).toHsv().toList()
            assertEquals(hex, "#%06X".format(Color.hsv(h, s, v).toArgb() and 0xFFFFFF))
        }
    }

    @Test
    fun contrastMatchesWcag() {
        assertEquals(21.0, contrastRatio(Color.Black, Color.White), 0.01)
        assertEquals(1.0, contrastRatio(Color.Red, Color.Red), 0.0001)
    }
}
