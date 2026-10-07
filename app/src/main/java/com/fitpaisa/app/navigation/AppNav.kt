package com.fitpaisa.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fitpaisa.app.core.auth.SessionViewModel
import com.fitpaisa.app.feature.home.HomeScreen
import com.fitpaisa.app.feature.levels.LevelsScreen
import com.fitpaisa.app.feature.profile.ProfileScreen
import com.fitpaisa.app.feature.signin.SignInScreen
import com.fitpaisa.app.feature.wallet.WalletScreen
import com.fitpaisa.app.feature.welcome.WelcomeScreen
import com.fitpaisa.app.ui.theme.Gold
import com.fitpaisa.app.ui.theme.MutedSage
import com.fitpaisa.app.ui.theme.NavBarGreen
import kotlinx.coroutines.launch

object Routes {
    const val WELCOME = "welcome"
    const val SIGNIN = "signin"
    const val HOME = "home"
    const val WALLET = "wallet"
    const val LEVELS = "levels"
    const val PROFILE = "profile"
    const val REWARDS = "rewards"
    const val ADMIN = "admin"
}

private data class Tab(
    val route: String,
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
)

private val TABS = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    Tab(Routes.REWARDS, "Rewards", Icons.Filled.CardGiftcard, Icons.Outlined.CardGiftcard),
    Tab(Routes.WALLET, "Wallet", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    Tab(Routes.LEVELS, "Levels", Icons.Filled.Layers, Icons.Outlined.Layers),
    Tab(Routes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
)

private val TAB_ROUTES = TABS.map { it.route }.toSet()

/**
 * App navigation (v2). Cold-start routing:
 *  - welcome not accepted -> Welcome
 *  - no session          -> SignIn
 *  - session             -> Home (with bottom nav)
 * The bottom bar is visible on the four tab destinations only.
 */
@Composable
fun AppNav(
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        startDestination = when {
            !sessionViewModel.isWelcomeAccepted() -> Routes.WELCOME
            sessionViewModel.isSignedIn() -> Routes.HOME
            else -> Routes.SIGNIN
        }
    }

    val start = startDestination
    if (start == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Gold)
        }
        return
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in TAB_ROUTES

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                BottomTabs(
                    currentRoute = currentRoute,
                    onTabSelected = { route -> navController.navigateToTab(route) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.WELCOME) {
                WelcomeScreen(
                    onGetStarted = {
                        scope.launch {
                            sessionViewModel.setWelcomeAccepted()
                            navController.navigate(Routes.SIGNIN) {
                                popUpTo(Routes.WELCOME) { inclusive = true }
                            }
                        }
                    }
                )
            }
            composable(Routes.SIGNIN) {
                SignInScreen(
                    onSignedIn = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SIGNIN) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.HOME) {
                HomeScreen()
            }
            composable(Routes.REWARDS) {
                com.fitpaisa.app.feature.rewards.RewardsScreen()
            }
            composable(Routes.WALLET) {
                WalletScreen()
            }
            composable(Routes.LEVELS) {
                LevelsScreen()
            }
            composable(Routes.ADMIN) {
                com.fitpaisa.app.feature.admin.AdminScreen()
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onSignedOut = {
                        navController.navigate(Routes.SIGNIN) {
                            // Clear everything: Back from SignIn exits the app.
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

private fun NavController.navigateToTab(route: String) {
    navigate(route) {
        // Tabs never stack on each other; back always exits from Home.
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomTabs(
    currentRoute: String?,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(containerColor = NavBarGreen) {
        TABS.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.filledIcon else tab.outlinedIcon,
                        contentDescription = tab.label
                    )
                },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Gold,
                    selectedTextColor = Gold,
                    unselectedIconColor = MutedSage,
                    unselectedTextColor = MutedSage,
                    indicatorColor = Gold.copy(alpha = 0.15f)
                )
            )
        }
    }
}
