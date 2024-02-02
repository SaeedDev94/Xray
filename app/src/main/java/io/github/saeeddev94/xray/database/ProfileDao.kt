package io.github.saeeddev94.xray.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ProfileDao {
    @Query("SELECT `id`, `index`, `name` FROM profiles ORDER BY `index` ASC")
    fun all(): List<ProfileList>

    @Query("UPDATE profiles SET `index` = `index` + 1")
    fun shiftIndex()

    @Query("UPDATE profiles SET `index` = `index` - 1 WHERE `index` > :index")
    fun fixIndex(index: Int)

    @Query("SELECT * FROM profiles WHERE `id` = :id")
    fun find(id: Long): Profile

    @Insert
    fun insert(profile: Profile): Long

    @Update
    fun update(profile: Profile)

    @Delete
    fun delete(profile: Profile)
}
