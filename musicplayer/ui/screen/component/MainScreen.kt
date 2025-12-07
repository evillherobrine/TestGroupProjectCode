package com.example.musicplayer.ui.screen.component

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.ui.navigation.AppDestinations
import com.example.musicplayer.ui.navigation.AppNavHost
import com.example.musicplayer.ui.screen.player.PlayerScaffold
import com.example.musicplayer.ui.screen.player.QueueScreen
import com.example.musicplayer.ui.screen.player.SleepTimerDialog
import com.example.musicplayer.viewmodel.playback.PlayerUiState
import com.example.musicplayer.viewmodel.playback.PlayerViewModel
import com.example.musicplayer.viewmodel.search.SearchViewModel
import kotlinx.coroutines.launch

@Suppress("AssignedValueIsNeverRead")
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    val sleepTimerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )
    val hostState = remember { SnackbarHostState() }
    val showSnackbar: (String) -> Unit = { message ->
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            hostState.showSnackbar(message)
        }
    }
    var homeScrollTrigger by remember { mutableLongStateOf(0L) }
    var libraryScrollTrigger by remember { mutableLongStateOf(0L) }
    val playerViewModel: PlayerViewModel = viewModel()
    val searchViewModel: SearchViewModel = viewModel()
    val playerState by playerViewModel.uiState.observeAsState(PlayerUiState())
    var showQueueBottomSheet by remember { mutableStateOf(false) }
    val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    val songOptionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSongOptionsBottomSheet by remember { mutableStateOf(false) }
    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var selectedHistoryEntryId by remember { mutableStateOf<Long?>(null) }
    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var onStartSelectionCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    if (showQueueBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueBottomSheet = false },
            sheetState = queueSheetState,
            dragHandle = null
        ) { QueueScreen(onShowSongOptions = { song -> showSongOptionsBottomSheet = true; selectedSongForOptions = song; selectedHistoryEntryId = null; selectedPlaylistId = null; onStartSelectionCallback = null }) }
    }
    if (showSongOptionsBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSongOptionsBottomSheet = false },
            sheetState = songOptionsSheetState,
            dragHandle = null
        ) {
            selectedSongForOptions?.let { song ->
                SongOptionsScreen(
                    song = song,
                    onDismiss = { showSongOptionsBottomSheet = false },
                    historyEntryId = selectedHistoryEntryId,
                    playlistId = selectedPlaylistId,
                    onStartSelection = onStartSelectionCallback
                )
            }
        }
    }
    if (showSleepTimerDialog) {
        SleepTimerDialog(onDismiss = { showSleepTimerDialog = false }, onTimerSet = { durationMs -> playerViewModel.setSleepTimer(durationMs); showSleepTimerDialog = false; showSnackbar(if (durationMs > 0) "Sleep timer set" else "Cancelled") })
    }
    val systemNavBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val customNavBarHeight = 56.dp
    val totalBottomBarHeight = customNavBarHeight + systemNavBarHeight
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        val peekHeight = com.example.musicplayer.ui.screen.player.MiniPlayerHeight + totalBottomBarHeight
        val bottomBarAlpha by remember(scaffoldState.bottomSheetState) {
            derivedStateOf {
                try {
                    val offset = scaffoldState.bottomSheetState.requireOffset()
                    val maxOffset = with(density) { (screenHeight - peekHeight).toPx() }
                    val progress = if (maxOffset > 0) (1f - (offset / maxOffset)).coerceIn(0f, 1f) else 0f
                    1f - progress
                } catch (_: Exception) {
                    1f
                }
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            PlayerScaffold(
                playerState = playerState,
                scaffoldState = scaffoldState,
                snackbarHostState = hostState,
                bottomBarHeight = customNavBarHeight,
                onPlayPause = { playerViewModel.togglePlayPause() },
                onNext = { playerViewModel.nextSong() },
                onPrev = { playerViewModel.prevSong() },
                onSeek = { position -> playerViewModel.seekTo(position) },
                onToggleFavorite = { playerViewModel.toggleFavorite() },
                onToggleRepeat = { playerViewModel.toggleRepeat() },
                onShowQueue = { showQueueBottomSheet = true },
                onShowSleepTimer = {
                    if (playerState.sleepTimerInMillis != null && playerState.sleepTimerInMillis!! > 0) {
                        showSleepTimerSheet = true
                    } else {
                        showSleepTimerDialog = true
                    }
                },
                onShowSongOptions = { song -> showSongOptionsBottomSheet = true; selectedSongForOptions = song; selectedHistoryEntryId = null; selectedPlaylistId = null; onStartSelectionCallback = null },
                onSongSwipe = { song -> playerViewModel.playSong(song) },
            ) { innerPadding, miniPlayerHeight ->
                val totalBottomSpacing = systemNavBarHeight + customNavBarHeight
                AppNavHost(
                    navController = navController,
                    scaffoldPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = totalBottomSpacing
                    ),
                    miniPlayerHeight = miniPlayerHeight,
                    onShowSongOptions = { song, historyId, playlistId, onStartSelection ->
                        showSongOptionsBottomSheet = true
                        selectedSongForOptions = song
                        selectedHistoryEntryId = historyId
                        selectedPlaylistId = playlistId
                        onStartSelectionCallback = onStartSelection
                    },
                    onShowSnackbar = showSnackbar,
                    searchViewModel = searchViewModel,
                    scrollToTopHome = homeScrollTrigger,
                    scrollToTopLibrary = libraryScrollTrigger,
                    scrollToTopHistory = 0L
                )
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(totalBottomBarHeight)
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
                    NavItem(AppDestinations.HOME, Icons.Filled.Home, Icons.Outlined.Home, "Home") { homeScrollTrigger = System.currentTimeMillis() },
                    NavItem(AppDestinations.LIBRARY, Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic, "Library") { libraryScrollTrigger = System.currentTimeMillis() },
                )
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = systemNavBarHeight),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { item ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        val contentColor = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    if (isSelected) item.onClickAction() else navController.navigate(item.route) { popUpTo(navController.graph.startDestinationId); launchSingleTop = true }
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Icon(if (isSelected) item.selectedIcon else item.unselectedIcon, item.label, tint = contentColor, modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(item.label, color = contentColor, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal))
                        }
                    }
                }
            }
        }
    }
    if (showSleepTimerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSleepTimerSheet = false },
            sheetState = sleepTimerSheetState
        ) {
            val remaining = playerState.sleepTimerInMillis ?: 0L
            SleepTimerBottomSheet(
                remainingMillis = remaining,
                onAddMinutes = { minutes -> playerViewModel.addSleepTimerMinutes(minutes) },
                onCancelTimer = {
                    playerViewModel.setSleepTimer(0)
                    showSleepTimerSheet = false
                },
                onDismiss = {
                    scope.launch { sleepTimerSheetState.hide() }.invokeOnCompletion {
                        showSleepTimerSheet = false
                    }
                }
            )
        }
    }
}
data class NavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
    val onClickAction: () -> Unit
)