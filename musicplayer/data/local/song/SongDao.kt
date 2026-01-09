package com.example.musicplayer.data.local.song

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.musicplayer.domain.model.Song

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<Song>)

    @Query("SELECT * FROM song")
    suspend fun getAllSongs(): List<Song>

    @Query("SELECT * FROM song WHERE id = :songId")
    suspend fun getSongById(songId: Long): Song?
}
