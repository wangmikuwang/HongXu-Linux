package io.wenyou.textquest.ui.common
import androidx.compose.material3.IconButton
import io.wenyou.textquest.ui.theme.LocalAppearance
import io.wenyou.textquest.ui.theme.LocalThemeStyle
import io.wenyou.textquest.ui.theme.ThemeStyle
import io.wenyou.textquest.ui.theme.distributedAccent
import io.wenyou.textquest.ui.theme.accentForeground
import io.wenyou.textquest.ui.theme.LocalAccentPalette


import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.contentColorFor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonDefaults
import io.wenyou.textquest.ui.theme.readableAccent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import io.wenyou.textquest.ui.common.AppText as Text
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    androidx.compose.material3.TextButton(onClick = onClick, modifier = modifier.pressMotion(interaction), interactionSource = interaction, enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.readableAccent()),
        content = content)
}

@Composable
fun AppOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    if (LocalThemeStyle.current == ThemeStyle.APPLE) {
        androidx.compose.material3.FilledTonalButton(onClick = onClick, modifier = modifier.pressMotion(interaction), interactionSource = interaction, enabled = enabled,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.readableAccent()), content = content)
        return
    }
    androidx.compose.material3.OutlinedButton(onClick = onClick, modifier = modifier.pressMotion(interaction), interactionSource = interaction, enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.readableAccent()),
        content = content)
}

/** 圆形 emoji 封面/头像（Material 圆角形态）。 */
@Composable
fun EmojiBadge(
    emoji: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    fontSize: TextUnit = 24.sp
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = color
    ) {
        Box(contentAlignment = Alignment.Center) {
            io.wenyou.textquest.ui.common.RawText(emoji, style = MaterialTheme.typography.titleLarge.copy(fontSize = fontSize))
        }
    }
}

/** 小圆角标签。 */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier, container: Color? = null, accentIndex: Int = 0) {
    val fill = container?.let { distributedAccent(accentIndex, it) }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = fill ?: MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        io.wenyou.textquest.ui.common.AppText(
            text,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
            color = if (container == null) MaterialTheme.colorScheme.onSurfaceVariant else if (LocalAccentPalette.current.isNotEmpty()) accentForeground(fill!!) else contentColorFor(container),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

/** Shared, theme-aware selection tags for library filters. */
@Composable
fun FilterTag(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, accentIndex: Int = 0) {
    val accent = distributedAccent(accentIndex, MaterialTheme.colorScheme.primary)
    val custom = LocalAccentPalette.current.isNotEmpty()
    FilterChip(selected = selected, onClick = onClick, modifier = modifier,
        leadingIcon = if (custom) {{
            if (selected) androidx.compose.material3.Icon(
                AppIcons.Check, contentDescription = null,
                modifier = Modifier.size(18.dp), tint = accentForeground(accent))
            else Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = accent,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {}
        }} else null,
        shape = RoundedCornerShape(50), label = { io.wenyou.textquest.ui.common.AppText(label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = accent,
            selectedLabelColor = if (custom) accentForeground(accent) else MaterialTheme.colorScheme.onPrimary))
}

/** 分组卡片标题。 */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = if (LocalThemeStyle.current == ThemeStyle.APPLE) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = if (LocalThemeStyle.current == ThemeStyle.APPLE) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(start = 4.dp, top = 12.dp, bottom = 6.dp)
    )
}

/** 带标题与说明的开关行（用于内容分类等布尔设置）。 */
@Composable
fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            io.wenyou.textquest.ui.common.RawText(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.size(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** 圆角色调容器卡片。 */
@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = if (LocalAppearance.current.listStyle == "rounded") RoundedCornerShape(24.dp) else MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

/** 文本输入框（支持多行与占位）。 */
@Composable
fun AppField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = 4,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    supporting: String = ""
) {
    // 保证 minLines <= maxLines，避免 Compose 抛 IllegalArgumentException（minLines<=maxLines 校验）
    val effMin = if (singleLine) 1 else minLines.coerceAtLeast(1)
    val effMax = if (singleLine) 1 else maxLines.coerceAtLeast(effMin)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        label = { Text(label) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = MaterialTheme.colorScheme.readableAccent(),
            focusedBorderColor = MaterialTheme.colorScheme.readableAccent(),
            cursorColor = MaterialTheme.colorScheme.readableAccent(),
            unfocusedBorderColor = if (LocalThemeStyle.current == ThemeStyle.APPLE) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline,
            focusedContainerColor = if (LocalThemeStyle.current == ThemeStyle.APPLE) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
            unfocusedContainerColor = if (LocalThemeStyle.current == ThemeStyle.APPLE) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent),
        singleLine = singleLine,
        minLines = effMin,
        maxLines = effMax,
        placeholder = if (placeholder.isNotBlank()) ({ io.wenyou.textquest.ui.common.AppText(placeholder) }) else null,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        supportingText = if (supporting.isNotBlank()) ({ io.wenyou.textquest.ui.common.AppText(supporting) }) else null
    )
}

/** Material3 ExposedDropdown：选择框。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AppDropdown(
    label: String,
    options: List<Pair<String, T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    if (LocalAppearance.current.popupStyle == "dialog") {
        Box(modifier) {
            OutlinedTextField(value = uiLabel(options.firstOrNull { it.second == selected }?.first ?: "", LocalAppearance.current.language), onValueChange = {},
                readOnly = true, enabled = enabled, singleLine = true, label = { Text(label) },
                modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { expanded = true })
            Box(Modifier.matchParentSize().clickable(enabled = enabled) { expanded = true })
        }
        if (expanded) androidx.compose.material3.AlertDialog(onDismissRequest = { expanded = false }, title = { Text(label) },
            text = { androidx.compose.foundation.lazy.LazyColumn { options.forEach { (name, value) -> item {
                Row(Modifier.fillMaxWidth().selectable(value == selected, role = Role.RadioButton, onClick = { onSelect(value); expanded = false }).padding(12.dp)) {
                    androidx.compose.material3.RadioButton(value == selected, onClick = null); io.wenyou.textquest.ui.common.AppText(name, Modifier.padding(start = 12.dp))
                }
            } } } }, confirmButton = { AppTextButton(onClick = { expanded = false }) { Text("关闭") } })
        return
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = uiLabel(options.firstOrNull { it.second == selected }?.first ?: "", LocalAppearance.current.language),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
        label = { Text(label) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedLabelColor = MaterialTheme.colorScheme.readableAccent(),
                focusedBorderColor = MaterialTheme.colorScheme.readableAccent()),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled).fillMaxWidth(),
            isError = error != null,
            supportingText = error?.let { { io.wenyou.textquest.ui.common.AppText(it) } }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (text, value) ->
                DropdownMenuItem(
                    text = { io.wenyou.textquest.ui.common.AppText(text) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** 彩色圆点取色选择。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorDots(
    colors: List<Color>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(modifier = modifier) {
        colors.forEachIndexed { index, color ->
            Box(
                Modifier.size(48.dp).selectable(index == selected, role = Role.RadioButton, onClick = { onSelect(index) })
                    .semantics { contentDescription = "颜色 ${index + 1}" },
                contentAlignment = Alignment.Center
            ) {
                Surface(shape = CircleShape, color = color,
                    modifier = Modifier.size(if (index == selected) 32.dp else 24.dp)) {}
            }
        }
    }
}

/** Rounded search box shared by settings and the story and character lists. */
@Composable
fun SearchField(query: String, onQueryChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(query, onQueryChange, modifier = modifier.fillMaxWidth(),
        singleLine = true, shape = MaterialTheme.shapes.medium,
        placeholder = { Text(placeholder) },
        leadingIcon = { AppIcon(AppIcons.Search, "搜索") },
        trailingIcon = if (query.isNotEmpty()) {{ IconButton(onClick = { onQueryChange("") }) { AppIcon(AppIcons.Close, "清除搜索") } }} else null,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.Transparent, focusedBorderColor = MaterialTheme.colorScheme.outline,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow))
}
