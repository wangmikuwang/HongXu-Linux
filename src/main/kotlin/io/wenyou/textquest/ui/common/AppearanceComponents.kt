package io.wenyou.textquest.ui.common

import io.wenyou.textquest.ui.theme.readableAccent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import io.wenyou.textquest.ui.theme.resolveFontWeight
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.wenyou.textquest.ui.theme.LocalAppearance

@Composable
fun AppText(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified, fontStyle: FontStyle? = null, fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null, letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null, textAlign: TextAlign? = null, lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip, softWrap: Boolean = true, maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1, onTextLayout: (TextLayoutResult) -> Unit = {}, style: TextStyle = LocalTextStyle.current) {
    val prefs = LocalAppearance.current
    androidx.compose.material3.Text(text = uiLabel(text = text, prefs.language), modifier = modifier, color = color, fontSize = fontSize, fontStyle = fontStyle,
        fontWeight = prefs.resolveFontWeight(fontWeight ?: style.fontWeight), fontFamily = fontFamily, letterSpacing = letterSpacing,
        textDecoration = textDecoration, textAlign = textAlign, lineHeight = lineHeight, overflow = overflow, softWrap = softWrap,
        maxLines = maxLines, minLines = minLines, onTextLayout = onTextLayout, style = style)
}

/** User-authored content never participates in interface translation. */
@Composable
fun RawText(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified, fontStyle: FontStyle? = null, fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null, letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null, textAlign: TextAlign? = null, lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip, softWrap: Boolean = true, maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1, onTextLayout: (TextLayoutResult) -> Unit = {}, style: TextStyle = LocalTextStyle.current) {
    val prefs = LocalAppearance.current
    androidx.compose.material3.Text(text = text, modifier = modifier, color = color, fontSize = fontSize, fontStyle = fontStyle,
        fontWeight = prefs.resolveFontWeight(fontWeight ?: style.fontWeight), fontFamily = fontFamily, letterSpacing = letterSpacing,
        textDecoration = textDecoration, textAlign = textAlign, lineHeight = lineHeight, overflow = overflow, softWrap = softWrap,
        maxLines = maxLines, minLines = minLines, onTextLayout = onTextLayout, style = style)
}

/** Icon preferences affect glyph color, never add a second background or shrink the glyph. */
@Composable
fun AppIcon(imageVector: ImageVector, contentDescription: String?, modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified) {
    val appearance = LocalAppearance.current
    val inherited = LocalContentColor.current
    val scheme = androidx.compose.material3.MaterialTheme.colorScheme
    val neutral = inherited == scheme.onSurface || inherited == scheme.onSurfaceVariant
    val resolved = if (tint != Color.Unspecified) tint
        else if (appearance.iconStyle == "color" && neutral && imageVector != AppIcons.ArrowBack)
            androidx.compose.material3.MaterialTheme.colorScheme.readableAccent()
        else LocalContentColor.current
    androidx.compose.material3.Icon(imageVector, contentDescription?.let { uiLabel(it, appearance.language) }, modifier, resolved)
}

@Composable
fun AppIcon(painter: Painter, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
    val appearance = LocalAppearance.current
    val inherited = LocalContentColor.current
    val scheme = androidx.compose.material3.MaterialTheme.colorScheme
    val neutral = inherited == scheme.onSurface || inherited == scheme.onSurfaceVariant
    val resolved = if (tint != Color.Unspecified) tint else if (appearance.iconStyle == "color" && neutral)
        androidx.compose.material3.MaterialTheme.colorScheme.readableAccent() else LocalContentColor.current
    androidx.compose.material3.Icon(painter, contentDescription?.let { uiLabel(it, appearance.language) }, modifier, resolved)
}
