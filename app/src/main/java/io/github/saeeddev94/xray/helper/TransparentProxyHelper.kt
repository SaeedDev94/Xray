package io.github.saeeddev94.xray.helper

import io.github.saeeddev94.xray.Settings

class TransparentProxyHelper(private val settings: Settings) {

    fun isRunning(): Boolean = settings.xrayCorePid().exists()
}
