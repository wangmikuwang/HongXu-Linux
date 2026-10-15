package io.wenyou.textquest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer

/** The bundled LXGW WenKai weights are the complete, unmodified fonts (same files as the Android app), covering all of this edition's text. */
class FontCoverageTest {
    private val app = File(".")
    private val fonts = listOf("lxgw_wenkai_regular.ttf", "lxgw_wenkai_medium.ttf").map { File(app, "src/main/resources/$it") }

    /** Code points mapped by the font's format 12 (full Unicode) cmap subtable. */
    private fun coverage(font: ByteBuffer): Set<Int> {
        val tables = font.getShort(4).toInt()
        val cmap = (0 until tables).map { 12 + it * 16 }.first { font.getInt(it) == 0x636D6170 /* "cmap" */ }
            .let { font.getInt(it + 8) }
        val subtables = (0 until font.getShort(cmap + 2).toInt()).map { cmap + font.getInt(cmap + 8 + it * 8) }
        val table = subtables.first { font.getShort(it).toInt() == 12 }
        return (0 until font.getInt(table + 12)).flatMapTo(HashSet()) {
            val group = table + 16 + it * 12
            font.getInt(group)..font.getInt(group + 4)
        }
    }

    private fun ideographs(text: String) = text.codePoints().toArray().filter {
        it in 0x3400..0x9FFF || it in 0xF900..0xFAFF || it in 0xAC00..0xD7AF || it >= 0x20000
    }

    @Test fun everyIdeographInTheAppsOwnTextIsInBothWeights() {
        val sources = File(app, "src/main").walkTopDown().filter { f ->
            f.isFile && (f.extension == "kt" || f.extension == "md" || f.extension == "json" ||
                (f.name == "strings.xml" && f.parentFile?.name?.startsWith("values") == true))
        }
        val used = sources.flatMap { ideographs(it.readText()) }.toSet()
        for (font in fonts) {
            val covered = coverage(ByteBuffer.wrap(font.readBytes()))
            assertEquals("${font.name} lacks: " + (used - covered).joinToString("") { String(Character.toChars(it)) }, emptySet<Int>(), used - covered)
            // The complete font has about 30,000 ideographs, beyond GB2312/Big5 and into Extension B (e.g. U+2000B).
            assertTrue("${font.name} is complete, rare ideographs included", covered.count { ideographs(String(Character.toChars(it))).isNotEmpty() } > 30_000 &&
                0x2000B in covered && "導演選擇".codePoints().allMatch { it in covered })
        }
    }

    @Test fun bundledWeightsAreTheUnmodifiedOriginals() {
        // Unmodified copies keep the original family name; any change would make them Modified Versions under the OFL.
        for (font in fonts) assertTrue("${font.name} keeps the original LXGW WenKai name",
            String(font.readBytes(), Charsets.UTF_16BE).contains("LXGW WenKai"))
    }
}
