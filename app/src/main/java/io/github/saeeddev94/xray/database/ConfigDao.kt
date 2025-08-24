package io.github.saeeddev94.xray.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update

@Dao
interface ConfigDao {
    @Query("SELECT * FROM configs WHERE id = 1")
    suspend fun get(): Config

    @Update
    suspend fun update(config: Config)
}
