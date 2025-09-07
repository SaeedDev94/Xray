package io.github.saeeddev94.xray.helper

import android.content.Context
import com.topjohnwu.superuser.Shell
import io.github.saeeddev94.xray.BuildConfig
import io.github.saeeddev94.xray.Settings
import io.github.saeeddev94.xray.service.TProxyService
import java.io.FileOutputStream

class TransparentProxyHelper(
    private val context: Context,
    private val settings: Settings,
) {

    private val networkStateHelper by lazy { NetworkStateHelper() }

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

    fun refreshProxy() {
        Shell.cmd("${cmd()} proxy refresh").exec()
    }

    fun kill() {
        if (settings.networkMonitorPid().exists()) {
            val path = settings.networkMonitorPid().absolutePath
            Shell.cmd("kill $(cat $path) && rm $path").exec()
        }
        if (settings.xrayCorePid().exists()) {
            disableProxy()
            stopService()
        }
    }

    fun monitorNetwork() {
        val script = settings.networkMonitorScript()
        val pid = settings.networkMonitorPid()
        if (!settings.tproxyAutoConnect || !settings.transparentProxy || pid.exists()) return
        networkStateHelper.monitor(script, pid)
    }

    fun networkState(): NetworkStateHelper.NetworkState {
        return networkStateHelper.getState()
    }

    fun bypassWiFi(state: NetworkStateHelper.NetworkState): Boolean {
        val tproxyBypassWiFi = settings.tproxyBypassWiFi
        val ssid = state.wifi ?: ""
        return tproxyBypassWiFi.isNotEmpty() && tproxyBypassWiFi.contains(ssid)
    }

    fun networkUpdate(value: NetworkStateHelper.NetworkState? = null) {
        if (!settings.tproxyAutoConnect) return
        val state = value ?: networkState()
        val isOnline = networkStateHelper.isOnline(state)
        val isRunning = settings.xrayCorePid().exists()
        if (!isOnline || bypassWiFi(state)) {
            TProxyService.stop(context)
            return
        }
        if (!isRunning) {
            TProxyService.start(context, false)
            return
        }
        refreshProxy()
    }

    fun install() {
        val xrayHelper = settings.xrayHelperFile()
        val appVersionCode = BuildConfig.VERSION_CODE
        val xrayHelperVersionCode = settings.xrayHelperVersionCode
        if (xrayHelper.exists() && xrayHelperVersionCode == appVersionCode) return
        if (xrayHelper.exists()) xrayHelper.delete()
        context.assets.open(xrayHelper.name).use { input ->
            FileOutputStream(xrayHelper).use { output ->
                input.copyTo(output)
            }
        }
        Shell.cmd("chown root:root ${xrayHelper.absolutePath}").exec()
        Shell.cmd("chmod +x ${xrayHelper.absolutePath}").exec()
        settings.xrayHelperVersionCode = appVersionCode
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

        val includedInterfaces = arrayListOf<String>()
        val excludedInterfaces = arrayListOf<String>()
        if (settings.tproxyHotspot) includedInterfaces.add(settings.hotspotInterface)
        else excludedInterfaces.add(settings.hotspotInterface)
        if (settings.tproxyTethering) includedInterfaces.add(settings.tetheringInterface)
        else excludedInterfaces.add(settings.tetheringInterface)
        if (includedInterfaces.isNotEmpty()) {
            yml.add("  apList:")
            includedInterfaces.forEach { yml.add("    - $it") }
        }
        if (excludedInterfaces.isNotEmpty()) {
            yml.add("  ignoreList:")
            excludedInterfaces.forEach { yml.add("    - $it") }
        }

        yml.add("")
        FileHelper.createOrUpdate(
            settings.xrayHelperConfig(),
            yml.joinToString("\n")
        )
    }
}
