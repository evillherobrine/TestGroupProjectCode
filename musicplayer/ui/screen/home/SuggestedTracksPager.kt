package com.example.musicplayer.ui.screen.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.ui.screen.search.TrackItem

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SuggestedTracksPager(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
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
                            onClick = { onSongClick(song) },
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
                                    onClick = { onSongClick(song) },
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