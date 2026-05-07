package com.neddy.watchlog.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.neddy.watchlog.R
import com.neddy.watchlog.ui.screens.detail.MediaDetailScreen
import com.neddy.watchlog.ui.screens.manualadd.ManualAddScreen
import com.neddy.watchlog.ui.screens.search.SearchScreen
import com.neddy.watchlog.ui.screens.settings.SettingsScreen
import com.neddy.watchlog.ui.screens.watchlist.WatchlistScreen

object Routes {
    const val WATCHLIST = "watchlist"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{mediaId}"
    const val MANUAL_ADD = "manual_add?mediaId={mediaId}"

    fun detail(mediaId: Long) = "detail/$mediaId"
    fun manualAdd(mediaId: Long? = null) =
        if (mediaId != null) "manual_add?mediaId=$mediaId" else "manual_add"
}

private data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val labelRes: Int
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.WATCHLIST, Icons.Filled.Home, R.string.home),
    BottomNavItem(Routes.SEARCH, Icons.Filled.FormatListBulleted, R.string.watchlist),
    BottomNavItem(Routes.SETTINGS, Icons.Filled.Settings, R.string.settings)
)

@Composable
fun AppNavigation(
    initialMediaId: Long? = null,
    onInitialMediaIdConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(initialMediaId) {
        initialMediaId?.let {
            navController.navigate(Routes.detail(it)) {
                launchSingleTop = true
            }
            onInitialMediaIdConsumed()
        }
    }

    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(tween(220, easing = EaseOut)) { it } + fadeIn(tween(220)),
                exit = shrinkVertically(tween(180, easing = EaseIn)) + fadeOut(tween(180))
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                            label = { Text(stringResource(item.labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.WATCHLIST,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
        ) {
            composable(Routes.WATCHLIST) {
                WatchlistScreen(
                    onNavigateToDetail = { id -> navController.navigate(Routes.detail(id)) }
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onNavigateToDetail = { id -> navController.navigate(Routes.detail(id)) },
                    onNavigateToAdd = { navController.navigate(Routes.manualAdd()) },
                    fabVisible = showBottomBar
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("mediaId") { type = NavType.LongType })
            ) {
                MediaDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id -> navController.navigate(Routes.manualAdd(id)) }
                )
            }
            composable(
                route = Routes.MANUAL_ADD,
                arguments = listOf(
                    navArgument("mediaId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) {
                ManualAddScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
