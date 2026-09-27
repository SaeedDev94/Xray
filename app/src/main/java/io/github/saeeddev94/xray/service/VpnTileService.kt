package io.github.saeeddev94.xray.service

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.edit
import io.github.saeeddev94.xray.R

class VpnTileService : TileService() {

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

    private fun handleUpdate() {
        val sharedPref = sharedPref(applicationContext)
        val label = sharedPref.getString(PREF_LABEL, null) ?: return
        val active = sharedPref.getBoolean(PREF_ACTIVE, false)
        updateTile(if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE, label)
    }

    private fun updateTile(newState: Int, newLabel: String) {
        qsTile?.apply {
            state = newState
            label = newLabel
            icon = Icon.createWithResource(applicationContext, R.drawable.baseline_vpn_key)
            updateTile()
        }
    }

    companion object {
        private const val PREF_NAME = "vpn_tile"
        private const val PREF_ACTIVE = "active"
        private const val PREF_LABEL = "label"

        fun update(context: Context, active: Boolean, label: String) {
            sharedPref(context).edit {
                putBoolean(PREF_ACTIVE, active)
                putString(PREF_LABEL, label)
            }
            requestListeningState(context, ComponentName(context, VpnTileService::class.java))
        }

        private fun sharedPref(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        }
    }

}
