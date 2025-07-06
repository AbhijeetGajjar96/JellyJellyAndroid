package com.example.jellyjelly1.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.shadow

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.jellyjelly1.ui.feed.FeedScreen
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.MaterialTheme

@Composable
fun BottomNavigationBar(
    items: List<BottomNavScreen>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 1.dp
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                    selected = selectedIndex == index,
                    onClick = { onTabSelected(index) },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.route,
                            tint = if (selectedIndex == index) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            item.label,
                            color = if (selectedIndex == index) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    },
                    alwaysShowLabel = true
                )
        }
    }
}



sealed class BottomNavScreen(val route: String, val icon: ImageVector, val label: String) {
    object Feed : BottomNavScreen("feed", Icons.Filled.Home, "Home")
    object Notifications : BottomNavScreen("notifications", Icons.Filled.Notifications, "Notifications")
    object Camera : BottomNavScreen("camera", Icons.Filled.Camera, "Camera")
    object Library : BottomNavScreen("library", Icons.Filled.Collections, "Library")
    object Wallet : BottomNavScreen("wallet", Icons.Filled.AccountBalance, "Wallet")
}