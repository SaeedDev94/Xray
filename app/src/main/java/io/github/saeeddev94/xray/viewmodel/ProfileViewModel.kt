package io.github.saeeddev94.xray.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.saeeddev94.xray.Xray
import io.github.saeeddev94.xray.database.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val profileRepository by lazy { getApplication<Xray>().profileRepository }

    val profiles = profileRepository.all.flowOn(Dispatchers.IO).stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        listOf(),
    )

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
