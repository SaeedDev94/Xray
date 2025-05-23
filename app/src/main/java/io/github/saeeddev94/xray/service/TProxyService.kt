package io.github.saeeddev94.xray.service

import XrayCore.XrayCore
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import io.github.saeeddev94.xray.BuildConfig
import io.github.saeeddev94.xray.R
import io.github.saeeddev94.xray.Settings
import io.github.saeeddev94.xray.Xray
import io.github.saeeddev94.xray.activity.MainActivity
import io.github.saeeddev94.xray.database.Profile
import io.github.saeeddev94.xray.dto.XrayConfig
import io.github.saeeddev94.xray.helper.FileHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import kotlin.reflect.cast

class TProxyService : VpnService() {

    companion object {
        init {
            System.loadLibrary("hev-socks5-tunnel")
        }

        const val STATUS_VPN_SERVICE_ACTION_NAME = "${BuildConfig.APPLICATION_ID}.VpnStatus"
        const val STOP_VPN_SERVICE_ACTION_NAME = "${BuildConfig.APPLICATION_ID}.VpnStop"
        const val START_VPN_SERVICE_ACTION_NAME = "${BuildConfig.APPLICATION_ID}.VpnStart"
        private const val VPN_SERVICE_NOTIFICATION_ID = 1
        private const val OPEN_MAIN_ACTIVITY_ACTION_ID = 2
        private const val STOP_VPN_SERVICE_ACTION_ID = 3

        fun status(context: Context) = startCommand(context, STATUS_VPN_SERVICE_ACTION_NAME)
        fun stop(context: Context) = startCommand(context, STOP_VPN_SERVICE_ACTION_NAME)

        fun start(context: Context, check: Boolean = true) {
            if (check && prepare(context) != null) {
                Log.e(
                    "TProxyService",
                    "Can't start: VpnService#prepare(): needs user permission"
                )
                return
            }
            startCommand(context, START_VPN_SERVICE_ACTION_NAME, true)
        }

        private fun startCommand(context: Context, name: String, foreground: Boolean = false) {
            Intent(context, TProxyService::class.java).also {
                it.action = name
                if (foreground) {
                    context.startForegroundService(it)
                } else {
                    context.startService(it)
                }
            }
        }
    }

    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private val profileRepository by lazy { Xray::class.cast(application).profileRepository }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var isRunning: Boolean = false
    private var tunDevice: ParcelFileDescriptor? = null

    private external fun TProxyStartService(configPath: String, fd: Int)
    private external fun TProxyStopService()
    private external fun TProxyGetStats(): LongArray

    override fun onCreate() {
        super.onCreate()
        Settings.sync(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            when (intent?.action) {
                START_VPN_SERVICE_ACTION_NAME -> start(getProfile())
                STOP_VPN_SERVICE_ACTION_NAME -> stopVPN()
                STATUS_VPN_SERVICE_ACTION_NAME -> statusVPN()
            }
        }
        return START_STICKY
    }

    override fun onRevoke() {
        stopVPN()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun getProfile(): Profile? {
        return if (Settings.selectedProfile == 0L) {
            null
        } else {
            profileRepository.find(Settings.selectedProfile)
        }
    }

    private fun getConfig(profile: Profile): XrayConfig? {
        val dir: File = applicationContext.filesDir
        val config: File = Settings.xrayConfig(applicationContext)
        FileHelper.createOrUpdate(config, profile.config)
        val error: String = XrayCore.test(dir.absolutePath, config.absolutePath)
        if (error.isNotEmpty()) {
            showToast(error)
            return null
        }
        return XrayConfig(dir.absolutePath, config.absolutePath)
    }

    private fun start(profile: Profile?) {
        if (profile == null) {
            startVPN(null)
        } else {
            getConfig(profile)?.let {
                startXray(it)
                startVPN(profile.name)
            }
        }
    }

    private fun startXray(config: XrayConfig) {
        XrayCore.start(config.dir, config.file)
    }

    private fun stopXray() {
        XrayCore.stop()
    }

    private fun startVPN(configName: String?) {
        /** Create Tun */
        val tun = Builder()
        val tunName = getString(R.string.appName)

        /** Basic tun config */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tun.setMetered(false)
        tun.setMtu(Settings.tunMtu)
        tun.setSession(tunName)

        /** IPv4 */
        tun.addAddress(Settings.tunAddress, Settings.tunPrefix)
        tun.addDnsServer(Settings.primaryDns)
        tun.addDnsServer(Settings.secondaryDns)

        /** IPv6 */
        if (Settings.enableIpV6) {
            tun.addAddress(Settings.tunAddressV6, Settings.tunPrefixV6)
            tun.addDnsServer(Settings.primaryDnsV6)
            tun.addDnsServer(Settings.secondaryDnsV6)
            tun.addRoute("::", 0)
        }

        /** Bypass LAN (IPv4) */
        if (Settings.bypassLan) {
            resources.getStringArray(R.array.publicIpAddresses).forEach {
                val address = it.split('/')
                tun.addRoute(address[0], address[1].toInt())
            }
        } else {
            tun.addRoute("0.0.0.0", 0)
        }

        /** Apps Routing */
        if (Settings.appsRoutingMode) tun.addDisallowedApplication(applicationContext.packageName)
        Settings.appsRouting.split("\n").forEach {
            val packageName = it.trim()
            if (packageName.isBlank()) return@forEach
            if (Settings.appsRoutingMode) tun.addDisallowedApplication(packageName)
            else tun.addAllowedApplication(packageName)
        }

        /** Build tun device */
        tunDevice = tun.establish()

        /** Check tun device */
        if (tunDevice == null) {
            Log.e("TProxyService", "tun#establish failed")
            return
        }

        /** Create, Update tun2socks config */
        val tun2socksConfig = arrayListOf(
            "tunnel:",
            "  name: $tunName",
            "  mtu: ${Settings.tunMtu}",
            "socks5:",
            "  address: ${Settings.socksAddress}",
            "  port: ${Settings.socksPort}",
        )
        if (
            Settings.socksUsername.trim().isNotEmpty() &&
            Settings.socksPassword.trim().isNotEmpty()
        ) {
            tun2socksConfig.add("  username: ${Settings.socksUsername}")
            tun2socksConfig.add("  password: ${Settings.socksPassword}")
        }
        tun2socksConfig.add(if (Settings.socksUdp) "  udp: udp" else "  udp: tcp")
        tun2socksConfig.add("")
        FileHelper.createOrUpdate(
            Settings.tun2socksConfig(applicationContext),
            tun2socksConfig.joinToString("\n")
        )

        /** Start tun2socks */
        TProxyStartService(Settings.tun2socksConfig(applicationContext).absolutePath, tunDevice!!.fd)

        /** Service Notification */
        val name = configName ?: Settings.tunName
        startForeground(VPN_SERVICE_NOTIFICATION_ID, createNotification(name))

        /** Broadcast start event */
        showToast("Start VPN")
        isRunning = true
        Intent(START_VPN_SERVICE_ACTION_NAME).also {
            it.`package` = BuildConfig.APPLICATION_ID
            it.putExtra("profile", name)
            sendBroadcast(it)
        }
    }

    private fun stopVPN() {
        TProxyStopService()
        stopXray()
        runCatching { tunDevice?.close() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        showToast("Stop VPN")
        tunDevice = null
        isRunning = false
        Intent(STOP_VPN_SERVICE_ACTION_NAME).also {
            it.`package` = BuildConfig.APPLICATION_ID
            sendBroadcast(it)
        }
        stopSelf()
    }

    private fun statusVPN() {
        Intent(STATUS_VPN_SERVICE_ACTION_NAME).also {
            it.`package` = BuildConfig.APPLICATION_ID
            it.putExtra("isRunning", isRunning)
            sendBroadcast(it)
        }
    }

    private fun createNotification(name: String): Notification {
        val pendingActivity = PendingIntent.getActivity(
            applicationContext,
            OPEN_MAIN_ACTIVITY_ACTION_ID,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val pendingStop = PendingIntent.getService(
            applicationContext,
            STOP_VPN_SERVICE_ACTION_ID,
            Intent(applicationContext, TProxyService::class.java).also {
                it.action = STOP_VPN_SERVICE_ACTION_NAME
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat
            .Builder(applicationContext, createNotificationChannel())
            .setSmallIcon(R.drawable.baseline_vpn_lock)
            .setContentTitle(name)
            .setContentIntent(pendingActivity)
            .addAction(0, getString(R.string.vpnStop), pendingStop)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel(): String {
        val id = "XrayVpnServiceNotification"
        val name = "Xray VPN Service"
        val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
        return id
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

}
