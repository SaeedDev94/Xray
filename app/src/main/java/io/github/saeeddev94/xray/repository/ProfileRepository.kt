package io.github.saeeddev94.xray.repository

import io.github.saeeddev94.xray.database.Profile
import io.github.saeeddev94.xray.database.ProfileDao

class ProfileRepository(private val profileDao: ProfileDao) {

    val all = profileDao.all()

    suspend fun linkProfiles(linkId: Long): List<Profile> {
        return profileDao.linkProfiles(linkId)
    }

    suspend fun find(id: Long): Profile {
        return profileDao.find(id)
    }

    suspend fun update(profile: Profile) {
        profileDao.update(profile)
    }

    suspend fun create(profile: Profile) {
        profileDao.create(profile)
    }

    suspend fun remove(profile: Profile) {
        profileDao.remove(profile)
    }

    suspend fun moveUp(start: Int, end: Int, exclude: Long) {
        profileDao.moveUp(start, end, exclude)
    }

    suspend fun moveDown(start: Int, end: Int, exclude: Long) {
        profileDao.moveDown(start, end, exclude)
    }
}
