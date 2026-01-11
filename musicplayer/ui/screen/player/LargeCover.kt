package com.example.musicplayer.ui.screen.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicplayer.R

@Composable
fun LargeCover(
    currentCoverUrlXL: String?
) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(currentCoverUrlXL)
                .crossfade(true)
                .placeholder(R.drawable.music_note)
                .error(R.drawable.music_note)
                .build(),
            contentDescription = "Album Cover",
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop
        )
}