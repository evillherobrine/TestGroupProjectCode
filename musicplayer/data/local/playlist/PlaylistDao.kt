package com.example.musicplayer.data.local.playlist

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Junction
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.example.musicplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow

data class PlaylistWithSongs(
    @Embedded
    val playlist: UserPlaylist,
    @Relation(
        parentColumn = "playlistId",
        entityColumn = "id",
        associateBy = Junction(PlaylistSongCrossRef::class)
    )
    val songs: List<FavouriteSong>
)
@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: UserPlaylist): Long
    @Query("SELECT * FROM user_playlists WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getPlaylistByName(name: String): UserPlaylist?
    @Transaction
    @Query("SELECT * FROM user_playlists")
    suspend fun getAllPlaylistsWithSongs(): List<PlaylistWithSongs>
    @Transaction
    @Query("SELECT * FROM user_playlists")
    fun getAllPlaylistsWithSongsFlow(): Flow<List<PlaylistWithSongs>>
    @Transaction
    @Query("SELECT * FROM user_playlists WHERE playlistId = :playlistId")
    suspend fun getPlaylistWithSongs(playlistId: Long): PlaylistWithSongs
    @Transaction
    @Query("SELECT * FROM user_playlists WHERE playlistId = :playlistId")
    fun getPlaylistWithSongsFlow(playlistId: Long): Flow<PlaylistWithSongs?>
    @Delete
    suspend fun deleteSongFromPlaylist(crossRef: PlaylistSongCrossRef)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistSongCrossRef(crossRef: PlaylistSongCrossRef)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongIfNotExists(song: FavouriteSong)
    @Query("DELETE FROM user_playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylistById(playlistId: Long)
    @Query("UPDATE user_playlists SET name = :name WHERE playlistId = :playlistId")
    suspend fun updatePlaylistName(playlistId: Long, name: String)
    @Query("""
        SELECT f.* FROM favourite_songs f
        INNER JOIN playlist_song_join j ON f.id = j.id
        WHERE j.playlistId = :playlistId
        ORDER BY j.orderIndex ASC
    """)
    fun getPlaylistSongsOrdered(playlistId: Long): Flow<List<FavouriteSong>>
    @Query("SELECT * FROM playlist_song_join WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    suspend fun getCrossRefs(playlistId: Long): List<PlaylistSongCrossRef>
    @Update
    suspend fun updateCrossRefs(crossRefs: List<PlaylistSongCrossRef>)
    @Query("SELECT MAX(orderIndex) FROM playlist_song_join WHERE playlistId = :playlistId")
    suspend fun getMaxOrderIndex(playlistId: Long): Int?
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistSongCrossRefs(crossRefs: List<PlaylistSongCrossRef>)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongsIfNotExists(songs: List<FavouriteSong>)
    @Transaction
    suspend fun addSongsToPlaylist(playlistId: Long, songs: List<Song>) {
        val favSongs = songs.map { it.toFavouriteSong().copy(isFavorite = false) }
        addSongsToPlaylistBatch(playlistId, favSongs)
    }
    @Transaction
    suspend fun addSongsToPlaylistBatch(playlistId: Long, songs: List<FavouriteSong>) {
        insertSongsIfNotExists(songs)
        val currentMaxOrder = getMaxOrderIndex(playlistId) ?: -1
        val crossRefs = songs.mapIndexed { index, song ->
            PlaylistSongCrossRef(
                playlistId = playlistId,
                id = song.id,
                orderIndex = currentMaxOrder + 1 + index
            )
        }
        insertPlaylistSongCrossRefs(crossRefs)
    }
}