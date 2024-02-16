package io.github.saeeddev94.xray

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

class ToggleService : TileService() {

    private val toggleVpnAction: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val profile = intent?.getStringExtra("profile") ?: ""
            when (intent?.action) {
                TProxyService.START_VPN_SERVICE_ACTION_NAME -> updateTile(Tile.STATE_ACTIVE, profile)
                TProxyService.STOP_VPN_SERVICE_ACTION_NAME -> updateTile(Tile.STATE_INACTIVE, getString(R.string.appName))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()
        IntentFilter().also {
            it.addAction(TProxyService.START_VPN_SERVICE_ACTION_NAME)
            it.addAction(TProxyService.STOP_VPN_SERVICE_ACTION_NAME)
            registerReceiver(toggleVpnAction, it, RECEIVER_NOT_EXPORTED)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        requestListeningState(this, ComponentName(this, ToggleService::class.java))
        return START_STICKY
    }

    override fun onClick() {
        super.onClick()
        when (qsTile.state) {
            Tile.STATE_INACTIVE -> {
                Intent(applicationContext, TProxyService::class.java).also {
                    startForegroundService(it)
                }
            }
            Tile.STATE_ACTIVE -> {
                Intent(TProxyService.STOP_VPN_SERVICE_ACTION_NAME).also {
                    it.`package` = BuildConfig.APPLICATION_ID
                    sendBroadcast(it)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(toggleVpnAction)
    }

    private fun updateTile(newState: Int, newLabel: String) {
        qsTile?.apply {
            state = newState
            label = newLabel
            icon = Icon.createWithResource(applicationContext, R.drawable.baseline_vpn_key)
            updateTile()
        }
    }

}
