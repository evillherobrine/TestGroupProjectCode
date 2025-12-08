package com.example.musicplayer.ui.screen.libary

import android.app.Application
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.ui.screen.search.TrackItem
import com.example.musicplayer.viewmodel.playback.PlayerViewModel
import com.example.musicplayer.viewmodel.playlist.FavoritesViewModel
import com.example.musicplayer.viewmodel.playlist.LocalPlaylistViewModel
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Suppress("AssignedValueIsNeverRead")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable@UnstableApi
fun UserPlaylistDetailScreen(
    playlistId: Long,
    playlistName: String,
    playerViewModel: PlayerViewModel,
    onNavigateBack: () -> Unit,
    onShowSongOptions: (song: Song, onStartSelection: () -> Unit) -> Unit,
    bottomBarPadding: Dp
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var currentName by remember { mutableStateOf(playlistName) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var newNameInput by remember { mutableStateOf("") }
    var isTransitioning by remember { mutableStateOf(true) }
    var localPlaylistViewModel: LocalPlaylistViewModel? = null
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedSongIds = remember { mutableStateListOf<Long>() }
    val songsState = if (playlistId == Playlist.FAVOURITES_PLAYLIST_ID) {
        val favoritesViewModel: FavoritesViewModel = viewModel()
        favoritesViewModel.favoriteSongs.collectAsState()
    } else {
        val context = LocalContext.current.applicationContext as Application
        localPlaylistViewModel = viewModel(
            factory = LocalPlaylistViewModel.Factory(context, playlistId)
        )
        val playlistInfo by localPlaylistViewModel.playlist.collectAsState()

        if (playlistInfo?.playlist?.name != null) {
            currentName = playlistInfo!!.playlist.name
        }
        localPlaylistViewModel.songs.collectAsState()
    }
    val dbSongs = songsState.value
    var uiSongs by remember { mutableStateOf(dbSongs) }
    LaunchedEffect(Unit) {
        delay(350)
        isTransitioning = false
    }
    LaunchedEffect(dbSongs) {
        if (uiSongs != dbSongs) {
            uiSongs = dbSongs
        }
    }
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (!isSelectionMode) {
            uiSongs = uiSongs.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
            localPlaylistViewModel?.moveSong(from.index, to.index)
        }
    }
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Playlist") },
            text = {
                OutlinedTextField(
                    value = newNameInput,
                    onValueChange = { newNameInput = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newNameInput.isNotBlank()) {
                            localPlaylistViewModel?.renamePlaylist(newNameInput)
                            showRenameDialog = false
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Selected Songs?") },
            text = { Text("Are you sure you want to remove ${selectedSongIds.size} songs from this playlist?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        localPlaylistViewModel?.removeSongs(selectedSongIds.toList())
                        selectedSongIds.clear()
                        isSelectionMode = false
                        showDeleteConfirmDialog = false
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                colors = if (isSelectionMode) {
                    TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                } else {
                    TopAppBarDefaults.topAppBarColors()
                },
                title = {
                    Column {
                        if (isSelectionMode) {
                            Text("${selectedSongIds.size} Selected")
                        } else {
                            Text(currentName)
                            if (!isTransitioning) {
                                Text(
                                    text = "${uiSongs.size} songs",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedSongIds.clear()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    } else {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (playlistId != Playlist.FAVOURITES_PLAYLIST_ID) {
                        if (isSelectionMode) {
                            IconButton(onClick = {
                                if (selectedSongIds.size == uiSongs.size) {
                                    selectedSongIds.clear()
                                } else {
                                    selectedSongIds.clear()
                                    selectedSongIds.addAll(uiSongs.map { it.id })
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = "Select All",
                                    tint = if (selectedSongIds.size == uiSongs.size && uiSongs.isNotEmpty())
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (selectedSongIds.isNotEmpty()) {
                                IconButton(onClick = { showDeleteConfirmDialog = true }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                                }
                            }
                        } else {
                            IconButton(onClick = {
                                newNameInput = currentName
                                showRenameDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename Playlist")
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            if (isTransitioning) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                if (playlistId == Playlist.FAVOURITES_PLAYLIST_ID) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = bottomBarPadding)
                    ) {
                        items(uiSongs) { song ->
                            TrackItem(
                                song = song,
                                onClick = { playerViewModel.playSongList(song, uiSongs) },
                                onLongClick = { onShowSongOptions(song) {} }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = bottomBarPadding),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiSongs, key = { it.id }) { song ->
                            ReorderableItem(
                                state = reorderableLazyListState,
                                key = song.id,
                                enabled = !isSelectionMode
                            ) { isDragging ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    if (isSelectionMode) {
                                        Checkbox(
                                            checked = selectedSongIds.contains(song.id),
                                            onCheckedChange = { checked ->
                                                if (checked) selectedSongIds.add(song.id)
                                                else selectedSongIds.remove(song.id)
                                            },
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }

                                    TrackItem(
                                        song = song,
                                        onClick = {
                                            if (isSelectionMode) {
                                                if (selectedSongIds.contains(song.id)) {
                                                    selectedSongIds.remove(song.id)
                                                } else {
                                                    selectedSongIds.add(song.id)
                                                }
                                            } else {
                                                playerViewModel.playSongList(song, uiSongs)
                                            }
                                        },
                                        onLongClick = {
                                            if (!isSelectionMode) {
                                                onShowSongOptions(song) {
                                                    isSelectionMode = true
                                                    if (!selectedSongIds.contains(song.id)) {
                                                        selectedSongIds.add(song.id)
                                                    }
                                                }
                                            }
                                        },
                                        showDragHandle = !isSelectionMode,
                                        isDragging = isDragging,
                                        dragHandleModifier = Modifier.draggableHandle(),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
                if (uiSongs.isNotEmpty() && !isSelectionMode) {
                    FloatingActionButton(
                        onClick = {
                            playerViewModel.playSongList(uiSongs.first(), uiSongs)
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                bottom = bottomBarPadding + 16.dp,
                                end = 16.dp
                            )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play All")
                    }
                }
            }
        }
    }
}