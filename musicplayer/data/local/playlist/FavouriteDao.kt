package com.example.musicplayer.data.local.playlist

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteDao {
    @Query("SELECT * FROM favourite_songs WHERE isFavorite = 1")
    fun getFavoritesFlow(): Flow<List<FavouriteSong>>
    @Query("SELECT * FROM favourite_songs WHERE id = :songId LIMIT 1")
    suspend fun getById(songId: Long): FavouriteSong?
    @Query("SELECT EXISTS(SELECT * FROM favourite_songs WHERE id = :songId AND isFavorite = 1)")
    suspend fun isFavorite(songId: Long): Boolean
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: FavouriteSong)
    @Delete
    suspend fun delete(song: FavouriteSong)
    @Query("UPDATE favourite_songs SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)
}