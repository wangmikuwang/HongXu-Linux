package io.wenyou.textquest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import io.wenyou.textquest.ui.common.AppIcon as Icon
import io.wenyou.textquest.ui.common.AppText as Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.wenyou.textquest.data.model.CharacterData
import io.wenyou.textquest.ui.common.AppTextButton
import io.wenyou.textquest.ui.common.EmojiBadge
import io.wenyou.textquest.ui.theme.avatarColor

@Composable
internal fun RoleSelectionDialog(characters: List<CharacterData>, onStart: (String) -> Unit, onCancel: () -> Unit) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    RoleSelectionContent(characters, selectedId, { selectedId = it }, onStart, onCancel)
}

@Composable
internal fun RoleSelectionContent(characters: List<CharacterData>, selectedId: String?, onSelect: (String) -> Unit,
    onStart: (String) -> Unit, onCancel: () -> Unit) {
    AlertDialog(onDismissRequest = onCancel, title = { Text("选择扮演的角色") }, text = {
        Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("这段旅程中，你将以谁的身份行动？", style = MaterialTheme.typography.bodyMedium)
            RoleRow("", "自由身份", "以自己的身份探索剧情", "👤", 0, selectedId, onSelect)
            characters.forEach { c -> RoleRow(c.id, c.name, c.tagline.ifBlank { c.personality }, c.emoji, c.colorIndex, selectedId, onSelect) }
        }
    }, confirmButton = {
        Button(enabled = selectedId != null, modifier = Modifier.testTag("role-start"), onClick = { selectedId?.let(onStart) }) { Text("开始剧情") }
    }, dismissButton = { AppTextButton(onClick = onCancel) { Text("取消") } })
}

@Composable
private fun RoleRow(id: String, name: String, description: String, emoji: String, colorIndex: Int,
    selectedId: String?, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("role-${id.ifBlank { "free" }}")
        .selectable(selected = selectedId == id, role = Role.RadioButton, onClick = { onSelect(id) }).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        EmojiBadge(emoji, avatarColor(colorIndex), size = 40.dp)
        Column(Modifier.weight(1f)) {
            io.wenyou.textquest.ui.common.RawText(name, style = MaterialTheme.typography.titleSmall)
            if (description.isNotBlank()) io.wenyou.textquest.ui.common.RawText(description, style = MaterialTheme.typography.bodySmall, maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RadioButton(selected = selectedId == id, onClick = null)
    }
}
