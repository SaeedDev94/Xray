package io.github.saeeddev94.xray

import android.content.Intent
import android.net.VpnService
import android.os.Binder
import android.os.IBinder
import android.os.ParcelFileDescriptor
import libXray.LibXray

class TProxyService : VpnService() {

    companion object {
        init {
            System.loadLibrary("hev-socks5-tunnel")
        }
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

    fun getIsRunning(): Boolean = isRunning
    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        stopVPN()
        super.onDestroy()
    }

    fun isConfigExists(): Boolean {
        val config = Settings.xrayConfig(applicationContext)
        return config.exists() && config.isFile
    }

    fun startVPN(): String {
        isRunning = true

        /** Start xray */
        if (Settings.useXray) {
            val datDir: String = applicationContext.filesDir.absolutePath
            val configPath: String = Settings.xrayConfig(applicationContext).absolutePath
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

        /** Pass all traffic to the tun (Except private IP addresses) */
        resources.getStringArray(R.array.publicIpAddresses).forEach {
            val address = it.split('/')
            tun.addRoute(address[0], address[1].toInt())
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
        if (Settings.socksUsername.trim().isNotEmpty()) {
            tun2socksConfig.add("  username: ${Settings.socksUsername}")
        }
        if (Settings.socksPassword.trim().isNotEmpty()) {
            tun2socksConfig.add("  password: ${Settings.socksPassword}")
        }
        tun2socksConfig.add(if (Settings.socksUdp) "  udp: udp" else "  udp: tcp")
        tun2socksConfig.add("")
        Settings.tun2socksConfig(applicationContext).writeText(tun2socksConfig.joinToString("\n"))

        /** Start tun2socks */
        TProxyStartService(Settings.tun2socksConfig(applicationContext).absolutePath, tunDevice!!.fd)

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
        stopSelf()
    }

}
