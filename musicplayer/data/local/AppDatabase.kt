package com.example.musicplayer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.musicplayer.data.local.history.HistoryDao
import com.example.musicplayer.data.local.history.HistoryEntry
import com.example.musicplayer.data.local.playlist.FavouriteDao
import com.example.musicplayer.data.local.playlist.FavouriteSong
import com.example.musicplayer.data.local.playlist.PlaylistDao
import com.example.musicplayer.data.local.playlist.PlaylistSongCrossRef
import com.example.musicplayer.data.local.playlist.UserPlaylist
import com.example.musicplayer.data.local.queue.QueueDao
import com.example.musicplayer.data.local.queue.QueueEntry
import com.example.musicplayer.data.local.search.SearchHistoryEntry
import com.example.musicplayer.data.local.search.SearchHistoryDao

@Database(
    entities = [
        FavouriteSong::class,
        UserPlaylist::class,
        PlaylistSongCrossRef::class,
        HistoryEntry::class,
        QueueEntry::class,
        SearchHistoryEntry::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favouriteDao(): FavouriteDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
    abstract fun queueDao(): QueueDao
    abstract fun searchQueryDao(): SearchHistoryDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "music_app_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}