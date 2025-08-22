package io.github.saeeddev94.xray.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import io.github.saeeddev94.xray.Settings
import io.github.saeeddev94.xray.helper.TransparentProxyHelper
import io.github.saeeddev94.xray.service.TProxyService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val systemUpTime = SystemClock.elapsedRealtime()
        val twoMinutes = 2L * 60L * 1000L
        val isAppLaunch = systemUpTime > twoMinutes
        if (
            context == null ||
            intent == null ||
            intent.action != Intent.ACTION_BOOT_COMPLETED ||
            isAppLaunch
        ) return
        val settings = Settings(context)
        val xrayCorePid = settings.xrayCorePid()
        val networkMonitorPid = settings.networkMonitorPid()
        if (xrayCorePid.exists()) xrayCorePid.delete()
        if (networkMonitorPid.exists()) networkMonitorPid.delete()
        if (!settings.bootAutoStart) {
            TProxyService.stop(context)
            return
        }
        if (settings.transparentProxy) {
            val pendingResult = goAsync()
            val transparentProxyHelper = TransparentProxyHelper(context, settings)
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                val state = transparentProxyHelper.networkState()
                val bypassWiFi = transparentProxyHelper.bypassWiFi(state)
                transparentProxyHelper.monitorNetwork()
                withContext(Dispatchers.Main) {
                    if (bypassWiFi) TProxyService.stop(context)
                    else TProxyService.start(context, false)
                    pendingResult.finish()
                }
            }
            return
        }
        TProxyService.start(context, true)
    }
}
