package io.github.saeeddev94.xray.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkDao {
    @Query("SELECT * FROM links ORDER BY id ASC")
    fun all(): Flow<List<Link>>

    @Query("SELECT * FROM links WHERE is_active = 1 ORDER BY id ASC")
    fun tabs(): Flow<List<Link>>

    @Query("SELECT * FROM links WHERE is_active = 1 ORDER BY id ASC")
    suspend fun activeLinks(): List<Link>

    @Insert
    suspend fun insert(link: Link): Long

    @Update
    suspend fun update(link: Link)

    @Delete
    suspend fun delete(link: Link)
}
