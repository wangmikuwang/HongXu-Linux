package io.wenyou.textquest

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.wenyou.textquest.ui.screens.*
import io.wenyou.textquest.ui.theme.WenYouTheme
import io.wenyou.textquest.ui.vm.AppUpdateViewModel
import androidx.compose.runtime.remember
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import io.wenyou.textquest.platform.appContext
import io.wenyou.textquest.platform.openAsset
import io.wenyou.textquest.platform.glassShader
import io.wenyou.textquest.ui.WenYouAppRoot
import io.wenyou.textquest.ui.common.DOCK_LENS_SHADER
import io.wenyou.textquest.ui.common.GLASS_SHADER
import io.wenyou.textquest.ui.theme.ThemeStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

/** What the desktop window provides: a resumed lifecycle and a ViewModel store. */
private class TestOwner : ViewModelStoreOwner, LifecycleOwner {
    override val viewModelStore = ViewModelStore()
    override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
}

/** Renders the real app root headlessly (no display needed) and saves it, so CI and reviewers can see it start. */
class RenderSmokeTest {
    @Test
    fun appRootRenders() {
        appContext = Context(createTempDirectory("hongxu-test").toFile())
        val container = WenYouApp.AppContainer(appContext)
        runBlocking { container.seedLibrary { openAsset(appContext, it) } }
        assertTrue(container.library.stories.value.isNotEmpty())

        // The UI thread on the desktop is the Swing event thread; navigation checks for it.
        runBlocking(Dispatchers.Main) { renderHome(container, "home.png") }
        container.settings.setThemeStyle(ThemeStyle.APPLE)
        runBlocking(Dispatchers.Main) { renderHome(container, "home-glass.png") }
    }

    /** The Android AGSL shaders must compile as Skia SkSL, or the glass silently falls back to a flat tint. */
    @Test
    fun glassShadersCompile() {
        assertNotNull(glassShader(GLASS_SHADER))
        assertNotNull(glassShader(DOCK_LENS_SHADER))
    }

    /** Each main screen composes and draws without throwing; the images are for a human look. */
    @Test
    fun screensRender() {
        appContext = Context(createTempDirectory("hongxu-test").toFile())
        val container = WenYouApp.AppContainer(appContext)
        runBlocking { container.seedLibrary { openAsset(appContext, it) } }
        val story = container.library.stories.value.first().id
        val updates = AppUpdateViewModel() // the app root passes its own; viewModel() cannot build one inside a nav entry on the JVM
        val screens: Map<String, @Composable (NavHostController) -> Unit> = mapOf(
            "stories" to { nav -> StoryListScreen(container, nav) },
            "characters" to { nav -> CharactersScreen(container, nav) },
            "create" to { nav -> CreationHubScreen(container, nav) },
            "settings" to { nav -> SettingsScreen(container, nav, updates) },
            "settings-system" to { nav -> SettingsScreen(container, nav, updates, category = "system") },
            "settings-backup" to { nav -> SettingsScreen(container, nav, updates, category = "backup") },
            "appearance" to { nav -> AppearanceScreen(container, nav) },
            "providers" to { nav -> ProvidersScreen(container, nav) },
            "provider-new" to { nav -> ProviderEditScreen(container, nav, "new") },
            "story-edit" to { nav -> StoryEditScreen(container, nav, story) },
            "play" to { nav -> PlayScreen(container, nav, story, "new") },
        )
        for ((name, screen) in screens) runBlocking(Dispatchers.Main) {
            render("screen-$name.png") {
                val prefs by container.settings.state.collectAsStateWithLifecycle()
                WenYouTheme(mode = prefs.themeMode, dynamicColor = prefs.dynamicColor, style = prefs.themeStyle, appearance = prefs.appearance) {
                    val nav = rememberNavController()
                    NavHost(nav, startDestination = "screen") { composable("screen") { screen(nav) } }
                }
            }
        }
    }

    private fun renderHome(container: WenYouApp.AppContainer, name: String) = render(name) { WenYouAppRoot(container) }

    private fun render(name: String, content: @Composable () -> Unit) {
        val scene = ImageComposeScene(480, 900, Density(1f)) {
            val owner = remember { TestOwner() }
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner, LocalLifecycleOwner provides owner, content = content)
        }
        try {
            for (frame in 0..30) scene.render(frame * 100_000_000L)
            val png = scene.render(3_200_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes
            File(System.getProperty("renderDir", "build"), name).apply { parentFile.mkdirs() }.writeBytes(png)
        } finally {
            scene.close()
        }
    }
}
