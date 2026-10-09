package io.wenyou.textquest.ui.common

import io.wenyou.textquest.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WhatsNewTest {
    private fun file(vararg candidates: String) = candidates.map(::File).first(File::isFile)

    @Test fun bundledNotesMatchTheChangelogAndLeadWithThisVersion() {
        val changelog = file("../CHANGELOG.md", "CHANGELOG.md").readText()
        val bundled = file("src/main/assets/$WHATS_NEW_ASSET", "app/src/main/assets/$WHATS_NEW_ASSET").readText()
        assertEquals("Regenerate the changelog so the in-app notes match", changelog, bundled)
        val notes = parseChangelog(bundled)
        assertEquals(BuildConfig.VERSION_NAME.substringBefore('-'), notes.first().version)
        assertTrue(notes.size >= 4 && notes.take(4).all { it.items.isNotEmpty() })
    }

    @Test fun markdownIsReducedToPlainText() {
        val notes = parseChangelog("# 更新日志\n\n介绍\n\n## 2.0.0\n- **加粗** 与 [链接](https://example.com) 和 `代码`\n\n## 1.0.0\n- 首版\n")
        assertEquals(listOf(ReleaseNotes("2.0.0", listOf("加粗 与 链接 和 代码")), ReleaseNotes("1.0.0", listOf("首版"))), notes)
    }
}
