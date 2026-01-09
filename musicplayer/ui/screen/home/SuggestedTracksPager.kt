package com.example.musicplayer.ui.screen.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
        val columnsPerPage = if (maxWidth < 400.dp) 1 else 2
        val songsPerColumn = 4
        val chunkSize = songsPerColumn * columnsPerPage
        val chunkedSongs = songs.chunked(chunkSize)
        val pagerState = rememberPagerState(pageCount = { chunkedSongs.size })

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = if (columnsPerPage == 1) {
                PaddingValues(start = 16.dp, end = 64.dp)
            } else {
                PaddingValues(horizontal = 16.dp)
            },
            pageSpacing = 16.dp,
            verticalAlignment = Alignment.Top
        ) { pageIndex ->
            val songsForPage = chunkedSongs[pageIndex]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                val firstColumnSize = if (columnsPerPage == 1) songsForPage.size else minOf(songsPerColumn, songsForPage.size)
                val firstColumnSongs = songsForPage.take(firstColumnSize)
                val secondColumnSongs = if (columnsPerPage > 1) songsForPage.drop(firstColumnSize) else emptyList()

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    firstColumnSongs.forEach { song ->
                        TrackItem(
                            song = song,
                            onClick = { onSongClick(song) },
                            onLongClick = { onShowSongOptions(song) }
                        )
                    }
                }

                if (columnsPerPage > 1) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        secondColumnSongs.forEach { song ->
                            TrackItem(
                                song = song,
                                onClick = { onSongClick(song) },
                                onLongClick = { onShowSongOptions(song) }
                            )
                        }
                    }
                }
            }
        }
    }
}