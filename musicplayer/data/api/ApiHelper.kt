package com.example.musicplayer.data.api

import android.util.Log
import com.example.musicplayer.domain.model.Song
import retrofit2.HttpException
import java.io.IOException
import kotlinx.coroutines.CancellationException

object ApiHelper {
    private const val TAG = "ApiHelper"
    suspend fun getPlayableSong(song: Song): Song? {
        try {
            val track = RetrofitClient.api.getTrackInfo(song.id)
            if (track.stream_url.isNotBlank()) {
                val streamPath = track.stream_url.trim()
                val finalUrl = if (streamPath.startsWith("http://") || streamPath.startsWith("https://")) {
                    streamPath
                } else {
                    RetrofitClient.URL.trimEnd('/') + "/" + streamPath.removePrefix("/")
                }
                Log.d(TAG, "getPlayableUrl: Got PROXY stream URL: '$finalUrl'")
                return song.copy(
                    url = finalUrl,
                    lastFetchTime = System.currentTimeMillis())
            } else {
                Log.d(TAG, "getPlayableUrl: no stream_url or empty for id=${song.id}")
                return null
            }
        } catch (e: HttpException) {
            Log.w(TAG, "getPlayableUrl: HTTP error code=${e.code()}", e)
            return null
        } catch (e: IOException) {
            Log.w(TAG, "getPlayableUrl: Network fail ${e.message}", e)
            return null
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "getPlayableUrl: Unknown error", e)
            return null
        }
    }
}