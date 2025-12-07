package com.example.musicplayer.ui.screen.home

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.ui.navigation.AppDestinations
import com.example.musicplayer.ui.screen.component.PlaylistOptionsSheet
import com.example.musicplayer.ui.screen.search.PlaylistGridItem
import com.example.musicplayer.ui.screen.search.TrackItem
import com.example.musicplayer.viewmodel.home.HomeViewModel
import com.example.musicplayer.viewmodel.playback.PlayerViewModel
import com.example.musicplayer.viewmodel.playback.QueueViewModel
import com.example.musicplayer.viewmodel.playlist.LibraryViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenComposable(
    onShowSongOptions: (Song) -> Unit,
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel,
    homeViewModel: HomeViewModel = viewModel(),
    navController: NavController,
    showSnackbar: (String) -> Unit,
    scrollToTop: Long,
    bottomPadding: Dp = 0.dp
) {
    val context = LocalContext.current.applicationContext as Application
    val libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory(context))
    val queueViewModel: QueueViewModel = viewModel()
    val suggestions by homeViewModel.suggestedSongs.observeAsState(emptyList())
    val playlists by homeViewModel.suggestedPlaylists.observeAsState(emptyList())
    val isLoading by homeViewModel.isLoading.observeAsState(false)
    var showPlaylistOptions by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val listState = rememberLazyListState()

    LaunchedEffect(scrollToTop) {
        if (scrollToTop > 0) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Home",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate(AppDestinations.SEARCH) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 16.dp,
                        bottom = bottomPadding + 16.dp
                    )
                ) {
                    if (suggestions.isNotEmpty()) {
                        item {
                            Text(
                                text = "Quick Play",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        item {
                            SuggestedTracksPager(
                                songs = suggestions,
                                playerViewModel = playerViewModel,
                                onShowSongOptions = onShowSongOptions
                            )
                        }
                    }
                    if (playlists.isNotEmpty()) {
                        item {
                            Text(
                                text = "Suggested Playlists",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 24.dp,
                                    bottom = 8.dp
                                )
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(playlists, key = { it.id }) { playlist ->
                                    PlaylistGridItem(
                                        playlist = playlist,
                                        onPlaylistClick = {
                                            val encodedName = URLEncoder.encode(
                                                playlist.name,
                                                StandardCharsets.UTF_8.toString()
                                            )
                                            if (playlist.id == Playlist.FAVOURITES_PLAYLIST_ID) {
                                                navController.navigate("${AppDestinations.USER_PLAYLIST_DETAIL}/${playlist.id}/$encodedName")
                                            } else {
                                                navController.navigate("${AppDestinations.API_PLAYLIST_DETAIL}/${playlist.id}/$encodedName")
                                            }
                                        },
                                        onPlaylistLongClick = {
                                            selectedPlaylist = playlist
                                            showPlaylistOptions = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (suggestions.isEmpty() && playlists.isEmpty()) {
                        item {
                            Text(
                                text = "No suggestions found.",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    if (showPlaylistOptions && selectedPlaylist != null) {
        ModalBottomSheet(
            onDismissRequest = { showPlaylistOptions = false },
            sheetState = sheetState,
            dragHandle = null
        ) {
            PlaylistOptionsSheet(
                playlist = selectedPlaylist!!,
                isLocalPlaylist = false,
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showPlaylistOptions = false
                    }
                },
                onPlayPlaylist = {
                    selectedPlaylist?.let { playlist ->
                        libraryViewModel.fetchOnlineSongs(playlist.id) { songs ->
                            if (songs.isNotEmpty()) {
                                playerViewModel.playSongList(songs.first(), songs)
                            }
                        }
                    }
                },
                onAddToQueue = {
                    selectedPlaylist?.let { playlist ->
                        libraryViewModel.fetchOnlineSongs(playlist.id) { songs ->
                            songs.forEach { queueViewModel.add(it) }
                        }
                    }
                },
                onSavePlaylist = {
                    selectedPlaylist?.let { playlist ->
                        libraryViewModel.importOnlinePlaylist(playlist.id, playlist.name)
                        showSnackbar("Playlist '${playlist.name}' saved to library")
                    }
                }
            )
        }
    }
}
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SuggestedTracksPager(
    songs: List<Song>,
    playerViewModel: PlayerViewModel,
    onShowSongOptions: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columnsPerPage = if (maxWidth < 600.dp) 1 else 2
        val songsPerColumn = 4
        val chunkSize = songsPerColumn * columnsPerPage
        val chunkedSongs = songs.chunked(chunkSize)
        val pagerState = rememberPagerState(pageCount = { chunkedSongs.size })
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 64.dp),
            pageSpacing = 16.dp
        ) { page ->
            val songsForPage = chunkedSongs[page]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val columns = songsForPage.chunked(songsPerColumn)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    columns.getOrNull(0)?.forEach { song ->
                        TrackItem(
                            song = song,
                            onClick = { playerViewModel.playSong(song) },
                            onLongClick = { onShowSongOptions(song) }
                        )
                    }
                }
                if (columnsPerPage > 1) {
                    if (columns.size > 1) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            columns[1].forEach { song ->
                                TrackItem(
                                    song = song,
                                    onClick = { playerViewModel.playSong(song) },
                                    onLongClick = { onShowSongOptions(song) }
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}