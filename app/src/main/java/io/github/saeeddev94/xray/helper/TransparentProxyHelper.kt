package io.github.saeeddev94.xray.helper

import com.topjohnwu.superuser.Shell
import io.github.saeeddev94.xray.Settings

class TransparentProxyHelper(private val settings: Settings) {

    fun isRunning(): Boolean = settings.xrayCorePid().exists()

    fun startService() {
        makeConfig()
        Shell.cmd("${cmd()} service start").exec()
    }

    fun stopService() {
        Shell.cmd("${cmd()} service stop").exec()
    }

    fun enableProxy() {
        Shell.cmd("${cmd()} proxy enable").exec()
    }

    fun disableProxy() {
        Shell.cmd("${cmd()} proxy disable").exec()
    }

    private fun cmd(): String {
        return arrayListOf(
            settings.xrayHelperFile().absolutePath,
            "-c",
            settings.xrayHelperConfig().absolutePath,
        ).joinToString(" ")
    }

    private fun makeConfig() {
        val yml = arrayListOf(
            "xrayHelper:",
            "  coreType: xray",
            "  corePath: ${settings.xrayCoreFile().absolutePath}",
            "  coreConfig: ${settings.xrayConfig().absolutePath}",
            "  dataDir: ${settings.baseDir().absolutePath}",
            "  runDir: ${settings.baseDir().absolutePath}",
            "proxy:",
            "  method: tproxy",
            "  tproxyPort: ${settings.tproxyPort}",
            "  enableIPv6: ${settings.enableIpV6}",
            "  mode: ${if (settings.appsRoutingMode) "blacklist" else "whitelist"}",
        )
        val appsList = settings.appsRouting.split("\n")
            .map { it.trim() }
            .filter { it.trim().isNotBlank() }
        if (appsList.isNotEmpty()) {
            yml.add("  pkgList:")
            appsList.forEach { yml.add("    - $it") }
        }
        yml.add("")
        FileHelper.createOrUpdate(
            settings.xrayHelperConfig(),
            yml.joinToString("\n")
        )
    }
}
