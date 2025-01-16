package io.github.saeeddev94.xray.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.github.saeeddev94.xray.Xray
import io.github.saeeddev94.xray.database.Profile
import io.github.saeeddev94.xray.dto.ProfileList

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val profileRepository by lazy { getApplication<Xray>().profileRepository }

    suspend fun all(): List<ProfileList> {
        return profileRepository.all()
    }

    suspend fun activeLinks(): List<Profile> {
        return profileRepository.activeLinks()
    }

    suspend fun linkProfiles(linkId: Long): List<Profile> {
        return profileRepository.linkProfiles(linkId)
    }

    suspend fun find(id: Long): Profile {
        return profileRepository.find(id)
    }

    suspend fun insert(profile: Profile): Long {
        return profileRepository.insert(profile)
    }

    suspend fun update(profile: Profile) {
        profileRepository.update(profile)
    }

    suspend fun delete(profile: Profile) {
        profileRepository.delete(profile)
    }

    suspend fun updateIndex(index: Int, id: Long) {
        profileRepository.updateIndex(index, id)
    }

    suspend fun fixInsertIndex() {
        profileRepository.fixInsertIndex()
    }

    suspend fun fixDeleteIndex(index: Int) {
        profileRepository.fixDeleteIndex(index)
    }

    suspend fun fixMoveUpIndex(start: Int, end: Int, exclude: Long) {
        profileRepository.fixMoveUpIndex(start, end, exclude)
    }

    suspend fun fixMoveDownIndex(start: Int, end: Int, exclude: Long) {
        profileRepository.fixMoveDownIndex(start, end, exclude)
    }
}
