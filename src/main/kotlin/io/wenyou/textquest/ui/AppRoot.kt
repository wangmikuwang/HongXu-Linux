package io.wenyou.textquest.ui
import io.wenyou.textquest.ui.common.AppIcons
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.animateFloatAsState
import io.wenyou.textquest.ui.common.AppMotion
import io.wenyou.textquest.ui.common.LocalDockPosition
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import io.wenyou.textquest.ui.theme.readableAccent
import io.wenyou.textquest.ui.theme.distributedAccent
import io.wenyou.textquest.ui.theme.accentForeground
import io.wenyou.textquest.ui.theme.LocalAccentPalette


import io.wenyou.textquest.ui.common.AppTextButton

import androidx.lifecycle.viewmodel.compose.viewModel
import io.wenyou.textquest.ui.vm.AppUpdateViewModel
import io.wenyou.textquest.ui.common.AppUpdateHost

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import io.wenyou.textquest.ui.common.GlassBackdrop
import io.wenyou.textquest.ui.common.liquidGlass
import io.wenyou.textquest.ui.theme.LocalAppearance
import io.wenyou.textquest.ui.theme.LocalGlassEnabled
import io.wenyou.textquest.ui.common.DockItem
import io.wenyou.textquest.ui.common.GlassDock
import io.wenyou.textquest.ui.common.AppIcon as Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.fillMaxSize
import io.wenyou.textquest.ui.theme.LocalThemeStyle
import io.wenyou.textquest.ui.theme.ThemeStyle
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import io.wenyou.textquest.ui.common.AppText as Text
import androidx.compose.runtime.CompositionLocalProvider
import io.wenyou.textquest.ui.common.LocalPrideGalleryOpen
import io.wenyou.textquest.ui.common.PrideGallery
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.wenyou.textquest.WenYouApp
import io.wenyou.textquest.ui.screens.CharacterEditScreen
import io.wenyou.textquest.ui.screens.CharactersScreen
import io.wenyou.textquest.ui.screens.HomeScreen
import io.wenyou.textquest.ui.screens.PlayScreen
import io.wenyou.textquest.ui.screens.ProviderEditScreen
import io.wenyou.textquest.ui.screens.ProvidersScreen
import io.wenyou.textquest.ui.screens.SettingsScreen
import io.wenyou.textquest.ui.screens.StoryEditScreen
import io.wenyou.textquest.ui.screens.StoryListScreen
import io.wenyou.textquest.ui.theme.WenYouTheme

/** 全部路由。编辑/游玩页参数约定：`new` 表示新建。 */
object R {
    const val HOME = "home"
    const val STORIES = "stories"
    const val CREATE = "create"
    const val CHARACTERS = "characters"
    const val PROVIDERS = "providers"
    const val SETTINGS = "settings"
    const val APPEARANCE = "appearance"
    const val SETTINGS_DETAIL = "settings_detail/{category}"
    fun settingsDetail(category: String) = "settings_detail/$category"
    const val PRIDE = "pride"

    const val ARG_STORY = "storyId"
    const val ARG_SAVE = "saveId"
    const val ARG_CHAR = "charId"
    const val ARG_PROVIDER = "providerId"

    const val STORY_EDIT = "story_edit/{$ARG_STORY}"
    const val CHAR_EDIT = "char_edit/{$ARG_CHAR}"
    const val PROVIDER_EDIT = "provider_edit/{$ARG_PROVIDER}"
    const val PLAY = "play/{$ARG_STORY}/{$ARG_SAVE}"

    fun storyEdit(id: String) = "story_edit/$id"
    fun charEdit(id: String) = "char_edit/$id"
    fun providerEdit(id: String) = "provider_edit/$id"
    fun play(storyId: String, saveId: String = "new") = "play/$storyId/$saveId"

    val HUB_ROUTES = listOf(HOME, STORIES, CREATE, CHARACTERS, SETTINGS)
    val HUBS = HUB_ROUTES.toSet()
}

@Composable
fun WenYouAppRoot(container: WenYouApp.AppContainer, updateVm: AppUpdateViewModel = viewModel(), showStartup: Boolean = false) {
    val prefs by container.settings.state.collectAsStateWithLifecycle()
    WenYouTheme(prefs.themeMode, prefs.dynamicColor, prefs.themeStyle, prefs.prideTheme, prefs.appearance) {
        Box(Modifier.fillMaxSize()) {
        AppUpdateHost(updateVm) {
            val writeError by container.library.writeError.collectAsStateWithLifecycle()
            if (writeError != null) {
                AlertDialog(
                    onDismissRequest = container.library::clearWriteError,
                    title = { Text("保存失败") },
                    text = { io.wenyou.textquest.ui.common.RawText(writeError.orEmpty()) },
                    confirmButton = { AppTextButton(onClick = container.library::clearWriteError) { Text("知道了") } }
                )
            }
            // The real app looks for a copied share code whenever its window regains focus; tests skip this.
            if (showStartup) {
                val focused = androidx.compose.ui.platform.LocalWindowInfo.current.isWindowFocused
                val context = io.wenyou.textquest.platform.platformContext()
                LaunchedEffect(focused) { if (focused) io.wenyou.textquest.platform.checkClipboardForShare(context, container.shareInbox) }
            }
            io.wenyou.textquest.ui.common.SharedImportHost(container)
            val nav = rememberNavController()
            val entry by nav.currentBackStackEntryAsState()
            var dockIndex by remember { mutableStateOf(0) }
            LaunchedEffect(entry?.destination?.route) {
                val index = R.HUB_ROUTES.indexOf(entry?.destination?.route)
                if (index >= 0) dockIndex = index
            }
            val dockPosition = animateFloatAsState(dockIndex.toFloat(), AppMotion.selection(), label = "navigation-lens")
            CompositionLocalProvider(LocalDockPosition provides dockPosition, LocalPrideGalleryOpen provides { nav.navigate(R.PRIDE) { launchSingleTop = true } }) {
                NavHost(navController = nav, startDestination = R.HOME,
                    enterTransition = {
                        if (initialState.destination.route in R.HUBS && targetState.destination.route in R.HUBS) fadeIn(AppMotion.fade())
                        else slideInHorizontally(AppMotion.page()) { it } + fadeIn(AppMotion.fade())
                    },
                    exitTransition = {
                        if (initialState.destination.route in R.HUBS && targetState.destination.route in R.HUBS) fadeOut(AppMotion.fade())
                        else slideOutHorizontally(AppMotion.page()) { -it / 4 } + fadeOut(AppMotion.fade())
                    },
                    popEnterTransition = {
                        if (initialState.destination.route in R.HUBS && targetState.destination.route in R.HUBS) fadeIn(AppMotion.fade())
                        else slideInHorizontally(AppMotion.page()) { -it / 4 } + fadeIn(AppMotion.fade())
                    },
                    popExitTransition = {
                        if (initialState.destination.route in R.HUBS && targetState.destination.route in R.HUBS) fadeOut(AppMotion.fade())
                        else slideOutHorizontally(AppMotion.page()) { it } + fadeOut(AppMotion.fade())
                    }
                ) {
                    composable(R.HOME) { HomeScreen(container, nav) }
                    composable(R.STORIES) { StoryListScreen(container, nav) }
                    composable(
                        R.STORY_EDIT,
                        arguments = listOf(navArgument(R.ARG_STORY) { type = NavType.StringType })
                    ) { entry ->
                        val id = entry.arguments?.getString(R.ARG_STORY) ?: "new"
                        StoryEditScreen(container, nav, storyId = id)
                    }
                    composable(R.CREATE) { io.wenyou.textquest.ui.screens.CreationHubScreen(container, nav) }
                    composable(R.CHARACTERS) { CharactersScreen(container, nav) }
                    composable(
                        R.CHAR_EDIT,
                        arguments = listOf(navArgument(R.ARG_CHAR) { type = NavType.StringType })
                    ) { entry ->
                        val id = entry.arguments?.getString(R.ARG_CHAR) ?: "new"
                        CharacterEditScreen(container, nav, charId = id)
                    }
                    composable(R.PROVIDERS) { ProvidersScreen(container, nav) }
                    composable(
                        R.PROVIDER_EDIT,
                        arguments = listOf(navArgument(R.ARG_PROVIDER) { type = NavType.StringType })
                    ) { entry ->
                        val id = entry.arguments?.getString(R.ARG_PROVIDER) ?: "new"
                        ProviderEditScreen(container, nav, providerId = id)
                    }
                    composable(R.SETTINGS) { SettingsScreen(container, nav, updateVm) }
                    composable(R.SETTINGS_DETAIL, arguments = listOf(navArgument("category") { type = NavType.StringType })) { entry ->
                        SettingsScreen(container, nav, updateVm, category = entry.arguments?.getString("category") ?: "system")
                    }
                    composable(R.APPEARANCE) { io.wenyou.textquest.ui.screens.AppearanceScreen(container, nav) }
                    composable(R.PRIDE) {
                        PrideGallery(prefs.prideThemesUnlocked, prefs.prideThemesEnabled, prefs.showLgbt,
                            container.settings::unlockPrideThemes, container.settings::setPrideThemesEnabled,
                            container.settings::setShowLgbt, container.settings::setPrideTheme, { nav.popBackStack() }, prefs.prideTheme)
                    }
                    composable(
                        R.PLAY,
                        arguments = listOf(
                            navArgument(R.ARG_STORY) { type = NavType.StringType },
                            navArgument(R.ARG_SAVE) { type = NavType.StringType }
                        )
                    ) { entry ->
                        val storyId = entry.arguments?.getString(R.ARG_STORY).orEmpty()
                        val saveId = entry.arguments?.getString(R.ARG_SAVE) ?: "new"
                        PlayScreen(container, nav, storyId = storyId, saveId = saveId)
                    }
                }
            }
        }
        if (showStartup) io.wenyou.textquest.ui.common.StartupOverlay(prefs.appearance)
        io.wenyou.textquest.platform.MessageHost(Modifier.align(androidx.compose.ui.Alignment.BottomCenter).padding(bottom = 96.dp))
        }
    }
}

/** Hub 底部导航（Material 3 NavigationBar）。 */
@Composable
fun HubBottomBar(nav: NavHostController) {
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val items = listOf(
        HubItem(R.HOME, "主页", AppIcons.Home),
        HubItem(R.STORIES, "剧情", AppIcons.List),
        HubItem(R.CREATE, "创建", AppIcons.Add),
        HubItem(R.CHARACTERS, "角色", AppIcons.Person),
        HubItem(R.SETTINGS, "设置", AppIcons.Settings)
    )
    fun selectRoute(index: Int) {
        val route = items[index].route
        if (current == route) return
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    if (LocalGlassEnabled.current) {
        GlassDock(items.map { DockItem(it.label, it.icon) },
            items.indexOfFirst { it.route == current }.coerceAtLeast(0), ::selectRoute)
    } else {
        NavigationBar {
            items.forEachIndexed { index, item ->
                val indicator = distributedAccent(index + 2, MaterialTheme.colorScheme.primaryContainer)
                NavigationBarItem(
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = if (LocalAccentPalette.current.isNotEmpty()) accentForeground(indicator) else MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = indicator,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    selected = current == item.route,
                    onClick = { selectRoute(index) },
                    icon = { Icon(item.icon, contentDescription = item.label, tint = androidx.compose.material3.LocalContentColor.current) },
                    label = { io.wenyou.textquest.ui.common.AppText(item.label) }
                )
            }
        }
    }
}

/** 带底部导航的 Hub 页 Scaffold（顶部栏由各页决定：无则传 null）。 */
@Composable
fun HubScaffold(
    topBar: @Composable () -> Unit,
    nav: NavHostController,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    if (LocalGlassEnabled.current) {
        val density = LocalDensity.current
        var barHeight by remember { mutableStateOf(112.dp) }
        GlassBackdrop(
            content = {
                Scaffold(topBar = topBar, bottomBar = {
                    Spacer(Modifier.fillMaxWidth().height(barHeight))
                }) { padding -> content(padding) }
            },
            controls = {
                Box(Modifier.align(Alignment.BottomCenter)
                    .onSizeChanged { barHeight = with(density) { it.height.toDp() } }.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth().liquidGlass(pill = true, clipContent = false)) { HubBottomBar(nav) }
            }
        )
    } else {
        Scaffold(topBar = topBar, bottomBar = { HubBottomBar(nav) }) { padding -> content(padding) }
    }
}

private data class HubItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
