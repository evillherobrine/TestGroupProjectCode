package com.example.musicplayer.ui.screen.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicplayer.R
import com.example.musicplayer.domain.model.Song
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LargeCover(
    queue: List<Song>,
    currentCoverUrlXL: String?,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .aspectRatio(1f)
    ) {
        if (queue.isEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(currentCoverUrlXL)
                    .crossfade(true)
                    .placeholder(R.drawable.image_24px)
                    .error(R.drawable.image_24px)
                    .build(),
                contentDescription = "Album Cover",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
        } else {
            HorizontalPager(
                state = pagerState,
                pageSpacing = 16.dp,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val pageOffset by remember(pagerState, page) {
                    derivedStateOf {
                        ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                    }
                }
                val scale = lerp(
                    start = 0.95f,
                    stop = 1f,
                    fraction = 1f - pageOffset.coerceIn(0f, 1f)
                )
                val alphaValue = lerp(
                    start = 0.7f,
                    stop = 1f,
                    fraction = 1f - pageOffset.coerceIn(0f, 1f)
                )
                val songAtPage = queue.getOrNull(page)
                val coverUrlAtPage = songAtPage?.coverXL ?: songAtPage?.cover ?: ""
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverUrlAtPage)
                        .crossfade(true)
                        .placeholder(R.drawable.image_24px)
                        .error(R.drawable.image_24px)
                        .build(),
                    contentDescription = "Album Cover Pager",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            alpha = alphaValue
                        }
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}