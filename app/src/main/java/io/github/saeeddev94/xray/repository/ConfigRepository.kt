package io.github.saeeddev94.xray.repository

import io.github.saeeddev94.xray.database.Config
import io.github.saeeddev94.xray.database.ConfigDao

class ConfigRepository(private val configDao: ConfigDao) {

    suspend fun get(): Config = configDao.get()

    suspend fun update(config: Config) {
        configDao.update(config)
    }
}
