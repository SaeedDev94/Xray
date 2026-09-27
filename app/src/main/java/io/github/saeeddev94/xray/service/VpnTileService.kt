package io.github.saeeddev94.xray.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.github.saeeddev94.xray.BuildConfig
import io.github.saeeddev94.xray.R
import io.github.saeeddev94.xray.Settings

class VpnTileService : TileService() {

    private val settings by lazy { Settings(applicationContext) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != UPDATE_TILE_ACTION_NAME) return START_NOT_STICKY
        settings.tileActive = intent.getBooleanExtra(EXTRA_ACTIVE, false)
        settings.tileLabel = intent.getStringExtra(EXTRA_LABEL)
        handleUpdate()
        requestListeningState(this, ComponentName(this, VpnTileService::class.java))
        return START_NOT_STICKY
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

    private fun handleUpdate() {
        val label = settings.tileLabel ?: getString(R.string.appName)
        val state = if (settings.tileActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        updateTile(state, label)
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
        private const val PKG_NAME = BuildConfig.APPLICATION_ID
        private const val UPDATE_TILE_ACTION_NAME = "$PKG_NAME.TileUpdate"
        private const val EXTRA_ACTIVE = "active"
        private const val EXTRA_LABEL = "label"

        fun update(context: Context, active: Boolean, label: String) {
            Intent(context, VpnTileService::class.java).also {
                it.action = UPDATE_TILE_ACTION_NAME
                it.putExtra(EXTRA_ACTIVE, active)
                it.putExtra(EXTRA_LABEL, label)
                context.startService(it)
            }
        }
    }
}
