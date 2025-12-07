package com.example.musicplayer.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
data class BatchSearchRequest(
    val queries: List<String>,
    val limit: Int
)
interface SoundCloudApi {
    @GET("search/tracks")
    suspend fun searchTrack(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): List<SoundCloudResponseItem>
    @GET("play/{id}")
    suspend fun getTrackInfo(@Path("id") id: Long): SoundCloudResponseItem
    @GET("search/playlists")
    suspend fun searchPlaylists(@Query("q") query: String,@Query("page") page: Int,@Query("limit") limit: Int): List<SoundCloudPlaylist>
    @GET("playlist/{id}")
    suspend fun getPlaylistTracks(@Path("id") playlistId: Long): List<SoundCloudResponseItem>
    @POST("search/batch")
    suspend fun searchBatch(
        @Body request: BatchSearchRequest
    ): List<SoundCloudResponseItem>
    @POST("search/batch_playlists")
    suspend fun searchBatchPlaylists(
        @Body request: BatchSearchRequest
    ): List<SoundCloudPlaylist>
}