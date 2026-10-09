package io.wenyou.textquest.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.wenyou.textquest.data.AppearanceFiles
import io.wenyou.textquest.platform.appIconPainter
import io.wenyou.textquest.platform.appName
import io.wenyou.textquest.platform.loadImage
import io.wenyou.textquest.platform.platformContext
import io.wenyou.textquest.ui.theme.AppearancePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun WallpaperPreview(key: String, modifier: Modifier = Modifier) {
    val context = platformContext()
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, key) {
        value = withContext(Dispatchers.IO) { AppearanceFiles.wallpaper(context, key)?.let(::loadImage) }
    }
    val colors = when (key) {
        "paper" -> listOf(Color(0xFFF2EEE5), Color(0xFFD8C7BB), Color(0xFF9B829F))
        "dawn" -> listOf(Color(0xFFFC9B87), Color(0xFFFBD9AF), Color(0xFF9CCEC9))
        "stars" -> listOf(Color(0xFF070B28), Color(0xFF404890), Color(0xFF8965A9))
        else -> listOf(Color(0xFF12151D), Color(0xFF3C315C), Color(0xFF716185))
    }
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(colors))) {
        bitmap?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

/** Startup effects run once per activity launch, with no repeating/background timer. */
@Composable
fun StartupOverlay(prefs: AppearancePrefs) {
    var visible by rememberSaveable { mutableStateOf(prefs.splashEnabled || prefs.splashAnimation) }
    var exit by remember { mutableStateOf(false) }
    val scale = animateFloatAsState(if (exit && prefs.splashAnimation) 1.12f else 1f, tween(220), label = "startup-icon")
    val opacity = animateFloatAsState(if (exit && prefs.splashAnimation) 0f else 1f, tween(220), label = "startup-mask")
    val selected = remember { if (prefs.splashRandom) (listOf("ink", "paper", "dawn", "stars") + listOf(prefs.splashWallpaper)).distinct().random() else prefs.splashWallpaper }
    LaunchedEffect(Unit) {
        if (visible) { delay(if (prefs.splashEnabled) 700 else 250); exit = true; if (prefs.splashAnimation) delay(220); visible = false }
    }
    if (visible) Box(Modifier.fillMaxSize().graphicsLayer { alpha = opacity.value }
        .background(MaterialTheme.colorScheme.background).clickable { visible = false }, contentAlignment = Alignment.Center) {
        if (prefs.splashEnabled) WallpaperPreview(selected, Modifier.fillMaxSize())
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(appIconPainter(), null,
                Modifier.size(112.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value })
            Spacer(Modifier.height(16.dp))
            AppText(appName(), style = MaterialTheme.typography.headlineLarge,
                color = if (prefs.splashEnabled) Color.White else MaterialTheme.colorScheme.onSurface)
        }
    }
}
