package io.wenyou.textquest

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.wenyou.textquest.platform.appContext
import io.wenyou.textquest.platform.openAsset
import io.wenyou.textquest.ui.DesktopFrame
import io.wenyou.textquest.ui.WenYouAppRoot
import io.wenyou.textquest.ui.screens.CreationHubScreen
import io.wenyou.textquest.ui.screens.StoryListScreen
import io.wenyou.textquest.ui.theme.ThemeStyle
import io.wenyou.textquest.ui.theme.WenYouTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

/** Store screenshots at the default window size: ./gradlew test --tests '*StoreScreenshots' -PstoreScreenshots=packaging/flatpak/screenshots */
class StoreScreenshots {
    private class Owner : ViewModelStoreOwner, LifecycleOwner {
        override val viewModelStore = ViewModelStore()
        override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
    }

    @Test fun render() {
        val out = System.getProperty("storeScreenshots")
        assumeTrue("set -PstoreScreenshots=<dir> to render", !out.isNullOrBlank())
        appContext = Context(createTempDirectory("hongxu-shots").toFile())
        val container = WenYouApp.AppContainer(appContext)
        runBlocking { container.seedLibrary { openAsset(appContext, it) } }
        fun shot(name: String, content: @Composable () -> Unit) = runBlocking(Dispatchers.Main) {
            val scene = ImageComposeScene(1120, 780, Density(1f)) {
                val owner = remember { Owner() }
                CompositionLocalProvider(LocalViewModelStoreOwner provides owner, LocalLifecycleOwner provides owner, content = content)
            }
            try {
                for (frame in 0..30) scene.render(frame * 100_000_000L)
                File(out, "$name.png").apply { parentFile.mkdirs() }.writeBytes(scene.render(3_200_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            } finally { scene.close() }
        }
        @Composable fun page(screen: @Composable (androidx.navigation.NavHostController) -> Unit) {
            val prefs by container.settings.state.collectAsStateWithLifecycle()
            WenYouTheme(mode = prefs.themeMode, dynamicColor = prefs.dynamicColor, style = prefs.themeStyle, appearance = prefs.appearance) {
                val nav = rememberNavController()
                DesktopFrame(nav, container.settings) { NavHost(nav, startDestination = "page") { composable("page") { screen(nav) } } }
            }
        }
        shot("home") { WenYouAppRoot(container) }
        shot("stories") { page { StoryListScreen(container, it) } }
        shot("create") { page { CreationHubScreen(container, it) } }
        container.settings.setThemeStyle(ThemeStyle.APPLE)
        shot("glass") { page { StoryListScreen(container, it) } }
    }
}
