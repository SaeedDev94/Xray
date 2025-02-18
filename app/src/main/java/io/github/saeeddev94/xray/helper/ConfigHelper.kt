package io.github.saeeddev94.xray.helper

import io.github.saeeddev94.xray.utils.XrayCore
import android.content.Context
import io.github.saeeddev94.xray.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ConfigHelper {
    companion object {
        suspend fun isValid(context: Context, json: String): String {
            return withContext(Dispatchers.IO) {
                val pwd = context.filesDir.absolutePath
                val testConfig = Settings.testConfig(context)
                FileHelper().createOrUpdate(testConfig, json)
                XrayCore.test(pwd, testConfig.absolutePath)
            }
        }
    }
}
