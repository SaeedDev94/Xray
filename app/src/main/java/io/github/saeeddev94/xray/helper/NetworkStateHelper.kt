package io.github.saeeddev94.xray.helper

import com.topjohnwu.superuser.Shell
import io.github.saeeddev94.xray.service.TProxyService
import java.io.File

class NetworkStateHelper() {

    fun monitor(script: File, pid: File) {
        if (!script.exists()) makeScript(script)
        val file = "/data/misc/net/rt_tables"
        val cmd = "nohup inotifyd ${script.absolutePath} $file:w" +
                " > /dev/null 2>&1 & echo $! > ${pid.absolutePath}"
        Shell.cmd(cmd).exec()
    }

    fun getState(): NetworkState {
        return NetworkState(getWifi(), getData())
    }

    fun isOnline(state: NetworkState): Boolean {
        return state.wifi != null || state.data
    }

    private fun makeScript(file: File) {
        val pkg = TProxyService.PKG_NAME
        val action = TProxyService.NETWORK_UPDATE_SERVICE_ACTION_NAME
        val content = arrayListOf(
            "#!/bin/sh",
            "",
            "am start-foreground-service -n $pkg/.service.TProxyService -a $action",
            "",
        ).joinToString("\n")
        FileHelper.createOrUpdate(file, content)
        Shell.cmd("chown root:root ${file.absolutePath}").exec()
        Shell.cmd("chmod +x ${file.absolutePath}").exec()
    }

    private fun getWifi(): String? {
        val cmd = "dumpsys wifi" +
                " | grep 'mWifiInfo SSID'" +
                " | awk -F 'SSID: ' '{print $2}'" +
                " | awk -F ',' '{print $1}'" +
                " | head -n 1"
        val result = Shell.cmd(cmd).exec()
        if (!result.isSuccess || result.out.isEmpty()) return null
        val ssid = result.out.first()
        if (ssid == "<unknown ssid>") return null
        return ssid.trim('"')
    }

    private fun getData(): Boolean {
        val cmd = "settings get global mobile_data" +
                " && settings get global mobile_data1" +
                " && settings get global mobile_data2"
        val result = Shell.cmd(cmd).exec()
        if (!result.isSuccess || result.out.isEmpty()) return false
        return result.out.contains("1")
    }

    data class NetworkState(
        val wifi: String?,
        val data: Boolean,
    )
}
