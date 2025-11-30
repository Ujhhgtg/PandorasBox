package dev.ujhhgtg.pandorasbox.ui.activities

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.models.DEFAULT_HOME_URL
import dev.ujhhgtg.pandorasbox.models.Module
import dev.ujhhgtg.pandorasbox.models.SettingItem
import dev.ujhhgtg.pandorasbox.services.DlnaServerService
import dev.ujhhgtg.pandorasbox.services.DownloadService
import dev.ujhhgtg.pandorasbox.services.InputMapperService
import dev.ujhhgtg.pandorasbox.services.OverlayService
import dev.ujhhgtg.pandorasbox.ui.composables.Text
import dev.ujhhgtg.pandorasbox.ui.composables.screens.AimBotScreen
import dev.ujhhgtg.pandorasbox.ui.composables.screens.BrowserScreen
import dev.ujhhgtg.pandorasbox.ui.composables.screens.CrosshairOverlayScreen
import dev.ujhhgtg.pandorasbox.ui.composables.screens.DlnaServerScreen
import dev.ujhhgtg.pandorasbox.ui.composables.screens.FileManagerScreen
import dev.ujhhgtg.pandorasbox.ui.composables.screens.GalleryOrganizerScreen
import dev.ujhhgtg.pandorasbox.ui.composables.screens.GalleryOrganizingScreen
import dev.ujhhgtg.pandorasbox.ui.composables.screens.InputMapperScreen
import dev.ujhhgtg.pandorasbox.ui.composables.screens.ModulesScreen
import dev.ujhhgtg.pandorasbox.ui.composables.screens.PlaygroundScreen
import dev.ujhhgtg.pandorasbox.ui.composables.settingsGraph
import dev.ujhhgtg.pandorasbox.ui.theme.AppTheme
import dev.ujhhgtg.pandorasbox.utils.PermissionManager
import dev.ujhhgtg.pandorasbox.utils.ReflectUtils
import dev.ujhhgtg.pandorasbox.utils.settings.HistoryRepository
import dev.ujhhgtg.pandorasbox.utils.settings.PrefsRepository
import dev.ujhhgtg.pandorasbox.utils.settings.PrefsRepository.Companion.bKey
import dev.ujhhgtg.pandorasbox.utils.settings.PrefsRepository.Companion.iKey
import dev.ujhhgtg.pandorasbox.utils.tooltip

val LocalTopBarSetter = compositionLocalOf<((@Composable () -> Unit)?) -> Unit> { {} }
val LocalBottomBarSetter = compositionLocalOf<((@Composable () -> Unit)?) -> Unit> { {} }
val LocalNavController = compositionLocalOf<NavController> { error("Not provided") }
val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState> { error("Not provided") }
val LocalActivityContext = compositionLocalOf<ComponentActivity> { error("Not provided") }
@OptIn(ExperimentalMaterial3Api::class)
val LocalScrollBehavior = compositionLocalOf<TopAppBarScrollBehavior> { error("Not provided") }
val LocalPrefsRepository = compositionLocalOf<PrefsRepository> { error("Not provided") }
val LocalHistoryRepository = compositionLocalOf<HistoryRepository> { error("Not provided") }

class MainActivity : ComponentActivity() {
    companion object {
        const val TAG: String = "PB.MainActivity"
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppContent()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppContent() {
        val prefs = remember { PrefsRepository(this) }
        val history = remember { HistoryRepository(this) }
        val navController = rememberNavController()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
        val topBarState = remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
        val bottomBarState = remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
        val snackbarHostState = remember { SnackbarHostState() }
        val haptic = LocalHapticFeedback.current

        val darkMode by prefs.rememberPreference(iKey("dark_mode"), 0)
        val dynamicColors by prefs.rememberPreference(bKey("dynamic_colors"), true)

        LaunchedEffect(Unit) {
            PermissionManager.checkAndRequestNotifications(this@MainActivity)
            toggleService<DownloadService>()
        }

        AppTheme(
            darkTheme = when (darkMode) {
                0 -> isSystemInDarkTheme()
                1 -> true
                2 -> false
                else -> error("does not exist")
            },
            dynamicColor = dynamicColors
        ) {
//        MiuixTheme(
//            colors = when (darkMode) {
//                0 -> if (isSystemInDarkTheme()) top.yukonga.miuix.kmp.theme.darkColorScheme() else top.yukonga.miuix.kmp.theme.lightColorScheme()
//                1 -> top.yukonga.miuix.kmp.theme.darkColorScheme()
//                2 -> top.yukonga.miuix.kmp.theme.lightColorScheme()
//                else -> error("does not exist")
//            }
//        ) {
            Scaffold(
                snackbarHost = {
                    val currentRoute = getCurrentRouteAsState(navController)
                    SnackbarHost(snackbarHostState, modifier = Modifier
                        .padding(bottom = if (currentRoute == "browser") 91.dp else 0.dp)
                        .fillMaxWidth(),
                        snackbar = {
                            Snackbar(
                                modifier = Modifier.alpha(0.7f),
                                snackbarData = it,
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                actionColor = MaterialTheme.colorScheme.primary,
                                actionContentColor = MaterialTheme.colorScheme.primary,
                                dismissActionContentColor = MaterialTheme.colorScheme.onSurface)
                        }
                    ) },
                topBar = {
                    AnimatedContent(topBarState.value) {
                        it?.invoke() ?: TopAppBar(
                            navigationIcon = {
                                val entry by navController.currentBackStackEntryAsState()
                                val canNavigateBack = remember(entry) {
                                    navController.previousBackStackEntry != null
                                }
                                AnimatedVisibility(canNavigateBack) {
                                    IconButton(onClick = { navController.popBackStack()
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress) }) {
                                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                            contentDescription = stringResource(R.string.back),
                                            modifier = Modifier.tooltip(stringResource(R.string.back)))
                                    }
                                }
                            },
                            title = { Text(R.string.app_name) },
                            scrollBehavior = scrollBehavior
                        )
                    }
                },
                bottomBar = {
                    AnimatedContent(bottomBarState.value) {
                        it?.invoke()
                    }
                }
            ) { padding ->
                CompositionLocalProvider(
                    LocalTopBarSetter provides { topBarState.value = it },
                    LocalBottomBarSetter provides { bottomBarState.value = it },
                    LocalSnackbarHostState provides snackbarHostState,
                    LocalNavController provides navController,
                    LocalActivityContext provides this,
                    LocalScrollBehavior provides scrollBehavior,
                    LocalPrefsRepository provides prefs,
                    LocalHistoryRepository provides history
                ) {
                    var showClearDataDialog by remember { mutableStateOf(false) }

                    NavHost(
                        navController = navController,
                        startDestination = "modules",
                        modifier = Modifier.padding(paddingValues = padding),
                        enterTransition = {
//                            fadeIn(animationSpec = tween(340))
                            slideInHorizontally(initialOffsetX = { it })
                        },
                        exitTransition = {
//                            fadeOut(animationSpec = tween(340))
                            slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut()
                        },
                        popEnterTransition = {
                            slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn()
                        },
                        popExitTransition = {
                            scaleOut(targetScale = 0.9f) + fadeOut()
                        },
                        builder = {
                            composable("overlay", deepLinks = listOf(navDeepLink { uriPattern = "pb://overlay" })) { CrosshairOverlayScreen { toggleService<OverlayService>() } }
                            composable("aim_bot", deepLinks = listOf(navDeepLink { uriPattern = "pb://aim_bot" })) { AimBotScreen() }
                            composable("input_mapper", deepLinks = listOf(navDeepLink { uriPattern = "pb://input_mapper" })) { InputMapperScreen { toggleService<InputMapperService>() } }
                            composable("dlna", deepLinks = listOf(navDeepLink { uriPattern = "pb://dlna" })) { DlnaServerScreen { toggleService<DlnaServerService>() } }
                            composable("file_manager", deepLinks = listOf(navDeepLink { uriPattern = "pb://file_manager" })) { FileManagerScreen("/storage/emulated/0") }
                            composable("browser", deepLinks = listOf(navDeepLink { uriPattern = "pb://browser" })) { BrowserScreen() }
                            composable("gallery_organizer", deepLinks = listOf(navDeepLink { uriPattern = "pb://gallery_organizer" })) { GalleryOrganizerScreen() }
                            composable("gallery_organizer_operation/{folderPath}", arguments = listOf(
                                navArgument("folderPath") { type = NavType.StringType }
                            )) { backStackEntry ->
                                val path = backStackEntry.arguments?.getString("folderPath")?.let { Uri.decode(it) }
                                if (path != null) {
                                    GalleryOrganizingScreen(folderPath = path)
                                } else {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Invalid Folder Path", color = androidx.compose.ui.graphics.Color.Red)
                                    }
                                }
                            }
                            composable("playground", deepLinks = listOf(navDeepLink { uriPattern = "pb://playground" })) { PlaygroundScreen() }
                            composable("modules") { ModulesScreen(navController, modules) { id, labelId, descId, deepLink, iconId ->
                                this@MainActivity.requestPinnedShortcut(id, getString(labelId), getString(descId), deepLink, iconId)
                            } }
                            settingsGraph(navController,
                                listOf(
                                    SettingItem.Action(getString(R.string.clear_data), { showClearDataDialog = true }, icon = { Icon(Icons.Default.Clear, null) },
                                        content = {
                                            var clearCookies by remember { mutableStateOf(true) }
                                            var clearCache by remember { mutableStateOf(true) }
                                            var clearLocalStorage by remember { mutableStateOf(false) }
                                            var clearPermissions by remember { mutableStateOf(false) }

                                            if (showClearDataDialog) {
                                                AlertDialog(
                                                    onDismissRequest = { showClearDataDialog = false },
                                                    title = { Text(R.string.clear_data) },
                                                    text = {
                                                        Column {
                                                            ListItem(headlineContent = { Text(R.string.cookies) },
                                                                trailingContent = { Checkbox(checked = clearCookies, onCheckedChange = { clearCookies = it }) },
                                                                modifier = Modifier.clickable { clearCookies = !clearCookies })
                                                            ListItem(headlineContent = { Text(R.string.cache) },
                                                                trailingContent = { Checkbox(checked = clearCache, onCheckedChange = { clearCache = it }) },
                                                                modifier = Modifier.clickable { clearCache = !clearCache })
                                                            ListItem(headlineContent = { Text(R.string.local_storage) },
                                                                trailingContent = { Checkbox(checked = clearLocalStorage, onCheckedChange = { clearLocalStorage = it }) },
                                                                modifier = Modifier.clickable { clearLocalStorage = !clearLocalStorage })
                                                            ListItem(headlineContent = { Text(R.string.site_permissions) },
                                                                trailingContent = { Checkbox(checked = clearPermissions, onCheckedChange = { clearPermissions = it }) },
                                                                modifier = Modifier.clickable { clearPermissions = !clearPermissions })
                                                        }
                                                    },
                                                    confirmButton = {
                                                        TextButton(onClick = {
                                                            if (clearCookies) {
                                                                val cm = CookieManager.getInstance()
                                                                cm.removeAllCookies(null)
                                                                cm.flush()
                                                            }
                                                            if (clearCache) {
                                                                WebView(this@MainActivity).clearCache(true)
                                                            }
                                                            if (clearLocalStorage) {
                                                                WebStorage.getInstance().deleteAllData()
                                                            }
                                                            if (clearPermissions) {
                                                                GeolocationPermissions.getInstance().clearAll()
                                                            }
                                                            showClearDataDialog = false
                                                        }) {
                                                            Text(R.string.clear)
                                                        }
                                                    },
                                                    dismissButton = {
                                                        TextButton(onClick = { showClearDataDialog = false }) { Text(R.string.cancel) }
                                                    }
                                                )
                                            }
                                        }),
                                    SettingItem.Input("Home page URL", defaultValue = DEFAULT_HOME_URL, validator = { it.isNotEmpty() }, icon = { Icon(
                                        Icons.Default.Home, null) })
                                ), "browser_settings")
                            settingsGraph(navController,
                                listOf(
                                    SettingItem.SubPage(
                                        label = "Appearance",
                                        children = listOf(
                                            SettingItem.Selection("Dark mode", listOf("System", "On", "Off"), icon = { Icon(painterResource(R.drawable.dark_mode_24px), null) }),
                                            SettingItem.Toggle("Dynamic colors", true, icon = { Icon(painterResource(R.drawable.colors_24px), null) })
                                        ),
                                        icon = { Icon(painterResource(R.drawable.palette_24px), null) }
                                    )
                                ), "settings", listOf(navDeepLink { uriPattern = "pb://settings" }))
                        }
                    )
                }
            }
        }
    }

    fun Context.requestPinnedShortcut(
        id: String,
        shortLabel: String,
        longLabel: String,
        uri: String,
        iconRes: Int
    ) {
        val shortcutManager = getSystemService(ShortcutManager::class.java)

        val intent = Intent(
            Intent.ACTION_VIEW,
            uri.toUri(),
            this,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val shortcut = ShortcutInfo.Builder(this, id)
            .setShortLabel(shortLabel)
            .setLongLabel(longLabel)
            .setIcon(Icon.createWithResource(this, iconRes))
            .setIntent(intent)
            .build()

        if (shortcutManager.isRequestPinShortcutSupported) {
            shortcutManager.requestPinShortcut(shortcut, null)
        }
    }

//    @Composable
//    fun BottomAppNavBar(navController: NavHostController, modules: List<Module>) {
//        val currentRoute = getCurrentRouteAsState(navController)
//
//        NavigationBar(
//            containerColor = MaterialTheme.colorScheme.surfaceContainer,
//            contentColor = MaterialTheme.colorScheme.onSurface
//        ) {
//            modules.slice(0..3).forEach { navItem ->
//                NavigationBarItem(
//                    colors = NavigationBarItemDefaults.colors().copy(
//                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
//                        selectedIndicatorColor = MaterialTheme.colorScheme.secondaryContainer
//                    ),
//                    selected = currentRoute == navItem.id,
//                    onClick = {
//                        navController.navigate(navItem.id) {
//                            popUpTo("modules") {
//                                saveState = true
//                            }
//                            launchSingleTop = true
//                            restoreState = true
//                        }
//                    },
//                    icon = {
////                        AnimatedContent(
////                            targetState = currentRoute,
////                            transitionSpec = { fadeIn() togetherWith fadeOut() }
////                        ) {
////                            if (it == navItem.route) {
//                        if (currentRoute == navItem.id) {
//                            navItem.selectedIcon()
//                        } else {
//                            navItem.unselectedIcon()
//                        }
////                        }
//                    },
//                    label = {
//                        Text(text = stringResource(navItem.label))
//                    },
//                    alwaysShowLabel = true
//                )
//            }
//        }
//    }

    val modules = listOf(
        Module(
            label = R.string.overlay,
            description = R.string.overlay_desc,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.picture_in_picture_24px),
                    contentDescription = null
                )
            },
            id = "overlay"
        ),
        Module(
            label = R.string.aim_bot,
            description = R.string.aim_bot_desc,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.center_focus_strong_24px),
                    contentDescription = null
                )
            },
            id = "aim_bot"
        ),
        Module(
            label = R.string.input_mapper,
            description = R.string.input_mapper_desc,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.keyboard_external_input_24px),
                    contentDescription = null
                )
            },
            id = "input_mapper"
        ),
        Module(
            label = R.string.dlna,
            description = R.string.dlna_desc,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.cast_24px),
                    contentDescription = null
                )
            },
            id = "dlna"
        ),
        Module(
            label = R.string.file_manager,
            description = R.string.file_manager_desc,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.folder_open_24px),
                    contentDescription = null
                )
            },
            id = "file_manager"
        ),
        Module(
            label = R.string.browser,
            description = R.string.browser_desc,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.language_24px),
                    contentDescription = null
                )
            },
            id = "browser"
        ),
        Module(
            label = R.string.gallery_organizer,
            description = R.string.gallery_organizer_desc,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.gallery_thumbnail_24px),
                    contentDescription = null
                )
            },
            id = "gallery_organizer"
        ),
        Module(
            label = R.string.playground,
            description = R.string.playground_desc,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.experiment_24px),
                    contentDescription = null
                )
            },
            id = "playground"
        ),
        Module(
            label = R.string.settings,
            description = R.string.settings_desc,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null
                )
            },
            id = "settings"
        )
    )

    @Composable
    private fun getCurrentRouteAsState(navController: NavController): String? {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        return currentRoute
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Service> toggleService(): Boolean {
        if (!(ReflectUtils.getCompanionField<T>("isRunning") as MutableState<Boolean>).value) {
            startForegroundService(Intent(this, T::class.java))
            Log.d(TAG, "started service ${T::class.simpleName}")
            return true
        }
        else {
            stopService(Intent(this, T::class.java))
            Log.d(TAG, "stopped service ${T::class.simpleName}")
            return false
        }
    }
}
