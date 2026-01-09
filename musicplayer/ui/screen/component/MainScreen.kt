package com.example.musicplayer.ui.screen.component

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.musicplayer.ui.navigation.AppDestinations
import com.example.musicplayer.ui.navigation.AppNavHost
import com.example.musicplayer.ui.screen.player.MiniPlayerHeight
import com.example.musicplayer.ui.screen.player.PlayerScaffold
import com.example.musicplayer.ui.screen.player.QueueScreen
import com.example.musicplayer.ui.screen.player.SleepTimerDialog
import com.example.musicplayer.viewmodel.MainViewModel
import com.example.musicplayer.viewmodel.playback.PlayerUiState
import com.example.musicplayer.viewmodel.playback.PlayerViewModel
import com.example.musicplayer.viewmodel.search.SearchViewModel
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@UnstableApi
fun MainScreen(
    playerViewModel: PlayerViewModel = viewModel(),
    searchViewModel: SearchViewModel = viewModel(),
    mainViewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val playerState by playerViewModel.uiState.collectAsState()
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true))
    val hostState = remember { SnackbarHostState() }
    val showSnackbar: (String) -> Unit = { message ->
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            hostState.showSnackbar(message)
        }
    }
    HandleDialogsAndSheets(
        mainViewModel = mainViewModel,
        playerViewModel = playerViewModel,
        playerState = playerState,
        showSnackbar = showSnackbar)
    val systemNavBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val customNavBarHeight = 56.dp
    val totalBottomBarHeight = if (isLandscape) 0.dp else customNavBarHeight + systemNavBarHeight
    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            MainNavigationRail(
                navController = navController,
                modifier = Modifier
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical)
            ),
            onHomeScroll = mainViewModel::scrollToTopHome,
            onLocalScroll = mainViewModel::scrollToTopLocal,
            onLibraryScroll = mainViewModel::scrollToTopLibrary
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.End))
            ) {
                PlayerScaffoldContent(
                    playerState = playerState,
                    scaffoldState = scaffoldState,
                    hostState = hostState,
                    navController = navController,
                    searchViewModel = searchViewModel,
                    mainViewModel = mainViewModel,
                    playerViewModel = playerViewModel,
                    showSnackbar = showSnackbar,
                    bottomBarHeight = 0.dp,
                    miniPlayerBottomPadding = 0.dp
                )
            }
        }
    } else {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))
        ) {
            val screenHeight = maxHeight
            val peekHeight = MiniPlayerHeight + totalBottomBarHeight
            val bottomBarAlpha by remember(scaffoldState.bottomSheetState) {
                derivedStateOf {
                    try {
                        val offset = scaffoldState.bottomSheetState.requireOffset()
                        val maxOffset = with(density) { (screenHeight - peekHeight).toPx() }
                        val progress = if (maxOffset > 0) (1f - (offset / maxOffset)).coerceIn(0f, 1f) else 0f
                        1f - progress
                    } catch (_: Exception) { 1f }
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                PlayerScaffoldContent(
                    playerState = playerState,
                    scaffoldState = scaffoldState,
                    hostState = hostState,
                    navController = navController,
                    searchViewModel = searchViewModel,
                    mainViewModel = mainViewModel,
                    playerViewModel = playerViewModel,
                    showSnackbar = showSnackbar,
                    bottomBarHeight = customNavBarHeight,
                    miniPlayerBottomPadding = totalBottomBarHeight
                )
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    NavigationBar(
                        navController = navController,
                        bottomBarAlpha = bottomBarAlpha,
                        bottomBarHeight = customNavBarHeight,
                        systemNavBarHeight = systemNavBarHeight,
                        onHomeScroll = mainViewModel::scrollToTopHome,
                        onLocalScroll = mainViewModel::scrollToTopLocal,
                        onLibraryScroll = mainViewModel::scrollToTopLibrary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@UnstableApi
private fun PlayerScaffoldContent(
    playerState: PlayerUiState,
    scaffoldState: BottomSheetScaffoldState,
    hostState: SnackbarHostState,
    navController: NavHostController,
    searchViewModel: SearchViewModel,
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel,
    showSnackbar: (String) -> Unit,
    bottomBarHeight: Dp,
    miniPlayerBottomPadding: Dp
) {
    PlayerScaffold(
        playerState = playerState,
        scaffoldState = scaffoldState,
        snackbarHostState = hostState,
        bottomBarHeight = bottomBarHeight,
        onPlayPause = playerViewModel::togglePlayPause,
        onNext = playerViewModel::nextSong,
        onPrev = playerViewModel::prevSong,
        onSeek = playerViewModel::seekTo,
        onToggleFavorite = playerViewModel::toggleFavorite,
        onToggleRepeat = playerViewModel::toggleRepeat,
        onShowQueue = mainViewModel::openQueue,
        onShowSleepTimer = {
            mainViewModel.openSleepTimer(playerState.sleepTimerInMillis != null && playerState.sleepTimerInMillis > 0)
        },
        onShowSongOptions = { song -> song?.let { mainViewModel.openSongOptions(it) } }
    ) { innerPadding, miniPlayerHeight ->
        AppNavHost(
            navController = navController,
            scaffoldPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = miniPlayerBottomPadding
            ),
            miniPlayerHeight = miniPlayerHeight,
            onShowSongOptions = mainViewModel::openSongOptions,
            onShowSnackbar = showSnackbar,
            searchViewModel = searchViewModel,
            scrollToTopHome = mainViewModel.homeScrollTrigger,
            scrollToTopLibrary = mainViewModel.libraryScrollTrigger,
            scrollToTopLocal = mainViewModel.localScrollTrigger,
            scrollToTopHistory = 0L
        )
    }
}

@Composable
private fun MainNavigationRail(
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
        header = {
            Spacer(modifier = Modifier.padding(top = 8.dp))
        }
    ) {
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
            )
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@UnstableApi
private fun HandleDialogsAndSheets(
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel,
    playerState: PlayerUiState,
    showSnackbar: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    if (mainViewModel.showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = mainViewModel::closeQueue,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = null
        ) {
            QueueScreen(
                onShowSongOptions = { song ->
                    mainViewModel.openSongOptions(song)
                }
            )
        }
    }
    if (mainViewModel.showSongOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = mainViewModel::closeSongOptions,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = null
        ) {
            mainViewModel.selectedSong?.let { song ->
                SongOptionsScreen(
                    song = song,
                    onDismiss = mainViewModel::closeSongOptions,
                    historyEntryId = mainViewModel.selectedHistoryId,
                    playlistId = mainViewModel.selectedPlaylistId,
                    onStartSelection = mainViewModel.onSelectionCallback
                )
            }
        }
    }
    if (mainViewModel.showSleepTimerDialog) {
        SleepTimerDialog(
            onDismiss = mainViewModel::closeSleepTimerDialog,
            onTimerSet = { durationMs ->
                playerViewModel.setSleepTimer(durationMs)
                mainViewModel.closeSleepTimerDialog()
                showSnackbar(if (durationMs > 0) "Sleep timer set" else "Cancelled")
            }
        )
    }
    if (mainViewModel.showSleepTimerSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = mainViewModel::closeSleepTimerSheet,
            sheetState = sheetState,
            dragHandle = null
        ) {
            SleepTimerBottomSheet(
                remainingMillis = playerState.sleepTimerInMillis ?: 0L,
                onAddMinutes = { minutes -> playerViewModel.addSleepTimerMinutes(minutes) },
                onCancelTimer = {
                    playerViewModel.setSleepTimer(0)
                    mainViewModel.closeSleepTimerSheet()
                },
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        mainViewModel.closeSleepTimerSheet()
                    }
                }
            )
        }
    }
}