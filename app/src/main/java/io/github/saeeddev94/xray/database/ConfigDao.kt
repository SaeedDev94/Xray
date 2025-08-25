package io.github.saeeddev94.xray.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ConfigDao {
    @Query("SELECT * FROM configs WHERE id = 1")
    suspend fun get(): Config?

    @Insert
    suspend fun insert(config: Config)

    @Update
    suspend fun update(config: Config)
}
