package io.github.saeeddev94.xray.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.saeeddev94.xray.Xray
import io.github.saeeddev94.xray.database.Profile
import io.github.saeeddev94.xray.dto.ProfileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val profileRepository by lazy { getApplication<Xray>().profileRepository }

    val profiles = profileRepository.all.flowOn(Dispatchers.IO).stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        listOf(),
    )
    val filtered = MutableSharedFlow<List<ProfileList>>()

    fun next(link: Long) = viewModelScope.launch {
        val list = profiles.value.filter { link == 0L || link == it.link }
        filtered.emit(list)
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

    suspend fun create(profile: Profile) {
        return profileRepository.create(profile)
    }

    suspend fun update(profile: Profile) {
        profileRepository.update(profile)
    }

    suspend fun remove(profile: Profile) {
        profileRepository.remove(profile)
    }

    suspend fun moveUp(start: Int, end: Int, exclude: Long) {
        profileRepository.moveUp(start, end, exclude)
    }

    suspend fun moveDown(start: Int, end: Int, exclude: Long) {
        profileRepository.moveDown(start, end, exclude)
    }
}
