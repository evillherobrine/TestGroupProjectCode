package com.example.musicplayer.ui.screen.component

import android.app.Application
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.musicplayer.R
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.ui.screen.player.formatTime
import com.example.musicplayer.viewmodel.history.HistoryViewModel
import com.example.musicplayer.viewmodel.playback.QueueViewModel
import com.example.musicplayer.viewmodel.playlist.FavoritesViewModel
import com.example.musicplayer.viewmodel.playlist.LibraryViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SongOptionsScreen(
    song: Song,
    favoritesViewModel: FavoritesViewModel = viewModel(),
    queueViewModel: QueueViewModel = viewModel(),
    historyViewModel: HistoryViewModel = viewModel(),
    onDismiss: () -> Unit,
    historyEntryId: Long? = null,
    playlistId: Long? = null,
    onStartSelection: (() -> Unit)? = null
) {
    val context = LocalContext.current.applicationContext as Application
    val libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory(context))
    val playlists by libraryViewModel.playlists.collectAsState()
    var showPlaylistSelection by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
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
                            showCreatePlaylistDialog = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) { Text("Cancel") }
            }
        )
    }
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(bottom = 24.dp)
            .heightIn(max = 500.dp)
    ) {
        AnimatedContent(
            targetState = showPlaylistSelection,
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut())
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut())
                }
            },
            label = "SongOptionsTransition"
        ) { isPlaylistSelection ->
            if (isPlaylistSelection) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showPlaylistSelection = false }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        Text(
                            text = "Add to Playlist",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    LazyColumn {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showCreatePlaylistDialog = true
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(4.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Create")
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Create new playlist",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        items(playlists) { playlist ->
                            if (playlist.id != Playlist.FAVOURITES_PLAYLIST_ID) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            libraryViewModel.addSongToPlaylist(playlist.id, song)
                                            onDismiss()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            model = playlist.coverUrl ?: R.drawable.ic_default_cover,
                                            error = painterResource(id = R.drawable.ic_default_cover)
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column {
                                        Text(
                                            text = playlist.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${playlist.songCount} songs",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val isFavorite by favoritesViewModel.favoriteSongs.observeAsState(initial = emptyList())
                val queue by queueViewModel.queue.observeAsState(initial = emptyList())
                val currentSong by queueViewModel.currentSong.observeAsState()
                val isFav = isFavorite.any { it.id == song.id }
                val inQueue = queue.any { it.id == song.id }
                val isCurrentSong = song.id == currentSong?.id
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = song.cover,
                                error = painterResource(id = R.drawable.ic_default_cover)
                            ),
                            contentDescription = "Song Cover",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            val formattedDuration = formatTime(song.duration)
                            Text(
                                text = "${song.artist} • ($formattedDuration)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { favoritesViewModel.toggleFavorite(song) }) {
                            val favoriteColor = Color(0xFFB83730)
                            Icon(
                                imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Favourite",
                                tint = favoriteColor
                            )
                        }
                    }
                    if (!isCurrentSong) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (inQueue) {
                                        queueViewModel.remove(song)
                                    } else {
                                        queueViewModel.add(song)
                                    }
                                    onDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = if (inQueue) R.drawable.playlist_remove_24px else R.drawable.queue_music_24px),
                                contentDescription = if (inQueue) "Dismiss queue" else "Enqueue"
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = if (inQueue) "Dismiss queue" else "Enqueue",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPlaylistSelection = true
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.playlist_add_24px),
                            contentDescription = "Add to playlist"
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = "Add to playlist", style = MaterialTheme.typography.bodyLarge)
                    }
                    if (onStartSelection != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onStartSelection.invoke()
                                    onDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckBox,
                                contentDescription = "Select"
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = "Select", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    if (historyEntryId != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    historyViewModel.deleteEntry(historyEntryId)
                                    onDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete from history"
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = "Delete from history", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    if (playlistId != null && playlistId != Playlist.FAVOURITES_PLAYLIST_ID) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    libraryViewModel.removeSongFromPlaylist(playlistId, song.id)
                                    onDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove from playlist"
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = "Remove from playlist", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}