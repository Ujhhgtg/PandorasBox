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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.models.Module
import dev.ujhhgtg.pandorasbox.services.DlnaServerService
import dev.ujhhgtg.pandorasbox.services.InputMapperService
import dev.ujhhgtg.pandorasbox.services.OverlayService
import dev.ujhhgtg.pandorasbox.ui.composables.screens.AimBotScreen
import dev.ujhhgtg.pandorasbox.ui.composables.screens.DlnaServerScreen
import dev.ujhhgtg.pandorasbox.ui.composables.screens.FileManagerScreen
import dev.ujhhgtg.pandorasbox.ui.composables.screens.InputMapperScreen
import dev.ujhhgtg.pandorasbox.ui.composables.screens.ModulesScreen
import dev.ujhhgtg.pandorasbox.ui.composables.screens.OverlayScreen
import dev.ujhhgtg.pandorasbox.ui.composables.screens.PlaygroundScreen
import dev.ujhhgtg.pandorasbox.ui.composables.screens.SettingsScreen
import dev.ujhhgtg.pandorasbox.ui.theme.AppTheme
import dev.ujhhgtg.pandorasbox.utils.ReflectUtils
import dev.ujhhgtg.pandorasbox.utils.ServiceLocator
import dev.ujhhgtg.pandorasbox.utils.SettingsRepository

class MainActivity : ComponentActivity() {
    companion object {
        const val TAG: String = "PB.MainActivity"
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                AppContent()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppContent() {
        val settings = remember { SettingsRepository(this) }
        val navController = rememberNavController()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

        Scaffold(
            topBar = { TopAppBar(
                navigationIcon = {
                    val entry by navController.currentBackStackEntryAsState()
                    val canNavigateBack = remember(entry) {
                        navController.previousBackStackEntry != null
                    }
                    AnimatedVisibility(canNavigateBack) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                title = { Text(stringResource(R.string.app_name)) },
                scrollBehavior = scrollBehavior
            ) }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "modules",
                modifier = Modifier.padding(paddingValues = padding),
                enterTransition = {
                    fadeIn(animationSpec = tween(340))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(340))
                },
                builder = {
                    composable("overlay", deepLinks = listOf(navDeepLink { uriPattern = "pb://overlay" })) { OverlayScreen(settings, scrollBehavior, this@MainActivity) { toggleService<OverlayService>() } }
                    composable("aim_bot", deepLinks = listOf(navDeepLink { uriPattern = "pb://aim_bot" })) { AimBotScreen(this@MainActivity) }
                    composable("input_mapper", deepLinks = listOf(navDeepLink { uriPattern = "pb://input_mapper" })) { InputMapperScreen(this@MainActivity, scrollBehavior) { toggleService<InputMapperService>() } }
                    composable("dlna", deepLinks = listOf(navDeepLink { uriPattern = "pb://dlna" })) { DlnaServerScreen(this@MainActivity, scrollBehavior, pickAudio, pickVideo) { toggleService<DlnaServerService>() } }
                    composable("file_manager", deepLinks = listOf(navDeepLink { uriPattern = "pb://file_manager" })) { FileManagerScreen(this@MainActivity, "/storage/emulated/0") }
                    composable("playground", deepLinks = listOf(navDeepLink { uriPattern = "pb://playground" })) { PlaygroundScreen(this@MainActivity, scrollBehavior) }
                    composable("settings", deepLinks = listOf(navDeepLink { uriPattern = "pb://settings" })) { SettingsScreen(navController, padding, settings, scrollBehavior) }
                    composable("modules") { ModulesScreen(navController, modules) { id, labelId, descId, deepLink, iconId ->
                        this@MainActivity.requestPinnedShortcut(id, getString(labelId), getString(descId), deepLink, iconId)
                    } }
                }
            )
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

    private val pickAudio = registerForActivityResult(ActivityResultContracts.GetContent()) { it?.let(::playUri) }
    private val pickVideo = registerForActivityResult(ActivityResultContracts.GetContent()) { it?.let(::playUri) }

    private fun playUri(uri: Uri) {
        val type = contentResolver.getType(uri) ?: "application/octet-stream"
        val dlnaService = ServiceLocator.get(DlnaServerService::class.java)
        dlnaService?.serveAndPlayUri(contentResolver, uri, type)
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
            unselectedIcon = {
                Icon(
                    painter = painterResource(R.drawable.picture_in_picture_24px),
                    contentDescription = stringResource(R.string.overlay)
                )
            },
            selectedIcon = {
                Icon(
                    painter = painterResource(R.drawable.picture_in_picture_filled_24px),
                    contentDescription = stringResource(R.string.overlay)
                )
            },
            id = "overlay"
        ),
        Module(
            label = R.string.aim_bot,
            description = R.string.aim_bot_desc,
            unselectedIcon = {
                Icon(
                    painter = painterResource(R.drawable.center_focus_strong_24px),
                    contentDescription = stringResource(R.string.aim_bot)
                )
            },
            selectedIcon = {
                Icon(
                    painter = painterResource(R.drawable.center_focus_strong_filled_24px),
                    contentDescription = stringResource(R.string.aim_bot)
                )
            },
            id = "aim_bot"
        ),
        Module(
            label = R.string.input_mapper,
            description = R.string.input_mapper_desc,
            unselectedIcon = {
                Icon(
                    painter = painterResource(R.drawable.keyboard_external_input_24px),
                    contentDescription = stringResource(R.string.input_mapper)
                )
            },
            selectedIcon = {
                Icon(
                    painter = painterResource(R.drawable.keyboard_external_input_filled_24px),
                    contentDescription = stringResource(R.string.input_mapper)
                )
            },
            id = "input_mapper"
        ),
        Module(
            label = R.string.dlna,
            description = R.string.dlna_desc,
            unselectedIcon = {
                Icon(
                    painter = painterResource(R.drawable.cast_24px),
                    contentDescription = stringResource(R.string.dlna)
                )
            },
            selectedIcon = {
                Icon(
                    painter = painterResource(R.drawable.cast_filled_24px),
                    contentDescription = stringResource(R.string.dlna)
                )
            },
            id = "dlna"
        ),
        Module(
            label = R.string.file_manager,
            description = R.string.file_manager_desc,
            unselectedIcon = {
                Icon(
                    painter = painterResource(R.drawable.folder_open_24px),
                    contentDescription = stringResource(R.string.file_manager)
                )
            },
            selectedIcon = {
                Icon(
                    painter = painterResource(R.drawable.folder_open_filled_24px),
                    contentDescription = stringResource(R.string.file_manager)
                )
            },
            id = "file_manager"
        ),
        Module(
            label = R.string.playground,
            description = R.string.playground_desc,
            unselectedIcon = {
                Icon(
                    painter = painterResource(R.drawable.experiment_24px),
                    contentDescription = stringResource(R.string.playground)
                )
            },
            selectedIcon = {
                Icon(
                    painter = painterResource(R.drawable.experiment_filled_24px),
                    contentDescription = stringResource(R.string.playground)
                )
            },
            id = "playground"
        ),
        Module(
            label = R.string.settings,
            description = R.string.settings_desc,
            unselectedIcon = {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "settings"
                )
            },
            selectedIcon = {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "settings"
                )
            },
            id = "settings"
        )
    )

//    @Composable
//    private fun getCurrentRouteAsState(navController: NavHostController): String? {
//        val navBackStackEntry by navController.currentBackStackEntryAsState()
//        val currentRoute = navBackStackEntry?.destination?.route
//        return currentRoute
//    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Service> toggleService(): Boolean {
        if (!(ReflectUtils.getCompanionField<T>("isRunning") as MutableState<Boolean>).value) {
            startService(Intent(this, T::class.java))
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
