package com.example.musicplayer.ui.screen.local

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.example.musicplayer.R
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.ui.screen.search.TrackItem
import com.example.musicplayer.viewmodel.playback.PlayerViewModel
import com.example.musicplayer.viewmodel.local.LocalMusicViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
@UnstableApi
fun LocalMusicScreen(
    playerViewModel: PlayerViewModel,
    onShowSongOptions: (Song) -> Unit,
    onAlbumClick: (Long, String) -> Unit,
    onArtistClick: (Long, String) -> Unit,
    bottomBarPadding: Dp,
    scrollToTop: Long,
    viewModel: LocalMusicViewModel = viewModel()
) {
    val context = LocalContext.current
    val hasPermission by viewModel.hasPermission.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val scope = rememberCoroutineScope()
    val tabTitles = listOf("Songs", "Albums", "Artists")
    val pagerState = rememberPagerState(pageCount = { tabTitles.size })
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updatePermissionStatus(isGranted)
    }
    LaunchedEffect(Unit) {
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            permissionToRequest
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.updatePermissionStatus(isGranted)
        if (!isGranted) {
            permissionLauncher.launch(permissionToRequest)
        }
    }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Local Music") },
                    scrollBehavior = scrollBehavior
                )
                if (hasPermission) {
                    SecondaryTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                                text = { Text(title) }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            if (!hasPermission) {
                PermissionRequestScreen(context = context)
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> LocalSongsTab(
                            viewModel = viewModel,
                            playerViewModel = playerViewModel,
                            onShowSongOptions = onShowSongOptions,
                            bottomPadding = bottomBarPadding,
                            scrollToTop = scrollToTop
                        )
                        1 -> LocalAlbumsTab(
                            viewModel = viewModel,
                            onAlbumClick = onAlbumClick,
                            bottomPadding = bottomBarPadding,
                            scrollToTop = scrollToTop
                        )
                        2 -> LocalArtistsTab(
                            viewModel = viewModel,
                            onArtistClick = onArtistClick,
                            bottomPadding = bottomBarPadding,
                            scrollToTop = scrollToTop
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRequestScreen(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Permission Required",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "This app needs access to your audio files to play local music. Please grant the permission to continue.",
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }) {
            Text("Open Settings")
        }
    }
}

@Composable@UnstableApi
fun LocalSongsTab(
    viewModel: LocalMusicViewModel,
    playerViewModel: PlayerViewModel,
    onShowSongOptions: (Song) -> Unit,
    bottomPadding: Dp,
    scrollToTop: Long
) {
    val pagingItems = viewModel.localMusicFlow.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    LaunchedEffect(scrollToTop) {
        if (scrollToTop > 0) {
            listState.animateScrollToItem(0)
        }
    }
    if (pagingItems.loadState.refresh is LoadState.Loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (pagingItems.itemCount == 0 && pagingItems.loadState.refresh is LoadState.NotLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No music files found")
        }
    } else {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomPadding)
            ) {
                items(
                    count = pagingItems.itemCount,
                    key = pagingItems.itemKey { it.id },
                    contentType = pagingItems.itemContentType { "song" }
                ) { index ->
                    val song = pagingItems[index]
                    if (song != null) {
                        TrackItem(
                            song = song,
                            onClick = {
                                playerViewModel.playSongList(song, pagingItems.itemSnapshotList.items)
                            },
                            onLongClick = { onShowSongOptions(song) }
                        )
                    }
                }
                if (pagingItems.loadState.append is LoadState.Loading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
            FloatingActionButton(
                onClick = {
                    pagingItems.itemSnapshotList.items.firstOrNull()?.let { firstSong ->
                        playerViewModel.playSongList(firstSong, pagingItems.itemSnapshotList.items)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = bottomPadding + 16.dp, end = 16.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play All")
            }
        }
    }
}
@Composable
fun LocalAlbumsTab(
    viewModel: LocalMusicViewModel,
    onAlbumClick: (Long, String) -> Unit,
    bottomPadding: Dp,
    scrollToTop: Long
) {
    val pagingItems = viewModel.localAlbumsFlow.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()
    LaunchedEffect(scrollToTop) {
        if (scrollToTop > 0) {
            gridState.animateScrollToItem(0)
        }
    }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(140.dp),
        contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(pagingItems.itemCount) { index ->
            pagingItems[index]?.let { album ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(140.dp).clickable { onAlbumClick(album.id, album.name) }
                ) {
                    AsyncImage(
                        model = album.coverUri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.image_24px)
                    )
                    Text(
                        text = album.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = album.artist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
@Composable
fun LocalArtistsTab(
    viewModel: LocalMusicViewModel,
    onArtistClick: (Long, String) -> Unit,
    bottomPadding: Dp,
    scrollToTop: Long
) {
    val pagingItems = viewModel.localArtistsFlow.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    LaunchedEffect(scrollToTop) {
        if (scrollToTop > 0) {
            listState.animateScrollToItem(0)
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding)
    ) {
        items(pagingItems.itemCount) { index ->
            pagingItems[index]?.let { artist ->
                ListItem(
                    modifier = Modifier.clickable { onArtistClick(artist.id, artist.name) },
                    headlineContent = { Text(artist.name) },
                    supportingContent = { Text("${artist.trackCount} songs") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                        )
                    }
                )
            }
        }
    }
}