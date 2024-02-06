package io.github.saeeddev94.xray

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import io.github.saeeddev94.xray.database.Profile
import libXray.LibXray

class TProxyService : VpnService() {

    companion object {
        init {
            System.loadLibrary("hev-socks5-tunnel")
        }

        const val VPN_SERVICE_NOTIFICATION_ID = 1
        const val OPEN_MAIN_ACTIVITY_ACTION_ID = 1
        const val STOP_VPN_SERVICE_ACTION_ID = 2
        const val STOP_VPN_SERVICE_ACTION_NAME = "XrayVpnServiceStopAction"
    }

    private external fun TProxyStartService(configPath: String, fd: Int)
    private external fun TProxyStopService()
    private external fun TProxyGetStats(): LongArray

    inner class ServiceBinder : Binder() {
        fun getService(): TProxyService = this@TProxyService
    }

    private val binder: ServiceBinder = ServiceBinder()
    private var isRunning: Boolean = false
    private var xrayProcess: Boolean = false
    private var tunDevice: ParcelFileDescriptor? = null
    private val stopVpnAction: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == STOP_VPN_SERVICE_ACTION_NAME) {
                stopVPN()
            }
        }
    }

    fun getIsRunning(): Boolean = isRunning
    override fun onBind(intent: Intent?): IBinder = binder

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        IntentFilter(STOP_VPN_SERVICE_ACTION_NAME).also {
            registerReceiver(stopVpnAction, it, RECEIVER_NOT_EXPORTED)
        }
        super.onCreate()
    }

    override fun onDestroy() {
        stopVPN()
        unregisterReceiver(stopVpnAction)
        super.onDestroy()
    }

    fun startVPN(profile: Profile?): String {
        isRunning = true

        /** Start xray */
        if (profile != null) {
            val configFile = Settings.xrayConfig(applicationContext)
            val configContent = if (configFile.exists()) configFile.bufferedReader().use { it.readText() } else ""
            if (profile.config != configContent) configFile.writeText(profile.config)
            val datDir: String = applicationContext.filesDir.absolutePath
            val configPath: String = configFile.absolutePath
            val maxMemory: Long = 67108864 // 64 MB * 1024 KB * 1024 B
            val error: String = LibXray.runXray(datDir, configPath, maxMemory)
            xrayProcess = error.isEmpty()
            if (!xrayProcess) {
                isRunning = false
                return error
            }
        }

        /** Create Tun */
        val tun = Builder()

        /** Basic tun config */
        tun.setMetered(false)
        tun.setMtu(Settings.tunMtu)
        tun.setSession(Settings.tunName)
        tun.addAddress(Settings.tunAddress, Settings.tunPrefix)
        tun.addDnsServer(Settings.primaryDns)
        tun.addDnsServer(Settings.secondaryDns)

        /** Routing config */
        if (Settings.bypassLan) {
            resources.getStringArray(R.array.publicIpAddresses).forEach {
                val address = it.split('/')
                tun.addRoute(address[0], address[1].toInt())
            }
        } else {
            tun.addRoute("0.0.0.0", 0)
        }

        /** Exclude apps */
        tun.addDisallowedApplication(applicationContext.packageName)
        Settings.excludedApps.split("\n").forEach { packageName ->
            if (packageName.trim().isNotEmpty()) tun.addDisallowedApplication(packageName)
        }

        /** Build tun device */
        tunDevice = tun.establish()

        /** Create, Update tun2socks config */
        val tun2socksConfig = arrayListOf(
            "tunnel:",
            "  name: ${Settings.tunName}",
            "  mtu: ${Settings.tunMtu}",
            "  ipv4:",
            "    gateway: ${Settings.tunGateway}",
            "    address: ${Settings.tunAddress}",
            "    prefix: ${Settings.tunPrefix}",
            "socks5:",
            "  address: ${Settings.socksAddress}",
            "  port: ${Settings.socksPort}",
        )
        if (Settings.socksUsername.trim().isNotEmpty() && Settings.socksPassword.trim().isNotEmpty()) {
            tun2socksConfig.add("  username: ${Settings.socksUsername}")
            tun2socksConfig.add("  password: ${Settings.socksPassword}")
        }
        tun2socksConfig.add(if (Settings.socksUdp) "  udp: udp" else "  udp: tcp")
        tun2socksConfig.add("")
        val tun2socksYaml = tun2socksConfig.joinToString("\n")
        val tun2socksFile = Settings.tun2socksConfig(applicationContext)
        val tun2socksContent = if (tun2socksFile.exists()) tun2socksFile.bufferedReader().use { it.readText() } else ""
        if (tun2socksYaml != tun2socksContent) tun2socksFile.writeText(tun2socksYaml)

        /** Start tun2socks */
        TProxyStartService(Settings.tun2socksConfig(applicationContext).absolutePath, tunDevice!!.fd)

        /** Service Notification */
        startForeground(VPN_SERVICE_NOTIFICATION_ID, createNotification(profile))

        return ""
    }

    fun stopVPN() {
        isRunning = false
        if (xrayProcess) {
            LibXray.stopXray()
        }
        if (tunDevice != null) {
            TProxyStopService()
            tunDevice!!.close()
            tunDevice = null
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(profile: Profile?): Notification {
        val pendingActivity = PendingIntent.getActivity(
            applicationContext,
            OPEN_MAIN_ACTIVITY_ACTION_ID,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val pendingStop = PendingIntent.getBroadcast(
            applicationContext,
            STOP_VPN_SERVICE_ACTION_ID,
            Intent(STOP_VPN_SERVICE_ACTION_NAME).also {
              it.`package` = BuildConfig.APPLICATION_ID
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat
            .Builder(applicationContext, createNotificationChannel())
            .setSmallIcon(R.drawable.baseline_vpn_lock)
            .setContentTitle(profile?.name ?: Settings.tunName)
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
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        return id
    }

}
