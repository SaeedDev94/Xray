package io.github.saeeddev94.xray.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import io.github.saeeddev94.xray.Settings
import io.github.saeeddev94.xray.service.TProxyService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        Settings.sync(context)
        if (!Settings.bootAutoStart) return
        val isPrepare = VpnService.prepare(context) == null
        if (!isPrepare) return
        Intent(context, TProxyService::class.java).also {
            it.action = TProxyService.START_VPN_SERVICE_ACTION_NAME
            context.startForegroundService(it)
        }
    }
}
