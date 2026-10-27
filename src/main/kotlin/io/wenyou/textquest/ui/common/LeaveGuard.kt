package io.wenyou.textquest.ui.common

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * Leaving an editor with unsaved changes (back button, gesture, Esc or the top bar arrow) asks before discarding them.
 * Returns the action for the top bar's back arrow.
 */
@Composable
fun rememberLeaveGuard(dirty: Boolean, onLeave: () -> Unit): () -> Unit {
    var asking by rememberSaveable { mutableStateOf(false) }
    // Back again while asking means "keep editing" (on desktop, Esc reaches this handler rather than the dialog).
    BackHandler(enabled = dirty) { asking = !asking }
    if (asking) AlertDialog(onDismissRequest = { asking = false },
        title = { AppText("放弃未保存的修改？") },
        text = { AppText("离开后，这次的修改不会保留。") },
        confirmButton = {
            AppTextButton(onClick = { asking = false; onLeave() }, modifier = Modifier.testTag("discard-changes")) {
                AppText("放弃修改", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { AppTextButton(onClick = { asking = false }) { AppText("继续编辑") } })
    return { if (dirty) asking = true else onLeave() }
}
