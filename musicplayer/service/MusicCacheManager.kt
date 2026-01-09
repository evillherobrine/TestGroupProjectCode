package com.example.musicplayer.service

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import com.example.musicplayer.MusicPlayerApp
import com.example.musicplayer.cache.MusicCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@UnstableApi
class MusicCacheManager(private val context: Context) {
    private val preloadSongs = 2
    private val preloadSize = 500L * 1024L
    fun prefetchQueueWindow(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val queue = MusicPlayerApp.queueUseCase.queue.value
            val currentSong = MusicPlayerApp.queueUseCase.currentSong.value ?: return@launch
            val currentIndex = queue.indexOfFirst { it.id == currentSong.id }
            if (currentIndex == -1) return@launch
            val startIndex = currentIndex + 1
            val endIndex = (startIndex + preloadSongs).coerceAtMost(queue.size)
            for (i in startIndex until endIndex) {
                if (!isActive) break
                val songToPreload = queue[i]
                if (songToPreload.isLocal) continue
                try {
                    val playableSong = MusicPlayerApp.queueUseCase.getPlayableSong(songToPreload)
                    if (playableSong != null && playableSong.url.isNotEmpty()) {
                        downloadToCache(playableSong.url, playableSong.id.toString())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    private fun downloadToCache(url: String, cacheKey: String) {
        try {
            val uri = url.toUri()
            if (uri.scheme == "content" || uri.scheme == "file") return
            val cache = MusicCache.get(context)
            if (cache.isCached(cacheKey, 0, preloadSize)) return
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
            val upstreamDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
            val cacheDataSource = CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(upstreamDataSourceFactory)
                .createDataSource()
            val dataSpec = DataSpec.Builder()
                .setUri(uri)
                .setKey(cacheKey)
                .setLength(preloadSize)
                .build()
            val cacheWriter = CacheWriter(cacheDataSource, dataSpec, null, null)
            cacheWriter.cache()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}