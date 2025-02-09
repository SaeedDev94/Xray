package io.github.saeeddev94.xray.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.VpnService
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.core.app.NotificationCompat
import io.github.saeeddev94.xray.BuildConfig
import io.github.saeeddev94.xray.R
import io.github.saeeddev94.xray.Settings
import io.github.saeeddev94.xray.activity.MainActivity
import io.github.saeeddev94.xray.database.Profile
import io.github.saeeddev94.xray.helper.FileHelper
import XrayCore.XrayCore
import android.annotation.SuppressLint
import android.util.Log
import io.github.saeeddev94.xray.Xray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.reflect.cast

class TProxyService : VpnService() {

    companion object {
        init {
            System.loadLibrary("hev-socks5-tunnel")
        }

        const val VPN_SERVICE_NOTIFICATION_ID = 1
        const val OPEN_MAIN_ACTIVITY_ACTION_ID = 1
        const val STOP_VPN_SERVICE_ACTION_ID = 2
        const val START_VPN_SERVICE_ACTION_NAME = "${BuildConfig.APPLICATION_ID}.VpnStart"
        const val STOP_VPN_SERVICE_ACTION_NAME = "${BuildConfig.APPLICATION_ID}.VpnStop"
    }

    private val profileRepository by lazy { Xray::class.cast(application).profileRepository }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private external fun TProxyStartService(configPath: String, fd: Int)
    private external fun TProxyStopService()
    private external fun TProxyGetStats(): LongArray

    inner class ServiceBinder : Binder() {
        fun getService(): TProxyService = this@TProxyService
    }

    private val binder: ServiceBinder = ServiceBinder()
    private var isRunning: Boolean = false
    private var tunDevice: ParcelFileDescriptor? = null
    private val stopVpnAction: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                STOP_VPN_SERVICE_ACTION_NAME -> stopVPN()
            }
        }
    }
    private val connectivity by lazy { getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    private val defaultNetworkCallback by lazy {
        object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                super.onLost(network)
                Intent(STOP_VPN_SERVICE_ACTION_NAME).also {
                    it.`package` = BuildConfig.APPLICATION_ID
                    sendBroadcast(it)
                }
            }
        }
    }

    fun getIsRunning(): Boolean = isRunning
    override fun onBind(intent: Intent?): IBinder = binder

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        Settings.sync(applicationContext)
        IntentFilter().also {
            it.addAction(STOP_VPN_SERVICE_ACTION_NAME)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(stopVpnAction, it, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(stopVpnAction, it)
            }
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
        scope.cancel()
        unregisterReceiver(stopVpnAction)
    }

    private fun findProfileAndStart() {
        scope.launch {
            val profile = if (Settings.selectedProfile == 0L) {
                null
            } else {
                profileRepository.find(Settings.selectedProfile)
            }
            startVPN(profile)
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
            val error: String = XrayCore.start(datDir, configPath, maxMemory)
            if (error.isNotEmpty()) {
                isRunning = false
                showToast(error)
                return
            }
        }

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

        /** Exclude apps */
        tun.addDisallowedApplication(applicationContext.packageName)
        Settings.excludedApps.split("\n").forEach { packageName ->
            if (packageName.trim().isNotEmpty()) tun.addDisallowedApplication(packageName)
        }

        /** Build tun device */
        tunDevice = tun.establish()

        /** Check tun device */
        if (tunDevice == null) {
            isRunning = false
            Log.e("TProxyService", "tun#establish failed")
            return
        }

        /** Register network callback */
        try {
            connectivity.registerDefaultNetworkCallback(defaultNetworkCallback)
        } catch (error: Exception) {
            error.printStackTrace()
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
        try {
            connectivity.unregisterNetworkCallback(defaultNetworkCallback)
        } catch (_: Exception) {
        }
        TProxyStopService()
        XrayCore.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        showToast("Stop VPN")
        stopSelf()
        try {
            tunDevice?.close()
        } catch (_: Exception) {
        } finally {
            tunDevice = null
        }
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
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

}
