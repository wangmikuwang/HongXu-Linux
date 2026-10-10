package io.wenyou.textquest

import io.wenyou.textquest.ui.common.EnglishText
import io.wenyou.textquest.ui.common.toEnglish
import io.wenyou.textquest.ui.common.toTraditional
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Every interface string has an English translation; add new ones to ui/common/I18nEnglish.kt. */
class I18nCoverageTest {
    private val sources = listOf(File("src/main/java/io/wenyou/textquest"), File("app/src/main/java/io/wenyou/textquest"),
        File("src/main/kotlin/io/wenyou/textquest")).filter(File::isDirectory)

    /** Data-layer files whose Chinese text is shown to the player (the rest are AI prompts or story content). */
    private val dataFiles = setOf("data/AppInstall.kt", "data/AppUpdates.kt", "data/AppearanceFiles.kt", "data/engine/Achievements.kt",
        "data/engine/Transcript.kt", "data/llm/Catalog.kt", "data/llm/ChatClient.kt", "data/llm/DeepSeekPricing.kt",
        "data/llm/IslandPayload.kt", "data/llm/UsageTracker.kt", "data/model/Models.kt", "data/model/CharacterMetrics.kt",
        "data/repo/ShareCode.kt", "GenerationService.kt", "MainActivity.kt")

    /** Chinese text in those files that is sent to the AI, is story content, or is only seen by developers. */
    private val exempt = setOf(
        "以细腻的中文文学性叙述为主，第三人称，节奏自然。", "<story> · N 步", "ViewModel 缺少 APPLICATION_KEY",
        "请通过带 CreationExtras 的路径创建 ViewModel", ".* · \\\\d+ 步", "【前情提要】\n{0}", "测试日志 time={0}\nversion={1}\n",
        "文游", "未完的故事", "星叙", "英语（English）", "繁体中文",
        "\n\n【输出语言】本次回复中所有面向玩家的文字（正文、旁白、台词、选项、标题、简介、人物设定、总结、导演回复）都必须用{0}书写，",
        "即使上文的设定、风格或记录使用其他语言、或写着「中文」。JSON 字段名、节点与人物 id、[to:…] 等标记保持原样，不要翻译。",
        "本轮放慢节奏：只推进一个很小的动作或片刻，着重细节、感官描写、人物神态与对话往来；不跳过时间，不引入新的重大事件。",
        "本轮加快节奏：明显推进剧情，可略过过渡与琐碎细节、适当跳过时间，直接进入下一个关键事件或转折；叙述简洁。",
    )

    private val literal = Regex("\"((?:[^\"\\\\\\n]|\\\\.)*)\"")

    /** "${a} and $b" becomes "{0} and {1}", the key format of EnglishText. */
    private fun template(s: String): String {
        val out = StringBuilder(); var i = 0; var n = 0
        while (i < s.length) {
            if (s.startsWith("\\$", i)) { out.append('$'); i += 2 } // an escaped dollar is shown as is
            else if (s.startsWith("\${", i)) {
                var depth = 0; var j = i + 1
                while (j < s.length) { if (s[j] == '{') depth++ else if (s[j] == '}' && --depth == 0) break; j++ }
                out.append("{${n++}}"); i = j + 1
            } else if (s[i] == '$' && i + 1 < s.length && (s[i + 1].isLetter() || s[i + 1] == '_')) {
                var j = i + 1
                while (j < s.length && (s[j].isLetterOrDigit() || s[j] == '_')) j++
                out.append("{${n++}}"); i = j
            } else out.append(s[i++])
        }
        return out.toString().replace("\\n", "\n").replace("\\\"", "\"")
    }

    private fun interfaceStrings(): Map<String, String> {
        val found = linkedMapOf<String, String>()
        for (root in sources) root.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val rel = file.relativeTo(root).invariantSeparatorsPath
            val isInterface = (rel.startsWith("ui/") && !rel.startsWith("ui/common/I18n")) || rel in dataFiles
            if (!isInterface || rel.contains("Preview")) return@forEach
            file.readLines().forEachIndexed { index, line ->
                if (line.trimStart().startsWith("//") || line.contains("@Preview")) return@forEachIndexed
                literal.findAll(line).map { template(it.groupValues[1]) }
                    .filter { s -> s.any { it.code in 0x4E00..0x9FFF } && s !in exempt }
                    .forEach { found.putIfAbsent(it, "$rel:${index + 1}") }
            }
        }
        return found
    }

    @Test fun everyInterfaceStringHasEnglish() {
        val missing = interfaceStrings().filterKeys { it !in EnglishText }
        assertTrue("Add English for these to ui/common/I18nEnglish.kt:\n" +
            missing.entries.joinToString("\n") { (text, at) -> "$at  $text" }, missing.isEmpty())
    }

    @Test fun templatesFillInValues() {
        assertEquals("Version 1.2.0", toEnglish("新版 1.2.0"))
        assertEquals("Imported: 3 new items", toEnglish("导入成功：新增 3 条内容"))
        // A value that is itself interface text is translated too; story content is left alone.
        assertEquals("Story title is incomplete or too long; generate again", toEnglish("剧情名 不完整或过长，请重新生成"))
        assertEquals("雨夜咖啡馆", toEnglish("雨夜咖啡馆"))
    }

    @Test fun traditionalUsesTaiwanWording() {
        assertEquals("設定", toTraditional("设置"))
        assertEquals("匯入備份", toTraditional("导入备份"))
        assertEquals("劇情庫", toTraditional("剧情库"))
        assertEquals("Ollama（本地免費）", toTraditional("Ollama（本地免费）"))
    }
}
