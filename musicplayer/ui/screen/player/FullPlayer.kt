@file:Suppress("AssignedValueIsNeverRead")

package com.example.musicplayer.ui.screen.player

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.example.musicplayer.data.local.memory.SongMemory
import com.example.musicplayer.viewmodel.memory.MemoryViewModel
import com.example.musicplayer.viewmodel.playback.PlayerUiState
import com.example.musicplayer.viewmodel.playback.PlayerViewModel

@UnstableApi
@Composable
fun FullPlayer(
    state: PlayerUiState,
    memoryViewModel: MemoryViewModel = viewModel(),
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPrevClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleFavorite:  () -> Unit,
    onToggleRepeat: () -> Unit,
    onShowQueue: () -> Unit,
    onShowSleepTimer:  () -> Unit,
    onShowSongOptions: () -> Unit,
) {
    val playerViewModel: PlayerViewModel = viewModel()
    val handleToggleNightMode = {
        android.util.Log.d("FullPlayer", "User clicked Night Mode button")
        playerViewModel.toggleNightMode()
    }
    val currentMemory by memoryViewModel.currentMemory.collectAsState()
    var showMemoryDialog by remember { mutableStateOf(false) }
    if (showMemoryDialog) {
        MemoryDialog(
            initialNote = currentMemory?.note ?: "",
            initialMood = currentMemory?.mood ?: "😊",
            onDismiss = { showMemoryDialog = false },
            onSave = { note: String, mood: String -> memoryViewModel.saveMemory(note, mood) },
            onDelete = { memoryViewModel.deleteMemory(); showMemoryDialog = false }
        )
    }
    val configuration = LocalConfiguration.current
    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        FullPlayerLandscape(
            state = state,
            currentMemory = currentMemory,
            onNoteClick = { showMemoryDialog = true },
            onPlayPauseClick = onPlayPauseClick,
            onNextClick = onNextClick,
            onPrevClick = onPrevClick,
            onToggleFavorite = onToggleFavorite,
            onToggleRepeat = onToggleRepeat,
            onShowQueue = onShowQueue,
            onShowSleepTimer = onShowSleepTimer,
            onShowSongOptions = onShowSongOptions,
            onToggleNightMode = handleToggleNightMode
        )
    } else {
        FullPlayerPortrait(
            state = state,
            currentMemory = currentMemory,
            onNoteClick = { showMemoryDialog = true },
            onPlayPauseClick = onPlayPauseClick,
            onNextClick = onNextClick,
            onPrevClick = onPrevClick,
            onSeek = onSeek,
            onToggleFavorite = onToggleFavorite,
            onToggleRepeat = onToggleRepeat,
            onShowQueue = onShowQueue,
            onShowSleepTimer = onShowSleepTimer,
            onShowSongOptions = onShowSongOptions,
            onToggleNightMode = handleToggleNightMode
        )
    }
}

@Composable
private fun FullPlayerLandscape(
    state: PlayerUiState,
    currentMemory: SongMemory?,
    onNoteClick: () -> Unit,
    onPlayPauseClick:  () -> Unit,
    onNextClick:  () -> Unit,
    onPrevClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleRepeat: () -> Unit,
    onShowQueue: () -> Unit,
    onShowSleepTimer: () -> Unit,
    onShowSongOptions: () -> Unit,
    onToggleNightMode: () -> Unit
) {
    val displayTitle = state.title
    val displayArtist = state.artist
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(insets)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            LargeCover(currentCoverUrlXL = state.coverUrlXL)
        }
        Column(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TrackInfo(title = displayTitle, artist = displayArtist)
                if (currentMemory != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                            .clickable { onNoteClick() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = currentMemory.mood, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentMemory.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    TextButton(onClick = onNoteClick) {
                        Text("✍️ Create Diary", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            TrackControl(
                isPlaying = state.isPlaying,
                isLoading = state.isLoading,
                repeatMode = state.repeatMode,
                isFavourite = state.isFavourite,
                onPlayPauseClick = onPlayPauseClick,
                onPrevClick = onPrevClick,
                onNextClick = onNextClick,
                onToggleRepeat = onToggleRepeat,
                onToggleFavorite = onToggleFavorite
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                UpNextBar(
                    upNextSong = state.upNextSong,
                    isNightModeEnabled = state.isNightModeEnabled,
                    onShowQueue = onShowQueue,
                    onShowSleepTimer = onShowSleepTimer,
                    onShowSongOptions = onShowSongOptions,
                    onToggleNightMode = onToggleNightMode
                )
            }
        }
    }
}

@Composable
private fun FullPlayerPortrait(
    state: PlayerUiState,
    currentMemory: SongMemory?,
    onNoteClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPrevClick:  () -> Unit,
    onSeek:  (Long) -> Unit,
    onToggleFavorite:  () -> Unit,
    onToggleRepeat: () -> Unit,
    onShowQueue: () -> Unit,
    onShowSleepTimer:  () -> Unit,
    onShowSongOptions: () -> Unit,
    onToggleNightMode:  () -> Unit
) {
    val displayTitle = state.title
    val displayArtist = state.artist
    val topPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = topPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            LargeCover(currentCoverUrlXL = state.coverUrlXL)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TrackInfo(title = displayTitle, artist = displayArtist)
                if (currentMemory != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                            .clickable { onNoteClick() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = currentMemory.mood, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentMemory.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    TextButton(onClick = onNoteClick) {
                        Text("✍️ Create Diary", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Progress(
                position = state.position,
                duration = state.duration,
                onSeek = onSeek
            )
            TrackControl(
                isPlaying = state.isPlaying,
                isLoading = state.isLoading,
                repeatMode = state.repeatMode,
                isFavourite = state.isFavourite,
                onPlayPauseClick = onPlayPauseClick,
                onPrevClick = onPrevClick,
                onNextClick = onNextClick,
                onToggleRepeat = onToggleRepeat,
                onToggleFavorite = onToggleFavorite
            )
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            UpNextBar(
                upNextSong = state.upNextSong,
                isNightModeEnabled = state.isNightModeEnabled,
                onShowQueue = onShowQueue,
                onShowSleepTimer = onShowSleepTimer,
                onShowSongOptions = onShowSongOptions,
                onToggleNightMode = onToggleNightMode
            )
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}