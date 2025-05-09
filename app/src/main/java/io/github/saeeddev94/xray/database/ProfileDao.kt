package io.github.saeeddev94.xray.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.saeeddev94.xray.dto.ProfileList
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT `id`, `index`, `name` FROM profiles ORDER BY `index` ASC")
    fun all(): Flow<List<ProfileList>>

    @Query(
        "SELECT profiles.* FROM profiles" +
        "  INNER JOIN links ON profiles.link_id = links.id" +
        "  WHERE links.is_active = 1" +
        "  ORDER BY links.id DESC, `index` DESC"
    )
    suspend fun activeLinks(): List<Profile>

    @Query("SELECT * FROM profiles WHERE link_id = :linkId")
    suspend fun linkProfiles(linkId: Long): List<Profile>

    @Query("SELECT * FROM profiles WHERE `id` = :id")
    suspend fun find(id: Long): Profile

    @Insert
    suspend fun insert(profile: Profile): Long

    @Update
    suspend fun update(profile: Profile)

    @Delete
    suspend fun delete(profile: Profile)

    @Query("UPDATE profiles SET `index` = :index WHERE `id` = :id")
    suspend fun updateIndex(index: Int, id: Long)

    @Query("UPDATE profiles SET `index` = `index` + 1")
    suspend fun fixInsertIndex()

    @Query("UPDATE profiles SET `index` = `index` - 1 WHERE `index` > :index")
    suspend fun fixDeleteIndex(index: Int)

    @Query(
        "UPDATE profiles" +
        "  SET `index` = `index` + 1" +
        "  WHERE `index` >= :start" +
        "  AND `index` < :end" +
        "  AND `id` NOT IN (:exclude)"
    )
    suspend fun fixMoveUpIndex(start: Int, end: Int, exclude: Long)

    @Query(
        "UPDATE profiles" +
        "  SET `index` = `index` - 1" +
        "  WHERE `index` > :start" +
        "  AND `index` <= :end" +
        "  AND `id` NOT IN (:exclude)"
    )
    suspend fun fixMoveDownIndex(start: Int, end: Int, exclude: Long)
}
