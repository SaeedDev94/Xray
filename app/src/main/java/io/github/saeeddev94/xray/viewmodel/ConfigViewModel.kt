package io.github.saeeddev94.xray.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.saeeddev94.xray.Xray
import io.github.saeeddev94.xray.database.Config
import kotlinx.coroutines.launch

class ConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val configRepository by lazy { getApplication<Xray>().configRepository }

    suspend fun get() = configRepository.get()

    fun update(config: Config) = viewModelScope.launch {
        configRepository.update(config)
    }
}
