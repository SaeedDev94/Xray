package io.github.saeeddev94.xray.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface LinkDao {
    @Query("SELECT * FROM links ORDER BY id DESC")
    fun all(): List<Link>

    @Insert
    fun insert(link: Link): Long

    @Update
    fun update(link: Link)

    @Delete
    fun delete(link: Link)
}
