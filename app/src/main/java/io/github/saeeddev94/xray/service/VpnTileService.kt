package io.github.saeeddev94.xray.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.edit
import io.github.saeeddev94.xray.R

class VpnTileService : TileService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        requestListeningState(this, ComponentName(this, VpnTileService::class.java))
        val action = intent?.getStringExtra("action") ?: ""
        val label = intent?.getStringExtra("label") ?: ""
        val sharedPref = sharedPref()
        sharedPref.edit {
            putString("action", action)
            putString("label", label)
        }
        handleUpdate(action, label)
        return START_STICKY
    }

    override fun onStartListening() {
        super.onStartListening()
        handleUpdate()
    }

    override fun onClick() {
        super.onClick()
        when (qsTile?.state) {
            Tile.STATE_INACTIVE -> TProxyService.start(applicationContext)
            Tile.STATE_ACTIVE -> TProxyService.stop(applicationContext)
        }
    }

    private fun handleUpdate(newAction: String? = null, newLabel: String? = null) {
        val sharedPref = sharedPref()
        val action = newAction ?: sharedPref.getString("action", "")!!
        val label = newLabel ?: sharedPref.getString("label", "")!!
        if (action.isNotEmpty() && label.isNotEmpty()) {
            when (action) {
                TProxyService.START_VPN_SERVICE_ACTION_NAME,
                TProxyService.NEW_CONFIG_SERVICE_ACTION_NAME -> updateTile(Tile.STATE_ACTIVE, label)

                TProxyService.STOP_VPN_SERVICE_ACTION_NAME -> updateTile(Tile.STATE_INACTIVE, label)
            }
        }
    }

    private fun updateTile(newState: Int, newLabel: String) {
        val tile = qsTile ?: return
        tile.apply {
            state = newState
            label = newLabel
            icon = Icon.createWithResource(applicationContext, R.drawable.vpn_key)
            updateTile()
        }
    }

    private fun sharedPref(): SharedPreferences {
        return getSharedPreferences("vpn_tile", Context.MODE_PRIVATE)
    }

}
