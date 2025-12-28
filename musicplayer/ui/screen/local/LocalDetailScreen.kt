package com.example.musicplayer.ui.screen.local

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicplayer.R
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.ui.screen.search.TrackItem
import com.example.musicplayer.viewmodel.playback.PlayerViewModel
import com.example.musicplayer.viewmodel.local.LocalDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable@UnstableApi
fun LocalDetailScreen(
    title: String,
    type: LocalDetailViewModel.DetailType,
    id: Long,
    path: String? = null,
    playerViewModel: PlayerViewModel,
    onNavigateBack: () -> Unit,
    onShowSongOptions: (Song) -> Unit,
    bottomBarPadding: Dp
) {
    val customFontFamily = FontFamily(
        Font(
            resId = R.font.inter,
            variationSettings = FontVariation.Settings(FontVariation.weight(800))
        )
    )
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: LocalDetailViewModel = viewModel(
        factory = LocalDetailViewModel.Factory(
            application = application,
            type = type,
            id = id,
            path = path
        )
    )
    val pagingItems = viewModel.songs.collectAsLazyPagingItems()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, fontFamily = customFontFamily) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomBarPadding)
            ) {
                // Detail Header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Cover Image / Icon
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            val firstSong = if (pagingItems.itemCount > 0) pagingItems[0] else null
                            when (type) {
                                LocalDetailViewModel.DetailType.ALBUM -> {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(firstSong?.cover)
                                            .crossfade(true)
                                            .placeholder(R.drawable.music_note)
                                            .error(R.drawable.music_note)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                LocalDetailViewModel.DetailType.ARTIST -> {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(100.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                LocalDetailViewModel.DetailType.FOLDER -> {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(100.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Large Play Button
                        IconButton(
                            onClick = {
                                if (pagingItems.itemCount > 0) {
                                    pagingItems.itemSnapshotList.items.firstOrNull()?.let {
                                        playerViewModel.playSongList(it, pagingItems.itemSnapshotList.items)
                                    }
                                }
                            },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play All",
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                // Songs List
                items(
                    count = pagingItems.itemCount,
                    key = pagingItems.itemKey { it.id },
                    contentType = pagingItems.itemContentType { "song" }
                ) { index ->
                    val song = pagingItems[index]
                    if (song != null) {
                        TrackItem(
                            song = song,
                            onClick = { playerViewModel.playSongList(song, pagingItems.itemSnapshotList.items) },
                            onLongClick = { onShowSongOptions(song) }
                        )
                    }
                }
            }
        }
    }
}