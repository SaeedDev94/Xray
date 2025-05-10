package io.github.saeeddev94.xray.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.saeeddev94.xray.Xray
import io.github.saeeddev94.xray.database.Link
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LinkViewModel(application: Application) : AndroidViewModel(application) {

    private val linkRepository by lazy { getApplication<Xray>().linkRepository }

    var activeTab: Long = 0L
    val tabs = linkRepository.tabs.flowOn(Dispatchers.IO).stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        listOf(),
    )
    val links = linkRepository.all.flowOn(Dispatchers.IO).stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        listOf(),
    )

    suspend fun activeLinks(): List<Link> {
        return linkRepository.activeLinks()
    }

    fun insert(link: Link) = viewModelScope.launch {
        linkRepository.insert(link)
    }

    fun update(link: Link) = viewModelScope.launch {
        linkRepository.update(link)
    }

    fun delete(link: Link) = viewModelScope.launch {
        linkRepository.delete(link)
    }
}
