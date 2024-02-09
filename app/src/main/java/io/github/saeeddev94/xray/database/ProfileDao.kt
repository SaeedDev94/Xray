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
    fun fixInsertIndex()

    @Query("UPDATE profiles SET `index` = `index` - 1 WHERE `index` > :index")
    fun fixDeleteIndex(index: Int)

    @Query("UPDATE profiles SET `index` = :index WHERE `id` = :id")
    fun updateIndex(index: Int, id: Long)

    @Query("UPDATE profiles SET `index` = `index` + 1 WHERE `index` >= :start AND `index` < :end AND `id` NOT IN (:exclude)")
    fun fixMoveUpIndex(start: Int, end: Int, exclude: Long)

    @Query("UPDATE profiles SET `index` = `index` - 1 WHERE `index` > :start AND `index` <= :end AND `id` NOT IN (:exclude)")
    fun fixMoveDownIndex(start: Int, end: Int, exclude: Long)

    @Query("SELECT * FROM profiles WHERE `id` = :id")
    fun find(id: Long): Profile

    @Insert
    fun insert(profile: Profile): Long

    @Update
    fun update(profile: Profile)

    @Delete
    fun delete(profile: Profile)
}
