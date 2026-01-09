package com.example.musicplayer.ui.screen.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicplayer.R
import com.example.musicplayer.domain.model.Playlist

@Composable
fun PlaylistOptionsSheet(
    playlist: Playlist,
    isLocalPlaylist: Boolean,
    onDismiss: () -> Unit,
    onPlayPlaylist: () -> Unit,
    onAddToQueue: () -> Unit,
    onDeletePlaylist: () -> Unit = {},
    onRenamePlaylist: () -> Unit = {},
    onSavePlaylist: () -> Unit = {},
    onSelectPlaylist: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (playlist.id == Playlist.FAVOURITES_PLAYLIST_ID) {
                    Box(modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer).fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(playlist.coverUrl ?: R.drawable.ic_default_cover)
                            .crossfade(true)
                            .placeholder(R.drawable.ic_default_cover)
                            .error(R.drawable.ic_default_cover)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(playlist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${playlist.songCount} songs", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onPlayPlaylist(); onDismiss() }.padding(16.dp, 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(16.dp))
            Text("Play this playlist", style = MaterialTheme.typography.bodyLarge)
        }
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onAddToQueue(); onDismiss() }.padding(16.dp, 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(R.drawable.queue_music_24px), null)
            Spacer(Modifier.width(16.dp))
            Text("Add to the queue", style = MaterialTheme.typography.bodyLarge)
        }
        if (isLocalPlaylist) {
            if (playlist.id != Playlist.FAVOURITES_PLAYLIST_ID && playlist.id != -2L) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onSelectPlaylist(); onDismiss() }.padding(16.dp, 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckBox, null)
                    Spacer(Modifier.width(16.dp))
                    Text("Select", style = MaterialTheme.typography.bodyLarge)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onRenamePlaylist(); onDismiss() }.padding(16.dp, 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, null)
                    Spacer(Modifier.width(16.dp))
                    Text("Rename playlist", style = MaterialTheme.typography.bodyLarge)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onDeletePlaylist(); onDismiss() }.padding(16.dp, 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(Modifier.width(16.dp))
                    Text("Delete playlist", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onSavePlaylist(); onDismiss() }.padding(16.dp, 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(16.dp))
                Text("Save to Library", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}