package io.wenyou.textquest.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Common flag designs; names are stable preference keys, not content classifications. */
enum class PrideTheme(val label: String, private val accentIndex: Int, vararg stripes: Long) {
    GAY("男同性恋", 0, 0xFF078D70, 0xFF26CEAA, 0xFF98E8C1, 0xFFFFFFFF, 0xFF7BADE2, 0xFF5049CC, 0xFF3D1A78),
    LESBIAN("女同性恋", 0, 0xFFD52D00, 0xFFFF9A56, 0xFFFFFFFF, 0xFFD162A4, 0xFFA30262),
    TRANS("跨性别", 0, 0xFF5BCEFA, 0xFFF5A9B8, 0xFFFFFFFF, 0xFFF5A9B8, 0xFF5BCEFA),
    RAINBOW("彩虹", 0, 0xFFE40303, 0xFFFF8C00, 0xFFFFED00, 0xFF008026, 0xFF24408E, 0xFF732982),
    BI("双性恋", 0, 0xFFD60270, 0xFF9B4F96, 0xFF0038A8),
    PAN("泛性恋", 0, 0xFFFF218C, 0xFFFFD800, 0xFF21B1FF),
    ASEXUAL("无性恋", 3, 0xFF000000, 0xFFA3A3A3, 0xFFFFFFFF, 0xFF800080),
    DEMISEXUAL("半性恋", 1, 0xFFFFFFFF, 0xFF800080, 0xFFA3A3A3),
    GRAYSEXUAL("灰性恋", 0, 0xFF740194, 0xFFAEB0AE, 0xFFFFFFFF, 0xFFAEB0AE, 0xFF740194),
    OMNI("全性恋", 1, 0xFFFF9ACE, 0xFFFF53BF, 0xFF200044, 0xFF6760FE, 0xFF8EA6FF),
    POLY("多性恋", 0, 0xFFF61CB9, 0xFF07D569, 0xFF1C92F6),
    ABRO("流动性取向", 0, 0xFF65C286, 0xFFB4E4CA, 0xFFFFFFFF, 0xFFF4A7B9, 0xFFE66591),
    AROMANTIC("无浪漫倾向", 0, 0xFF3DA542, 0xFFA7D379, 0xFFFFFFFF, 0xFFA9A9A9, 0xFF000000),
    NONBINARY("非二元", 2, 0xFFFFF430, 0xFFFFFFFF, 0xFF9C59D1, 0xFF000000),
    GENDERFLUID("性别流动", 0, 0xFFFF75A2, 0xFFFFFFFF, 0xFFBE18D6, 0xFF000000, 0xFF333EBD),
    GENDERQUEER("性别酷儿", 0, 0xFFB57EDC, 0xFFFFFFFF, 0xFF4A8123),
    AGENDER("无性别", 3, 0xFF000000, 0xFFB9B9B9, 0xFFFFFFFF, 0xFFB8F483, 0xFFFFFFFF, 0xFFB9B9B9, 0xFF000000),
    INTERSEX("间性", 1, 0xFFFFD800, 0xFF7902AA),
    UNLABELLED("不贴标签", 0, 0xFFB7D7A3, 0xFFF9F5E9, 0xFFB9D6F0, 0xFFF2E1A0),
    STRAIGHT("异性恋（黑白旗）", 0, 0xFF000000, 0xFFFFFFFF, 0xFF000000, 0xFFFFFFFF, 0xFF000000, 0xFFFFFFFF);

    val colors = stripes.map { Color(it) }
    val accent get() = colors[accentIndex]
    val description: String get() = when (this) {
        GAY -> "性取向：对男性产生情感或性吸引的男性也可认同这一身份。绿、青、白、蓝、紫七色旗代表男同性恋社群，涵盖跨性别和非二元成员，强调多元与归属。"
        LESBIAN -> "性取向：通常指被女性吸引的女性，也有非二元者使用此身份。橙、白、粉五色旗由日落旗简化而来，表达独立、社群、与女性身份的联系及爱。"
        TRANS -> "性别身份：自我性别与出生时被指定的性别不一致，不决定性取向。蓝与粉沿用传统性别色，白色包含非二元、过渡中或尚在探索的人；对称排列寓意自我认同。"
        RAINBOW -> "社群旗帜：代表整个 LGBTQ+ 社群，并非单一性取向。六色常解释为红色生命、橙色疗愈、黄色阳光、绿色自然、蓝色和谐、紫色精神，象征多元与共同的骄傲。"
        BI -> "性取向：可能对两种或更多性别产生吸引，不要求程度或方式相同。粉与蓝原指向同性及不同性别的吸引，交叠的紫色象征双性恋；身份并不限于二元性别。"
        PAN -> "性取向：可能被任何性别的人吸引，性别并非决定因素。粉、黄、蓝三色旗常分别联系女性、非二元者和男性，强调超越性别边界的吸引。"
        ASEXUAL -> "性取向：很少或不体验性吸引，不等于不能恋爱或选择禁欲。黑色象征无性恋，灰色涵盖灰性恋与半性恋，白色联系有性吸引者，紫色象征社群。"
        DEMISEXUAL -> "性取向：通常在形成深厚情感联系后才可能体验性吸引；建立联系不保证出现吸引。黑三角与白、紫、灰条纹沿用无性恋光谱色彩，表达半性恋的归属。"
        GRAYSEXUAL -> "性取向：在有性与无性体验之间，可能很少、较弱或只在特定情境下体验性吸引。紫、灰、白相间的旗帜是灰性恋及无性恋光谱的社群识别符号。"
        OMNI -> "性取向：可能对所有性别产生吸引，同时仍可能注意性别差异或有所偏好。粉、深紫、蓝组成的五色旗代表全性恋社群，强调多元吸引与自我表达。"
        POLY -> "性取向：可能对多种、但不一定所有性别产生吸引。粉、绿、蓝三色旗代表多性恋社群；它与描述关系安排的多伴侣关系并非同一概念。"
        ABRO -> "性取向：吸引的对象或体验可能随时间变化。绿、白、粉渐变旗是流动性取向社群的标志；逐色含义没有一致解释，不将后来的诠释当作统一定义。"
        AROMANTIC -> "浪漫取向：很少或不体验浪漫吸引，与是否体验性吸引不同。深浅绿象征无浪漫光谱，白色联系友谊及柏拉图式关系，灰黑涵盖不同的性吸引体验。"
        NONBINARY -> "性别身份：不完全属于男性或女性的二元划分。黄色指二元之外，白色指多种性别，紫色指男性与女性经验的混合，黑色指无性别；每个人的认同各不相同。"
        GENDERFLUID -> "性别身份：性别体验可能随时间流动，不等于性取向变化。粉、白、紫、黑、蓝五色旗涵盖女性、多个性别、混合性别、无性别及男性经验。"
        GENDERQUEER -> "性别身份：不受传统二元性别规范限制，可涵盖多种身份。紫色联系混合或模糊的性别经验，白色联系无性别，绿色联系二元之外。"
        AGENDER -> "性别身份：认同没有性别、性别中性或不以性别描述自己。黑白联系无性别，灰色联系部分无性别体验，绿色联系非二元经验；它不等于无性恋。"
        INTERSEX -> "身体性征：先天性征存在不符合典型男女二分的变异，不是性取向，也不规定性别身份。黄底避开传统粉蓝分类，紫色完整圆环象征完整、自主与不被缺损化。"
        UNLABELLED -> "自我描述：选择不用单一标签定义性取向或性别，也可能仍在探索。浅绿、米白、浅蓝、浅黄四色旗代表不贴标签的自我表达；不规定每种颜色的唯一含义。"
        STRAIGHT -> "性取向：通常指被不同性别的人吸引。黑白相间旗用于表示异性恋，但不是统一的 LGBTQ+ 社群或盟友旗；身份本身不代表是否支持平等。"
    }

    val stripeWeights get() = if (this == BI || this == DEMISEXUAL) listOf(2f, 1f, 2f) else colors.map { 1f }

    companion object {
        fun fromStored(raw: String?) = entries.firstOrNull { it.name == raw }
    }
}

/** Flag colors remain exact; only foreground text chooses black or white. */
internal fun prideColors(base: ColorScheme, theme: PrideTheme): ColorScheme {
    val colors = (listOf(theme.accent) + theme.colors + if (theme == PrideTheme.DEMISEXUAL) listOf(Color.Black) else emptyList()).distinct()
    fun accent(index: Int) = colors[index % colors.size]
    fun text(color: Color): Color {
        val luminance = color.luminance() + 0.05f
        return if (luminance / 0.05f >= 1.05f / luminance) Color.Black else Color.White
    }
    val primary = accent(0); val secondary = accent(1); val tertiary = accent(2)
    val primaryContainer = accent(3); val secondaryContainer = accent(4); val tertiaryContainer = accent(5)
    val inverse = accent(6)
    return base.copy(
        primary = primary, onPrimary = text(primary), primaryContainer = primaryContainer, onPrimaryContainer = text(primaryContainer),
        secondary = secondary, onSecondary = text(secondary), secondaryContainer = secondaryContainer, onSecondaryContainer = text(secondaryContainer),
        tertiary = tertiary, onTertiary = text(tertiary), tertiaryContainer = tertiaryContainer, onTertiaryContainer = text(tertiaryContainer),
        inversePrimary = inverse, inverseSurface = text(inverse), inverseOnSurface = if (text(inverse) == Color.Black) Color.White else Color.Black,
        surfaceTint = Color.Transparent
    )
}

/** Keep raw flag fills; text-only accents need contrast against neutral surfaces too. */
internal fun ColorScheme.readableAccent(accent: Color = primary): Color {
    val foreground = accent.luminance()
    val surfaces = listOf(background, surface, surfaceContainerLowest, surfaceContainerLow,
        surfaceContainer, surfaceContainerHigh, surfaceContainerHighest)
    return if (surfaces.all {
        val background = it.luminance()
        (maxOf(foreground, background) + 0.05f) / (minOf(foreground, background) + 0.05f) >= 4.5f
    }) accent else onSurface
}
