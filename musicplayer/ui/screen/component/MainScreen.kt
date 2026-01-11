package com.example.musicplayer.ui.screen.component

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
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
            NavigationRail(
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