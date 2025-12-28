package com.example.musicplayer.ui.screen.home

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.navigation.NavController
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.ui.navigation.AppDestinations
import com.example.musicplayer.ui.screen.component.PlaylistOptionsSheet
import com.example.musicplayer.viewmodel.home.HomeViewModel
import com.example.musicplayer.viewmodel.playback.PlayerViewModel
import com.example.musicplayer.viewmodel.playback.QueueViewModel
import com.example.musicplayer.viewmodel.playlist.LibraryViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.example.musicplayer.R
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Suppress("AssignedValueIsNeverRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable@UnstableApi
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
    val customFontFamily = FontFamily(
        Font(
            resId = R.font.inter,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(800)
            )
        )
    )
    val interMediumFontFamily = FontFamily(
        Font(
            resId = R.font.inter,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(500)
            )
        )
    )
    val context = LocalContext.current.applicationContext as Application
    val libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory(context))
    val queueViewModel: QueueViewModel = viewModel()
    val suggestions by homeViewModel.suggestedSongs.collectAsState()
    val playlists by homeViewModel.suggestedPlaylists.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
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
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = customFontFamily
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
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = interMediumFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 18.sp
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        item {
                            SuggestedTracksPager(
                                songs = suggestions,
                                onSongClick = { playerViewModel.playSong(it) },
                                onShowSongOptions = onShowSongOptions
                            )
                        }
                    }
                    if (playlists.isNotEmpty()) {
                        item {
                            Text(
                                text = "Suggested Playlists",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = interMediumFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 18.sp
                                ),
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 24.dp,
                                    bottom = 8.dp
                                )
                            )
                        }
                        item {
                            SuggestedPlaylistsRow(
                                playlists = playlists,
                                onPlaylistClick = { playlist ->
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
                                onPlaylistLongClick = { playlist ->
                                    selectedPlaylist = playlist
                                    showPlaylistOptions = true
                                }
                            )
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