package io.github.saeeddev94.xray.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkDao {
    @Query("SELECT * FROM links ORDER BY id DESC")
    fun all(): Flow<List<Link>>

    @Insert
    suspend fun insert(link: Link): Long

    @Update
    suspend fun update(link: Link)

    @Delete
    suspend fun delete(link: Link)
}
