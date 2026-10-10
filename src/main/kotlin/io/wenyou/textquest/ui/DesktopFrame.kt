package io.wenyou.textquest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isBackPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import io.wenyou.textquest.data.repo.SettingsStore
import io.wenyou.textquest.ui.common.AppIcon
import io.wenyou.textquest.ui.common.AppText

/** True when the window is wide enough for the desktop layout: navigation rail on the left, no bottom dock. */
val LocalWideLayout = staticCompositionLocalOf { false }

/** Readable line length for page content on wide windows. */
private val MaxContentWidth = 880.dp

/** Set by [DesktopFrame]; the window forwards key presses that nothing on screen handled (Main.kt). */
var windowKeyHandler: (KeyEvent) -> Boolean = { false }

/**
 * Desktop keyboard and mouse: Esc and the mouse back button go back, Ctrl + = / - / 0 zoom the interface,
 * and wide windows get a navigation rail with page content kept to a readable width.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DesktopFrame(nav: NavHostController, settings: SettingsStore, content: @Composable () -> Unit) {
    DisposableEffect(nav, settings) {
        windowKeyHandler = { event -> event.type == KeyEventType.KeyDown && handleKey(event, nav, settings) }
        onDispose { windowKeyHandler = { false } }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .onPointerEvent(PointerEventType.Press) { if (it.buttons.isBackPressed) nav.popBackStack() }) {
        val wide = maxWidth >= 840.dp
        CompositionLocalProvider(LocalWideLayout provides wide) {
            Row(Modifier.fillMaxSize()) {
                if (wide) HubRail(nav)
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.widthIn(max = MaxContentWidth).fillMaxSize()) { content() }
                }
            }
        }
    }
}

private fun handleKey(event: KeyEvent, nav: NavHostController, settings: SettingsStore): Boolean {
    if (event.key == Key.Escape) return nav.previousBackStackEntry != null && nav.popBackStack()
    if (!event.isCtrlPressed) return false
    val step = when (event.key) {
        Key.Equals, Key.Plus, Key.NumPadAdd -> 10
        Key.Minus, Key.NumPadSubtract -> -10
        Key.Zero, Key.NumPad0 -> 0
        else -> return false
    }
    settings.updateAppearance { prefs ->
        val current = if (prefs.displayScale == 0) 100 else prefs.displayScale
        val next = if (step == 0) 100 else (current + step).coerceIn(80, 120)
        prefs.copy(displayScale = if (next == 100) 0 else next)
    }
    return true
}

@Composable
private fun HubRail(nav: NavHostController) {
    val current = nav.currentBackStackEntryAsState().value?.destination?.route
    NavigationRail {
        Spacer(Modifier.height(12.dp))
        hubItems.forEach { item ->
            NavigationRailItem(selected = current == item.route, onClick = { navigateHub(nav, item.route, current) },
                icon = { AppIcon(item.icon, contentDescription = item.label) }, label = { AppText(item.label) })
        }
    }
}
