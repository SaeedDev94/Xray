package io.github.saeeddev94.xray.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.saeeddev94.xray.Settings
import io.github.saeeddev94.xray.service.TProxyService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null || intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val settings = Settings(context)
        val transparentProxy = settings.transparentProxy
        if (!settings.bootAutoStart) return
        TProxyService.start(
            context, !transparentProxy, !transparentProxy
        )
    }
}
