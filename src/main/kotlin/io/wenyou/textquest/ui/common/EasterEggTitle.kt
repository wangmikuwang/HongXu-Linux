package io.wenyou.textquest.ui.common

import android.os.SystemClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import io.wenyou.textquest.ui.common.AppText as Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun EasterEggTitle(text: String, style: TextStyle, tapMessage: String, holdMessage: String) {
    var taps by remember { mutableIntStateOf(0) }
    var lastTap by remember { mutableLongStateOf(0L) }
    var surprise by rememberSaveable { mutableStateOf<String?>(null) }
    io.wenyou.textquest.ui.common.RawText(text, style = style, modifier = Modifier.heightIn(min = 48.dp).combinedClickable(
        onClickLabel = "连点五次探索彩蛋",
        onLongClickLabel = "探索彩蛋",
        onClick = {
            val now = SystemClock.elapsedRealtime()
            taps = if (now - lastTap > 2_000L) 1 else taps + 1
            lastTap = now
            if (taps >= 5) {
                taps = 0
                surprise = tapMessage
            }
        },
        onLongClick = { taps = 0; surprise = holdMessage }
    ))
    surprise?.let { message ->
        AlertDialog(onDismissRequest = { surprise = null },
            title = { Text("你发现了彩蛋！") },
            text = { io.wenyou.textquest.ui.common.RawText(message) },
            confirmButton = { AppTextButton(onClick = { surprise = null }) { Text("收下惊喜") } })
    }
}
