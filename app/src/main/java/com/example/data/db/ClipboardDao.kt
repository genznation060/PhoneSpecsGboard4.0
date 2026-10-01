package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ClipboardItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard_items ORDER BY isPinned DESC, isFavorite DESC, createdAt DESC")
    fun getAll(): Flow<List<ClipboardItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClipboardItem): Long

    @Delete
    suspend fun delete(item: ClipboardItem)

    @Query("DELETE FROM clipboard_items")
    suspend fun deleteAll()

    @Query("UPDATE clipboard_items SET isFavorite = :fav WHERE id = :id")
    suspend fun toggleFavorite(id: Long, fav: Boolean)

    @Query("UPDATE clipboard_items SET isPinned = :pin WHERE id = :id")
    suspend fun togglePin(id: Long, pin: Boolean)
}
