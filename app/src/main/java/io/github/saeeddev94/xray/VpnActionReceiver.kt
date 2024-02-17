package io.github.saeeddev94.xray

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class VpnActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        val action = intent?.action ?: ""
        val label = intent?.getStringExtra("profile") ?: context.getString(R.string.appName)
        Intent(context, VpnTileService::class.java).also {
            it.putExtra("action", action)
            it.putExtra("label", label)
            context.startService(it)
        }
    }

}
