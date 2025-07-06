package com.example.jellyjelly1.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.jellyjelly1.ui.feed.FeedScreen
import com.example.jellyjelly1.ui.notifications.NotificationsScreen
import com.example.jellyjelly1.ui.camera.CameraScreen
import com.example.jellyjelly1.ui.cameraroll.CameraRollScreen
import com.example.jellyjelly1.ui.wallet.WalletScreen
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    val navItems = listOf(
        BottomNavScreen.Feed,
        BottomNavScreen.Camera,
        BottomNavScreen.Library,
    )

    var selectedIndex by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                items = navItems,
                selectedIndex = selectedIndex,
                onTabSelected = { index ->
                    selectedIndex = index
                    navController.navigate(navItems[index].route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        JellyNavHost(
            modifier = Modifier.padding(innerPadding),
            navController = navController
        )
    }
}

@Composable
fun JellyNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavScreen.Feed.route,
        modifier = modifier
    ) {
        composable(BottomNavScreen.Feed.route) { FeedScreen() }
        composable(BottomNavScreen.Notifications.route) { NotificationsScreen() }
        composable(BottomNavScreen.Camera.route) { CameraScreen() }
        composable(BottomNavScreen.Library.route) { CameraRollScreen() }
        composable(BottomNavScreen.Wallet.route) { WalletScreen() }
    }
}