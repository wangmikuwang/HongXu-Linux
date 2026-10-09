package io.wenyou.textquest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer

/** The bundled font is the Android app's subset (HongXu/third_party/lxgw-wenkai/subset.py); this edition's own text must never fall outside it. */
class FontCoverageTest {
    private val app = File(".")
    private val fontFile = File(app, "src/main/resources/bundled_kai_regular.ttf")

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

    @Test fun everyIdeographInTheAppsOwnTextIsInTheBundledFont() {
        val covered = coverage(ByteBuffer.wrap(fontFile.readBytes()))
        val sources = File(app, "src/main").walkTopDown().filter { f ->
            f.isFile && (f.extension == "kt" || f.extension == "md" || f.extension == "json" ||
                (f.name == "strings.xml" && f.parentFile?.name?.startsWith("values") == true))
        }
        val missing = sources.flatMap { ideographs(it.readText()) }.toSet() - covered
        assertEquals("Add these to the Android app's font subset (third_party/lxgw-wenkai/subset.py), then sync: " + missing.joinToString("") { String(Character.toChars(it)) },
            emptySet<Int>(), missing)
        assertTrue("Common simplified and traditional characters", "的一是了我你他她们说这那剧情角色導演選擇A，。".codePoints().allMatch { it in covered })
        assertTrue("Rare ideographs are left to the system font", 0x20000 !in covered)
    }

    @Test fun theBundledFontIsTheRenamedSubset() {
        assertTrue("Bundled font should be the ~6 MB subset", fontFile.length() < 10_000_000)
        assertTrue("OFL reserved names must not be used by the modified font",
            String(fontFile.readBytes(), Charsets.UTF_16BE).contains("Bundled Kai"))
    }
}
