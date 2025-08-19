package io.github.saeeddev94.xray.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.saeeddev94.xray.service.TProxyService

class NetworkUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (
            context == null ||
            intent == null ||
            intent.action !== TProxyService.NETWORK_UPDATE_SERVICE_ACTION_NAME
        ) return
        TProxyService.networkUpdate(context)
    }
}
