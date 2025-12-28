package com.example.musicplayer.ui.screen.libary

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicplayer.R
import com.example.musicplayer.domain.model.Playlist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistItem(
    playlist: Playlist,
    onPlaylistClick: () -> Unit,
    onPlaylistLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false
) {
    val interFontFamily = FontFamily(
        Font(
            resId = R.font.inter,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(500)
            )
        )
    )

    val isSystemPlaylist = playlist.id == Playlist.FAVOURITES_PLAYLIST_ID || playlist.id == -2L
    val isSelectable = !isSystemPlaylist
    val alpha by animateFloatAsState(
        targetValue = if (isSelectionMode && !isSelectable) 0.3f else 1f,
        label = "alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.92f else 1f,
        label = "scale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "border"
    )

    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .alpha(alpha)
                .scale(scale)
                .clip(MaterialTheme.shapes.medium)
                .combinedClickable(
                    onClick = {
                        if (isSelectionMode && !isSelectable) return@combinedClickable
                        onPlaylistClick()
                    },
                    onLongClick = {
                        if (isSelectionMode && !isSelectable) return@combinedClickable
                        onPlaylistLongClick()
                    }
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                    .border(
                        BorderStroke(
                            if (isSelected) 3.dp else 1.dp,
                            if (isSelected) borderColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    playlist.id == Playlist.FAVOURITES_PLAYLIST_ID -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Favorites",
                                modifier = Modifier.fillMaxWidth(0.65f),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    playlist.id == -2L -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Local Music",
                                modifier = Modifier.fillMaxWidth(0.65f),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    playlist.coverUrl == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.playlist_play),
                                contentDescription = "New Playlist",
                                modifier = Modifier.fillMaxWidth(0.65f),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    else -> {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(playlist.coverUrl)
                                .crossfade(true)
                                .placeholder(R.drawable.ic_default_cover)
                                .error(R.drawable.ic_default_cover)
                                .build(),
                            contentDescription = "Playlist Cover",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = interFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
            )
            if (!isSystemPlaylist) {
                Text(
                    text = "${playlist.songCount} songs",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                )
            }
        }
    }
}