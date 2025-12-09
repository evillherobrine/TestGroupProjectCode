package com.example.musicplayer.ui.screen.component

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.rememberNavController
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.ui.navigation.AppNavHost
import com.example.musicplayer.ui.screen.player.PlayerScaffold
import com.example.musicplayer.ui.screen.player.QueueScreen
import com.example.musicplayer.ui.screen.player.SleepTimerDialog
import com.example.musicplayer.viewmodel.playback.PlayerViewModel
import com.example.musicplayer.viewmodel.search.SearchViewModel
import kotlinx.coroutines.launch

@Suppress("AssignedValueIsNeverRead")
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@UnstableApi
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
    val playerViewModel: PlayerViewModel = viewModel()
    val searchViewModel: SearchViewModel = viewModel()
    val playerState by playerViewModel.uiState.collectAsState()
    var showQueueBottomSheet by remember { mutableStateOf(false) }
    val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    val songOptionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSongOptionsBottomSheet by remember { mutableStateOf(false) }
    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var selectedHistoryEntryId by remember { mutableStateOf<Long?>(null) }
    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var onStartSelectionCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    var localScrollTrigger by remember { mutableLongStateOf(0L) }
    var homeScrollTrigger by remember { mutableLongStateOf(0L) }
    var libraryScrollTrigger by remember { mutableLongStateOf(0L) }
    if (showQueueBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueBottomSheet = false },
            sheetState = queueSheetState,
            dragHandle = null
        ) {
            QueueScreen(
                onShowSongOptions = { song ->
                    showSongOptionsBottomSheet = true
                    selectedSongForOptions = song
                    selectedHistoryEntryId = null
                    selectedPlaylistId = null
                    onStartSelectionCallback = null
                }
            )
        }
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
        SleepTimerDialog(
            onDismiss = { showSleepTimerDialog = false },
            onTimerSet = { durationMs ->
                playerViewModel.setSleepTimer(durationMs)
                showSleepTimerDialog = false
                showSnackbar(if (durationMs > 0) "Sleep timer set" else "Cancelled")
            }
        )
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
                onShowSongOptions = { song ->
                    showSongOptionsBottomSheet = true
                    selectedSongForOptions = song
                    selectedHistoryEntryId = null
                    selectedPlaylistId = null
                    onStartSelectionCallback = null
                }
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
                    scrollToTopLocal = localScrollTrigger,
                    scrollToTopHistory = 0L
                )
            }
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                MainBottomBar(
                    navController = navController,
                    bottomBarAlpha = bottomBarAlpha,
                    bottomBarHeight = customNavBarHeight,
                    systemNavBarHeight = systemNavBarHeight,
                    onHomeScroll = { homeScrollTrigger = System.currentTimeMillis() },
                    onLocalScroll = { localScrollTrigger = System.currentTimeMillis() },
                    onLibraryScroll = { libraryScrollTrigger = System.currentTimeMillis() }
                )
            }
        }
    }
    if (showSleepTimerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSleepTimerSheet = false },
            sheetState = sleepTimerSheetState,
            dragHandle = null
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