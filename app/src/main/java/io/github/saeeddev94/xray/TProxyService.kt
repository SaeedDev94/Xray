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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import io.github.saeeddev94.xray.database.Profile
import io.github.saeeddev94.xray.database.XrayDatabase
import io.github.saeeddev94.xray.helper.FileHelper
import libXray.LibXray

class TProxyService : VpnService() {

    companion object {
        init {
            System.loadLibrary("hev-socks5-tunnel")
        }

        const val VPN_SERVICE_NOTIFICATION_ID = 1
        const val OPEN_MAIN_ACTIVITY_ACTION_ID = 1
        const val STOP_VPN_SERVICE_ACTION_ID = 2
        const val START_VPN_SERVICE_ACTION_NAME = "XrayVpnServiceStartAction"
        const val STOP_VPN_SERVICE_ACTION_NAME = "XrayVpnServiceStopAction"
    }

    private val handler = Handler(Looper.getMainLooper())

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
            when (intent?.action) {
                STOP_VPN_SERVICE_ACTION_NAME -> stopVPN()
            }
        }
    }

    fun getIsRunning(): Boolean = isRunning
    override fun onBind(intent: Intent?): IBinder = binder

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()
        Settings.sync(applicationContext)
        IntentFilter().also {
            it.addAction(STOP_VPN_SERVICE_ACTION_NAME)
            registerReceiver(stopVpnAction, it, RECEIVER_NOT_EXPORTED)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        findProfileAndStart()
        return START_STICKY
    }

    override fun onRevoke() {
        stopVPN()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(stopVpnAction)
    }

    private fun findProfileAndStart() {
        val selectedProfile = Settings.selectedProfile
        if (selectedProfile == 0L) {
            startVPN(null)
        } else {
            Thread {
                val profile = XrayDatabase.ref(applicationContext).profileDao().find(selectedProfile)
                startVPN(profile)
            }.start()
        }
    }

    private fun startVPN(profile: Profile?) {
        isRunning = true

        /** Start xray */
        if (profile != null) {
            FileHelper().createOrUpdate(Settings.xrayConfig(applicationContext), profile.config)
            val datDir: String = applicationContext.filesDir.absolutePath
            val configPath: String = Settings.xrayConfig(applicationContext).absolutePath
            val maxMemory: Long = 67108864 // 64 MB * 1024 KB * 1024 B
            val error: String = LibXray.runXray(datDir, configPath, maxMemory)
            xrayProcess = error.isEmpty()
            if (!xrayProcess) {
                isRunning = false
                showToast(error)
                return
            }
        }

        /** Create Tun */
        val tun = Builder()

        /** Basic tun config */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tun.setMetered(false)
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

        /** IPv6 */
        if (Settings.enableIpV6) {
            tun.addAddress(Settings.tunAddressV6, Settings.tunPrefixV6)
            tun.addDnsServer(Settings.primaryDnsV6)
            tun.addDnsServer(Settings.secondaryDnsV6)
            tun.addRoute("::", 0)
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
        FileHelper().createOrUpdate(Settings.tun2socksConfig(applicationContext), tun2socksConfig.joinToString("\n"))

        /** Start tun2socks */
        TProxyStartService(Settings.tun2socksConfig(applicationContext).absolutePath, tunDevice!!.fd)

        /** Service Notification */
        val name = profile?.name ?: Settings.tunName
        startForeground(VPN_SERVICE_NOTIFICATION_ID, createNotification(name))

        /** Broadcast start event */
        Intent(START_VPN_SERVICE_ACTION_NAME).also {
            it.`package` = BuildConfig.APPLICATION_ID
            it.putExtra("profile", name)
            sendBroadcast(it)
        }

        showToast("Start VPN")
    }

    private fun stopVPN() {
        isRunning = false
        if (xrayProcess) {
            LibXray.stopXray()
            xrayProcess = false
        }
        if (tunDevice != null) {
            TProxyStopService()
            tunDevice!!.close()
            tunDevice = null
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        showToast("Stop VPN")
        stopSelf()
    }

    private fun createNotification(name: String): Notification {
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
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        return id
    }

    private fun showToast(message: String) {
        handler.post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

}
