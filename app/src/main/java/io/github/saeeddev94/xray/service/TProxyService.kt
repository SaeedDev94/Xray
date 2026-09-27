package io.github.saeeddev94.xray.service

import XrayCore.XrayCore
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.topjohnwu.superuser.Shell
import io.github.saeeddev94.xray.BuildConfig
import io.github.saeeddev94.xray.R
import io.github.saeeddev94.xray.Settings
import io.github.saeeddev94.xray.Xray
import io.github.saeeddev94.xray.activity.MainActivity
import io.github.saeeddev94.xray.database.Config
import io.github.saeeddev94.xray.database.Profile
import io.github.saeeddev94.xray.dto.XrayConfig
import io.github.saeeddev94.xray.helper.ConfigHelper
import io.github.saeeddev94.xray.helper.FileHelper
import io.github.saeeddev94.xray.helper.TransparentProxyHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import kotlin.reflect.cast

@SuppressLint("VpnServicePolicy")
class TProxyService : VpnService() {

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val binder by lazy { ServiceBinder() }
    private val scope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    private val settings by lazy { Settings(applicationContext) }
    private val transparentProxyHelper by lazy { TransparentProxyHelper(this, settings) }

    private val app by lazy { Xray::class.cast(application) }
    private val configRepository by lazy { app.configRepository }
    private val profileRepository by lazy { app.profileRepository }

    private var isRunning: Boolean = false
    private var tunDevice: ParcelFileDescriptor? = null
    private var cellularCallback: ConnectivityManager.NetworkCallback? = null
    private var stateListener: ((Boolean) -> Unit)? = null
    private var toast: Toast? = null
    private var script: String? = null

    private val connectivityManager by lazy {
        getSystemService(ConnectivityManager::class.java)
    }
    private val notificationManager by lazy {
        getSystemService(NotificationManager::class.java)
    }

    private external fun TProxyStartService(configPath: String, fd: Int): Boolean
    private external fun TProxyStopService(): Boolean
    private external fun TProxyIsRunning(): Boolean
    private external fun TProxyGetStats(): LongArray

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            when (intent?.action) {
                START_VPN_SERVICE_ACTION_NAME -> start(getProfile(), globalConfigs())
                NEW_CONFIG_SERVICE_ACTION_NAME -> newConfig(getProfile(), globalConfigs())
                STOP_VPN_SERVICE_ACTION_NAME -> stopVPN()
                NETWORK_UPDATE_SERVICE_ACTION_NAME -> transparentProxyHelper.networkUpdate()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action == SERVICE_INTERFACE) return super.onBind(intent)
        return binder
    }

    override fun onRevoke() {
        stopVPN()
    }

    override fun onDestroy() {
        scope.cancel()
        unregisterCellularCallback()
        stateListener = null
        toast = null
        super.onDestroy()
    }

    private fun configName(profile: Profile?): String = profile?.name ?: settings.tunName

    private fun getIsRunning(): Boolean {
        return if (settings.transparentProxy) {
            transparentProxyHelper.isRunning()
        } else {
            isRunning
        }
    }

    private suspend fun getProfile(): Profile? {
        return if (settings.selectedProfile == 0L) {
            null
        } else {
            profileRepository.find(settings.selectedProfile)
        }
    }

    private suspend fun globalConfigs(): Config {
        return configRepository.get()
    }

    private fun getConfig(profile: Profile, globalConfigs: Config): XrayConfig? {
        val dir: File = applicationContext.filesDir
        val config: File = settings.xrayConfig()
        val configResult = runCatching {
            ConfigHelper(settings, globalConfigs, profile.config)
        }
        val configHelper = configResult.getOrNull()
        val script: String? = configHelper?.script()
        val error: String = if (configResult.isSuccess) {
            FileHelper.createOrUpdate(config, "$configHelper")
            XrayCore.test(dir.absolutePath, config.absolutePath)
        } else {
            configResult.exceptionOrNull()?.message ?: getString(R.string.invalidProfile)
        }
        if (error.isNotEmpty()) {
            showToast(error)
            return null
        }
        return XrayConfig(
            dir.absolutePath,
            config.absolutePath,
            script,
        )
    }

    private fun start(profile: Profile?, globalConfigs: Config) {
        if (profile == null) return
        getConfig(profile, globalConfigs)?.let {
            startXray(it)
            startVPN(profile)
        }
    }

    private fun newConfig(profile: Profile?, globalConfigs: Config) {
        if (!getIsRunning() || profile == null) return
        stopXray()
        getConfig(profile, globalConfigs).also {
            if (it == null) stopVPN() else startXray(it)
        }?.let {
            val name = configName(profile)
            val notification = createNotification(name)
            showToast(name)
            VpnTileService.update(applicationContext, true, name)
            notificationManager.notify(VPN_SERVICE_NOTIFICATION_ID, notification)
        }
    }

    private fun startXray(config: XrayConfig) {
        when (settings.transparentProxy) {
            true -> {
                config.script?.let {
                    script = it
                    Shell.cmd("nohup $it start > /dev/null 2>&1 &").exec()
                }
                transparentProxyHelper.startService()
            }
            false -> XrayCore.start(config.dir, config.file)
        }
    }

    private fun stopXray() {
        when (settings.transparentProxy) {
            true -> {
                script?.let {
                    script = null
                    Shell.cmd("$it stop").exec()
                }
                transparentProxyHelper.stopService()
            }
            false -> XrayCore.stop()
        }
    }

    private fun startVPN(profile: Profile?) {
        if (settings.transparentProxy) {
            transparentProxyHelper.enableProxy()
            transparentProxyHelper.monitorNetwork()
        } else if (settings.tun2socks) {
            /** Create Tun */
            val tun = Builder()
            val tunName = getString(R.string.appName)

            /** Basic tun config */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tun.setMetered(false)
            tun.setMtu(settings.tunMtu)
            tun.setSession(tunName)

            /** IPv4 */
            tun.addAddress(settings.tunAddress, settings.tunPrefix)
            tun.addDnsServer(settings.primaryDns)
            tun.addDnsServer(settings.secondaryDns)

            /** IPv6 */
            if (settings.enableIpV6) {
                tun.addAddress(settings.tunAddressV6, settings.tunPrefixV6)
                tun.addDnsServer(settings.primaryDnsV6)
                tun.addDnsServer(settings.secondaryDnsV6)
                tun.addRoute("::", 0)
            }

            /** Bypass LAN (IPv4) */
            if (settings.bypassLan) {
                settings.tunRoutes.forEach {
                    val address = it.split('/')
                    tun.addRoute(address[0], address[1].toInt())
                }
            } else {
                tun.addRoute("0.0.0.0", 0)
            }

            /** Apps Routing */
            if (settings.appsRoutingMode) tun.addDisallowedApplication(applicationContext.packageName)
            settings.appsRouting.split("\n").forEach {
                val packageName = it.trim()
                if (packageName.isBlank()) return@forEach
                if (settings.appsRoutingMode) tun.addDisallowedApplication(packageName)
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
                "  mtu: ${settings.tunMtu}",
                "misc:",
                "  tcp-read-write-timeout: ${settings.tunTcpReadWriteTimeout}",
                "  udp-read-write-timeout: ${settings.tunUdpReadWriteTimeout}",
                "socks5:",
                "  address: ${settings.socksAddress}",
                "  port: ${settings.socksPort}",
            )
            if (
                settings.socksUsername.trim().isNotEmpty() &&
                settings.socksPassword.trim().isNotEmpty()
            ) {
                tun2socksConfig.add("  username: ${settings.socksUsername}")
                tun2socksConfig.add("  password: ${settings.socksPassword}")
            }
            tun2socksConfig.add(if (settings.socksUdp) "  udp: udp" else "  udp: tcp")
            tun2socksConfig.add("")
            FileHelper.createOrUpdate(
                settings.tun2socksConfig(),
                tun2socksConfig.joinToString("\n")
            )

            /** Start tun2socks */
            TProxyStartService(settings.tun2socksConfig().absolutePath, tunDevice!!.fd)
        }

        /** Service Notification */
        val name = configName(profile)
        startForeground(VPN_SERVICE_NOTIFICATION_ID, createNotification(name))

        /** Listen for cellular changes */
        if (cellularCallback == null) {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()
            cellularCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    this@TProxyService.transparentProxyHelper.networkUpdate()
                }
            }
            connectivityManager.registerNetworkCallback(request, cellularCallback!!)
        }

        /** Notify start event */
        showToast("Start VPN")
        isRunning = true
        notifyState(true, name)
    }

    private fun stopVPN() {
        if (settings.transparentProxy) {
            transparentProxyHelper.disableProxy()
        } else {
            TProxyStopService()
            runCatching { tunDevice?.close() }
            tunDevice = null
            isRunning = false
        }
        stopXray()
        unregisterCellularCallback()
        stopForeground(STOP_FOREGROUND_REMOVE)
        showToast("Stop VPN")
        notifyState(false, getString(R.string.appName))
        stopSelf()
    }

    private fun unregisterCellularCallback() {
        cellularCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
        cellularCallback = null
    }

    private fun notifyState(isRunning: Boolean, label: String) {
        VpnTileService.update(applicationContext, isRunning, label)
        mainHandler.post { stateListener?.invoke(isRunning) }
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
        mainHandler.post {
            toast?.cancel()
            toast = Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).also {
                it.show()
            }
        }
    }

    inner class ServiceBinder : Binder() {
        fun isRunning(): Boolean = getIsRunning()

        fun setStateListener(listener: ((Boolean) -> Unit)?) {
            stateListener = listener
        }
    }

    companion object {
        init {
            System.loadLibrary("hev-socks5-tunnel")
        }

        const val PKG_NAME = BuildConfig.APPLICATION_ID
        const val STOP_VPN_SERVICE_ACTION_NAME = "$PKG_NAME.VpnStop"
        const val START_VPN_SERVICE_ACTION_NAME = "$PKG_NAME.VpnStart"
        const val NEW_CONFIG_SERVICE_ACTION_NAME = "$PKG_NAME.NewConfig"
        const val NETWORK_UPDATE_SERVICE_ACTION_NAME = "$PKG_NAME.NetworkUpdate"
        private const val VPN_SERVICE_NOTIFICATION_ID = 1
        private const val OPEN_MAIN_ACTIVITY_ACTION_ID = 2
        private const val STOP_VPN_SERVICE_ACTION_ID = 3

        fun stop(context: Context) = startCommand(context, STOP_VPN_SERVICE_ACTION_NAME)
        fun newConfig(context: Context) = startCommand(context, NEW_CONFIG_SERVICE_ACTION_NAME)

        fun start(context: Context, check: Boolean) {
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
}
