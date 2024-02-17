package io.github.saeeddev94.xray

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class VpnTileService : TileService() {

    private var action: String = ""
    private var label: String = ""

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        requestListeningState(this, ComponentName(this, VpnTileService::class.java))
        action = intent?.getStringExtra("action") ?: ""
        label = intent?.getStringExtra("label") ?: ""
        handleUpdate()
        return START_STICKY
    }

    override fun onStartListening() {
        super.onStartListening()
        handleUpdate()
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

    private fun handleUpdate() {
        if (action.isNotEmpty() && label.isNotEmpty()) {
            when (action) {
                TProxyService.START_VPN_SERVICE_ACTION_NAME -> updateTile(Tile.STATE_ACTIVE, label)
                TProxyService.STOP_VPN_SERVICE_ACTION_NAME -> updateTile(Tile.STATE_INACTIVE, label)
            }
        }
    }

    private fun updateTile(newState: Int, newLabel: String) {
        val tile = qsTile ?: return
        tile.apply {
            state = newState
            label = newLabel
            icon = Icon.createWithResource(applicationContext, R.drawable.baseline_vpn_key)
            updateTile()
        }
        action = ""
        label = ""
    }

}
