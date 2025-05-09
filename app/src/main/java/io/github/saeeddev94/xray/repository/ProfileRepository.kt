package io.github.saeeddev94.xray.repository

import io.github.saeeddev94.xray.database.Profile
import io.github.saeeddev94.xray.database.ProfileDao

class ProfileRepository(private val profileDao: ProfileDao) {

    val all = profileDao.all()

    suspend fun activeLinks(): List<Profile> {
        return profileDao.activeLinks()
    }

    suspend fun linkProfiles(linkId: Long): List<Profile> {
        return profileDao.linkProfiles(linkId)
    }

    suspend fun find(id: Long): Profile {
        return profileDao.find(id)
    }

    suspend fun insert(profile: Profile): Long {
        return profileDao.insert(profile)
    }

    suspend fun update(profile: Profile) {
        profileDao.update(profile)
    }

    suspend fun delete(profile: Profile) {
        profileDao.delete(profile)
    }

    suspend fun updateIndex(index: Int, id: Long) {
        profileDao.updateIndex(index, id)
    }

    suspend fun fixInsertIndex() {
        profileDao.fixInsertIndex()
    }

    suspend fun fixDeleteIndex(index: Int) {
        profileDao.fixDeleteIndex(index)
    }

    suspend fun fixMoveUpIndex(start: Int, end: Int, exclude: Long) {
        profileDao.fixMoveUpIndex(start, end, exclude)
    }

    suspend fun fixMoveDownIndex(start: Int, end: Int, exclude: Long) {
        profileDao.fixMoveDownIndex(start, end, exclude)
    }
}
