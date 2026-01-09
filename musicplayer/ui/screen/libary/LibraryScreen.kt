package com.example.musicplayer.ui.screen.libary

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.example.musicplayer.R
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.ui.navigation.AppDestinations
import com.example.musicplayer.ui.screen.component.PlaylistOptionsSheet
import com.example.musicplayer.viewmodel.playback.PlayerViewModel
import com.example.musicplayer.viewmodel.playback.QueueViewModel
import com.example.musicplayer.viewmodel.playlist.LibraryViewModel
import kotlinx.coroutines.launch

@Suppress("AssignedValueIsNeverRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable@UnstableApi
fun PlaylistScreenComposable(
    onPlaylistClick: (id: Long, name: String) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    onShowSnackbar: (String) -> Unit,
    scrollToTop: Long,
    navController: NavController
) {
    val customFontFamily = FontFamily(
        Font(
            resId = R.font.inter,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(800)
            )
        )
    )
    val context = LocalContext.current.applicationContext as Application
    val libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory(context))
    val playerViewModel: PlayerViewModel = viewModel()
    val queueViewModel: QueueViewModel = viewModel()
    val playlists by libraryViewModel.playlists.collectAsState()
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedPlaylistIds = remember { mutableStateListOf<Long>() }
    val selectablePlaylists = playlists.filter { it.id != Playlist.FAVOURITES_PLAYLIST_ID && it.id != -2L }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var renamePlaylistName by remember { mutableStateOf("") }
    val gridState = rememberLazyGridState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    LaunchedEffect(scrollToTop) {
        if (scrollToTop > 0) {
            gridState.animateScrollToItem(0)
        }
    }
    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedPlaylistIds.clear()
    }
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            libraryViewModel.createPlaylist(newPlaylistName)
                            newPlaylistName = ""
                            showCreateDialog = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
    if (showRenameDialog && selectedPlaylist != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Playlist") },
            text = {
                OutlinedTextField(
                    value = renamePlaylistName,
                    onValueChange = { renamePlaylistName = it },
                    label = { Text("New Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renamePlaylistName.isNotBlank()) {
                            libraryViewModel.renamePlaylist(selectedPlaylist!!.id, renamePlaylistName)
                            showRenameDialog = false
                        }
                    }
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Playlists?") },
            text = { Text("Are you sure you want to delete ${selectedPlaylistIds.size} selected playlists?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        libraryViewModel.deletePlaylists(selectedPlaylistIds.toList())
                        selectedPlaylistIds.clear()
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
    if (showOptionsSheet && selectedPlaylist != null) {
        ModalBottomSheet(
            onDismissRequest = { showOptionsSheet = false },
            sheetState = sheetState,
            dragHandle = null
        ) {
            PlaylistOptionsSheet(
                playlist = selectedPlaylist!!,
                isLocalPlaylist = true,
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showOptionsSheet = false
                    }
                },
                onPlayPlaylist = {
                    selectedPlaylist?.let { playlist ->
                        if (playlist.id != -2L) {
                            libraryViewModel.getPlaylistSongs(playlist.id) { songs ->
                                if (songs.isNotEmpty()) {
                                    playerViewModel.playSongList(songs.first(), songs)
                                }
                            }
                        }
                    }
                },
                onAddToQueue = {
                    selectedPlaylist?.let { playlist ->
                        if (playlist.id != -2L) {
                            libraryViewModel.getPlaylistSongs(playlist.id) { songs ->
                                if (songs.isNotEmpty()) {
                                    songs.forEach { song ->
                                        queueViewModel.add(song)
                                    }
                                }
                            }
                        }
                    }
                },
                onRenamePlaylist = {
                    renamePlaylistName = selectedPlaylist!!.name
                    showRenameDialog = true
                },
                onDeletePlaylist = {
                    selectedPlaylist?.let { playlist ->
                        if (playlist.id != Playlist.FAVOURITES_PLAYLIST_ID && playlist.id != -2L) {
                            libraryViewModel.deletePlaylist(playlist.id)
                            onShowSnackbar("Playlist '${playlist.name}' deleted")
                        }
                    }
                },
                onSelectPlaylist = {
                    isSelectionMode = true
                    selectedPlaylistIds.clear()
                    selectedPlaylistIds.add(selectedPlaylist!!.id)
                }
            )
        }
    }
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                colors = if (isSelectionMode) {
                    TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                } else {
                    TopAppBarDefaults.topAppBarColors()
                },
                title = {
                    if (isSelectionMode) {
                        Text("${selectedPlaylistIds.size} Selected")
                    } else {
                        Text("Library",
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = customFontFamily)
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedPlaylistIds.clear()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            if (selectedPlaylistIds.size == selectablePlaylists.size) {
                                selectedPlaylistIds.clear()
                            } else {
                                selectedPlaylistIds.clear()
                                selectedPlaylistIds.addAll(selectablePlaylists.map { it.id })
                            }
                        }) {
                            val allSelected = selectedPlaylistIds.size == selectablePlaylists.size && selectablePlaylists.isNotEmpty()
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select All",
                                tint = if (allSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (selectedPlaylistIds.isNotEmpty()) {
                            IconButton(onClick = { showDeleteConfirmDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                            }
                        }
                    } else {
                        IconButton(onClick = { navController.navigate(AppDestinations.HISTORY) }) {
                            Icon(Icons.Default.History, contentDescription = "History")
                        }
                        IconButton(onClick = { navController.navigate(AppDestinations.SEARCH) }) {
                            Icon(Icons.Default.Search, "Search")
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.padding(bottom = bottomPadding)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create new playlist")
                }
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 128.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = bottomPadding + 8.dp,
                start = 8.dp,
                end = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = playlists,
                key = { it.id }
            ) { playlist ->
                PlaylistItem(
                    playlist = playlist,
                    isSelectionMode = isSelectionMode,
                    isSelected = selectedPlaylistIds.contains(playlist.id),
                    onPlaylistClick = {
                        if (isSelectionMode) {
                            if (playlist.id != Playlist.FAVOURITES_PLAYLIST_ID && playlist.id != -2L) {
                                if (selectedPlaylistIds.contains(playlist.id)) {
                                    selectedPlaylistIds.remove(playlist.id)
                                } else {
                                    selectedPlaylistIds.add(playlist.id)
                                }
                            }
                        } else {
                            onPlaylistClick(playlist.id, playlist.name)
                        }
                    },
                    onPlaylistLongClick = {
                        if (!isSelectionMode) {
                            if (playlist.id != -2L) {
                                selectedPlaylist = playlist
                                showOptionsSheet = true
                            }
                        }
                    }
                )
            }
        }
    }
}