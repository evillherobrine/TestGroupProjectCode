package com.example.musicplayer.ui.screen.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.musicplayer.ui.navigation.AppDestinations

data class NavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
    val onClickAction: () -> Unit
)
@Composable
fun NavigationBar(
    navController: NavController,
    bottomBarAlpha: Float,
    bottomBarHeight: Dp,
    systemNavBarHeight: Dp,
    onHomeScroll: () -> Unit,
    onLocalScroll: () -> Unit,
    onLibraryScroll: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(bottomBarHeight + systemNavBarHeight)
            .graphicsLayer {
                alpha = bottomBarAlpha
                translationY = if (bottomBarAlpha == 0f) 1000f else 0f
            },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val items = listOf(
            NavItem(
                route = AppDestinations.HOME,
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                label = "Home",
                onClickAction = onHomeScroll
            ),
            NavItem(
                route = AppDestinations.LOCAL_MUSIC,
                selectedIcon = Icons.Filled.Folder,
                unselectedIcon = Icons.Outlined.Folder,
                label = "Local",
                onClickAction = onLocalScroll
            ),
            NavItem(
                route = AppDestinations.LIBRARY,
                selectedIcon = Icons.Filled.LibraryMusic,
                unselectedIcon = Icons.Outlined.LibraryMusic,
                label = "Library",
                onClickAction = onLibraryScroll
            ),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = systemNavBarHeight),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentDestination?.hierarchy?.any { destination ->
                    if (item.route == AppDestinations.LOCAL_MUSIC) {
                        destination.route == AppDestinations.LOCAL_MUSIC ||
                                destination.route?.startsWith(AppDestinations.LOCAL_ALBUM_DETAIL) == true ||
                                destination.route?.startsWith(AppDestinations.LOCAL_ARTIST_DETAIL) == true
                    } else {
                        destination.route == item.route
                    }
                } == true
                val contentColor = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (isSelected) {
                                if (currentDestination.route == item.route) {
                                    item.onClickAction()
                                } else {
                                    navController.navigate(item.route) {
                                        popUpTo(item.route) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = contentColor,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        color = contentColor,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal))
                }
            }
        }
    }
}

@Composable
fun NavigationRail(
    navController: NavController,
    modifier: Modifier = Modifier,
    onHomeScroll: () -> Unit,
    onLocalScroll: () -> Unit,
    onLibraryScroll: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    fun isSelected(route: String): Boolean {
        return currentDestination?.hierarchy?.any { destination ->
            if (route == AppDestinations.LOCAL_MUSIC) {
                destination.route == AppDestinations.LOCAL_MUSIC ||
                        destination.route?.startsWith(AppDestinations.LOCAL_ALBUM_DETAIL) == true ||
                        destination.route?.startsWith(AppDestinations.LOCAL_ARTIST_DETAIL) == true ||
                        destination.route?.startsWith(AppDestinations.LOCAL_FOLDER_DETAIL) == true
            } else {
                destination.route == route
            }
        } == true
    }
    NavigationRail(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Spacer(Modifier.weight(1f))
        val isHomeSelected = isSelected(AppDestinations.HOME)
        NavigationRailItem(
            selected = isHomeSelected,
            onClick = {
                if (isHomeSelected) {
                    if (currentDestination?.route == AppDestinations.HOME) onHomeScroll()
                } else {
                    navController.navigate(AppDestinations.HOME) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            },
            icon = {
                Icon(
                    if (isHomeSelected) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Home"
                )
            },
            label = { Text("Home") },
            colors = NavigationRailItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        )
        Spacer(Modifier.weight(1f))
        val isLocalSelected = isSelected(AppDestinations.LOCAL_MUSIC)
        NavigationRailItem(
            selected = isLocalSelected,
            onClick = {
                if (isLocalSelected) {
                    if (currentDestination?.route == AppDestinations.LOCAL_MUSIC) onLocalScroll()
                } else {
                    navController.navigate(AppDestinations.LOCAL_MUSIC) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            },
            icon = {
                Icon(
                    if (isLocalSelected) Icons.Filled.Folder else Icons.Outlined.Folder,
                    contentDescription = "Local"
                )
            },
            label = { Text("Local") },
            colors = NavigationRailItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        )
        Spacer(Modifier.weight(1f))
        val isLibrarySelected = isSelected(AppDestinations.LIBRARY)
        NavigationRailItem(
            selected = isLibrarySelected,
            onClick = {
                if (isLibrarySelected) {
                    if (currentDestination?.route == AppDestinations.LIBRARY) onLibraryScroll()
                } else {
                    navController.navigate(AppDestinations.LIBRARY) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            },
            icon = {
                Icon(
                    if (isLibrarySelected) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
                    contentDescription = "Library"
                )
            },
            label = { Text("Library") },
            colors = NavigationRailItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer
            ))
        Spacer(Modifier.weight(1f))
    }
}
