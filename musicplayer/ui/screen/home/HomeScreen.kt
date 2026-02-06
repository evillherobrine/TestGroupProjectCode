package com.example.musicplayer.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.example.musicplayer.R
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.ui.navigation.AppDestinations
import com.example.musicplayer.viewmodel.home.HomeViewModel
import com.example.musicplayer.viewmodel.playback.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@UnstableApi
fun HomeScreenComposable(
    onShowSongOptions: (Song) -> Unit,
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel,
    homeViewModel: HomeViewModel = viewModel(),
    navController: NavController,
    scrollToTop: Long,
    bottomPadding: Dp = 0.dp
) {
    val userPersona by homeViewModel.userPersona.collectAsState()
    val customFontFamily = FontFamily(
        Font(
            resId = R.font.inter,
            variationSettings = FontVariation.Settings(FontVariation.weight(800))
        )
    )
    val interMediumFontFamily = FontFamily(
        Font(
            resId = R.font.inter,
            variationSettings = FontVariation.Settings(FontVariation.weight(500))
        )
    )
    val recentSongs by homeViewModel.recentSongs.collectAsState()
    val trendingSongs by homeViewModel.trendingSongs.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
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
            if (isLoading && trendingSongs.isEmpty() && recentSongs.isEmpty()) {
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
                    item {
                        userPersona?.let { persona ->
                            ListeningStyleCard(
                                persona = persona,
                                onClick = { navController.navigate(AppDestinations.INSIGHTS) }
                            )
                        }
                    }
                    if (recentSongs.isNotEmpty()) {
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
                                songs = recentSongs,
                                onSongClick = { playerViewModel.playSong(it) },
                                onShowSongOptions = onShowSongOptions
                            )
                        }
                    }
                    if (trendingSongs.isNotEmpty()) {
                        item {
                            Text(
                                text = "Trending Now",
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
                            SuggestedTracksPager(
                                songs = trendingSongs,
                                onSongClick = { playerViewModel.playSong(it) },
                                onShowSongOptions = onShowSongOptions
                            )
                        }
                    } else if (recentSongs.isNotEmpty() && isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 20.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    if (!isLoading && recentSongs.isEmpty() && trendingSongs.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Cannot load suggestions. Check your connection.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningStyleCard(
    persona: Pair<String, String>,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your Listening Style",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${persona.first} ${persona.second}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}