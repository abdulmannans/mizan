package app.mizan.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.mizan.android.ui.screens.AccountScreen
import app.mizan.android.ui.screens.FundDetailScreen
import app.mizan.android.ui.screens.FundsScreen
import app.mizan.android.ui.screens.HomeScreen
import app.mizan.android.ui.screens.MetalDetailScreen
import app.mizan.android.ui.screens.MetalsScreen
import app.mizan.android.ui.screens.MissedScreen
import app.mizan.android.ui.screens.OnboardingScreen
import app.mizan.android.ui.screens.WatchlistScreen

object Routes {
    const val HOME = "home"
    const val FUNDS = "funds"
    const val METALS = "metals"
    const val MISSED = "missed"
    const val WATCHLIST = "watchlist"
    const val ACCOUNT = "account"
    const val FUND_DETAIL = "fund/{schemeCode}"
    const val METAL_DETAIL = "metal/{metalId}"

    fun fundDetail(schemeCode: Long) = "fund/$schemeCode"
    fun metalDetail(metalId: String) = "metal/$metalId"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home),
    Tab(Routes.FUNDS, "Funds", Icons.AutoMirrored.Filled.ListAlt),
    Tab(Routes.METALS, "Metals", Icons.Filled.Diamond),
    Tab(Routes.MISSED, "Missed", Icons.AutoMirrored.Filled.TrendingDown),
)

@Composable
fun MizanApp(pendingRoute: String?, onRouteConsumed: () -> Unit) {
    val rootViewModel: RootViewModel = hiltViewModel()
    val acknowledged by rootViewModel.acknowledged.collectAsState()

    when (acknowledged) {
        null -> Box(Modifier.fillMaxSize())
        false -> OnboardingScreen()
        true -> MainShell(pendingRoute = pendingRoute, onRouteConsumed = onRouteConsumed)
    }
}

@Composable
private fun MainShell(pendingRoute: String?, onRouteConsumed: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var menuOpen by remember { mutableStateOf(false) }

    if (pendingRoute != null) {
        androidx.compose.runtime.LaunchedEffect(pendingRoute) {
            navController.navigate(pendingRoute)
            onRouteConsumed()
        }
    }

    val showBottomBar = tabs.any { it.route == currentRoute }

    Scaffold(
        topBar = {
            if (showBottomBar) {
                TopAppBar(
                    title = { Text("Mizan") },
                    actions = {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Watchlist") },
                                onClick = {
                                    menuOpen = false
                                    navController.navigate(Routes.WATCHLIST)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Account") },
                                onClick = {
                                    menuOpen = false
                                    navController.navigate(Routes.ACCOUNT)
                                },
                            )
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navController.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenFund = { navController.navigate(Routes.fundDetail(it)) },
                    onOpenMissed = { navController.switchTab(Routes.MISSED) },
                    onOpenFunds = { navController.switchTab(Routes.FUNDS) },
                )
            }
            composable(Routes.FUNDS) {
                FundsScreen(onOpenFund = { navController.navigate(Routes.fundDetail(it)) })
            }
            composable(Routes.METALS) {
                MetalsScreen(onOpenMetal = { navController.navigate(Routes.metalDetail(it)) })
            }
            composable(Routes.MISSED) {
                MissedScreen(onOpenFund = { navController.navigate(Routes.fundDetail(it)) })
            }
            composable(Routes.WATCHLIST) {
                WatchlistScreen(
                    onBack = { navController.popBackStack() },
                    onOpenFund = { navController.navigate(Routes.fundDetail(it)) },
                )
            }
            composable(Routes.ACCOUNT) {
                AccountScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.FUND_DETAIL) {
                FundDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.METAL_DETAIL) {
                MetalDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
