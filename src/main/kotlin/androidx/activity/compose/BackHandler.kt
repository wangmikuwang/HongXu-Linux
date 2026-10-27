package androidx.activity.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/** Desktop stand-in for Android's back callbacks: Esc and the mouse back button ask the latest enabled handler first. */
object DesktopBack {
    class Handler(var enabled: Boolean, var onBack: () -> Unit)

    private val handlers = mutableListOf<Handler>()

    /** True when an on-screen handler took the back press. */
    fun dispatch(): Boolean {
        val handler = handlers.lastOrNull { it.enabled } ?: return false
        handler.onBack()
        return true
    }

    internal fun add(handler: Handler) { handlers += handler }
    internal fun remove(handler: Handler) { handlers -= handler }
}

@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    val current by rememberUpdatedState(onBack)
    val handler = remember { DesktopBack.Handler(enabled) { current() } }
    handler.enabled = enabled
    DisposableEffect(handler) {
        DesktopBack.add(handler)
        onDispose { DesktopBack.remove(handler) }
    }
}
