package io.wenyou.textquest.ui.common

import java.util.Locale

/*
 * Interface translation. Interface text is written in Simplified Chinese; AppText (and icon descriptions) pass it
 * through [uiLabel]. English comes from EnglishText, Traditional Chinese from a character table plus Taiwan wording.
 * Story content, characters, imported data and AI output are shown with RawText and never translated here.
 */

/** "zh-CN", "zh-TW" or "en" for a stored preference ("system", "zh-CN", "zh-TW", "en"). */
fun resolveLanguage(preference: String): String {
    val tag = if (preference == "system") Locale.getDefault().toLanguageTag() else preference
    return when {
        tag.startsWith("zh-TW") || tag.startsWith("zh-HK") || tag.startsWith("zh-MO") || tag.startsWith("zh-Hant") -> "zh-TW"
        tag.startsWith("zh") -> "zh-CN"
        // English is the only complete non-Chinese translation, so other system languages get English.
        else -> "en"
    }
}

/** The interface language preference currently applied, for text shown outside composition (toasts, notifications). */
@Volatile var appLanguage: String = "system"

/** Translates interface text outside composition, using [appLanguage]. */
fun tr(text: String): String = uiLabel(text, appLanguage)

/** Translates a template such as "已用{0}秒" (an EnglishText key) and then fills in [values], which stay as given. */
fun trf(template: String, vararg values: Any?): String =
    values.foldIndexed(tr(template)) { i, text, value -> text.replace("{$i}", value.toString()) }

internal fun uiLabel(text: String, language: String): String = when (resolveLanguage(language)) {
    "en" -> toEnglish(text)
    "zh-TW" -> toTraditional(text)
    else -> text
}

private fun hasChinese(text: String) = text.any { it.code in 0x3400..0x9FFF }

/** Exact entries first, then templates whose "{n}" parts are filled with (translated) values; unknown text is kept. */
internal fun toEnglish(text: String): String {
    if (!hasChinese(text)) return text
    EnglishText[text]?.let { return it }
    for ((pattern, english) in englishTemplates) {
        val match = pattern.matchEntire(text) ?: continue
        return match.groupValues.drop(1).foldIndexed(english) { i, out, value -> out.replace("{$i}", toEnglish(value)) }
    }
    return text
}

private val englishTemplates: List<Pair<Regex, String>> by lazy {
    EnglishText.filterKeys { it.contains("{0}") }.map { (key, english) ->
        val parts = key.split(Regex("\\{\\d\\}")) // Android's ICU regex rejects an unescaped "}"
        Regex(parts.joinToString("(.+?)") { Regex.escape(it) }, RegexOption.DOT_MATCHES_ALL) to english
    }.sortedByDescending { it.first.pattern.length }
}

internal fun toTraditional(text: String): String {
    if (!hasChinese(text)) return text
    val out = StringBuilder(text.length)
    var i = 0
    outer@ while (i < text.length) {
        for ((simplified, taiwan) in TaiwanPhrases) {
            if (text.startsWith(simplified, i)) { out.append(taiwan); i += simplified.length; continue@outer }
        }
        out.append(TraditionalChars[text[i]] ?: text[i])
        i++
    }
    return out.toString()
}
