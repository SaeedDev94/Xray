package com.xtls.xray

import android.content.Intent
import android.net.VpnService
import android.os.Binder
import android.os.IBinder
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

class TProxyService : VpnService() {

    companion object {
        init {
            System.loadLibrary("hev-socks5-tunnel")
        }
    }

    private external fun TProxyStartService(configPath: String, fd: Int)
    private external fun TProxyStopService()

    inner class ServiceBinder : Binder() {
        fun getService(): TProxyService = this@TProxyService
    }

    private val binder: ServiceBinder = ServiceBinder()
    private var isRunning: Boolean = false
    private var tunDevice: ParcelFileDescriptor? = null
    private var socksProcess: Process? = null

    fun getIsRunning(): Boolean = isRunning
    fun xrayPath(): String = "${applicationContext.applicationInfo.nativeLibraryDir}/libxray.so"
    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        stopVPN()
        super.onDestroy()
    }

    fun isConfigExists(): Boolean {
        val config = Settings.xrayConfig(applicationContext)
        return config.exists() && config.isFile
    }

    fun installXray() {
        val filesPath = applicationContext.filesDir.absolutePath
        assets.list("")?.forEach { fileName ->
            var input: InputStream? = null
            var output: OutputStream? = null
            try {
                val file = File(filesPath, fileName)
                input = assets.open(fileName)
                output = FileOutputStream(file)
                /** Copy file */
                val buffer = ByteArray(1024)
                var read: Int?
                while (input.read(buffer).also { read = it } != -1) {
                    read?.let { output.write(buffer, 0, it) }
                }
                /** Set file permission 644 */
                val permission = HashSet<PosixFilePermission>()
                permission.add(PosixFilePermission.OWNER_READ)
                permission.add(PosixFilePermission.OWNER_WRITE)
                permission.add(PosixFilePermission.GROUP_READ)
                permission.add(PosixFilePermission.OTHERS_READ)
                Files.setPosixFilePermissions(file.toPath(), permission)
            } catch (_: IOException) {
                // Ignore
            } finally {
                if (input != null) {
                    try { input.close() } catch (_: IOException) {}
                }
                if (output != null) {
                    try { output.close() } catch (_: IOException) {}
                }
            }
        }
    }

    fun startVPN() {
        isRunning = true
        if (Settings.useXray) {
            /** Start xray */
            Thread {
                val xrayCommand = arrayListOf(
                    xrayPath(), "run", "-c", Settings.xrayConfig(applicationContext).absolutePath
                )
                val xrayProcess = ProcessBuilder(xrayCommand)
                val xrayEnv = xrayProcess.environment()
                xrayEnv["xray.location.asset"] = applicationContext.filesDir.absolutePath
                socksProcess = xrayProcess.start()
                socksProcess!!.waitFor()
            }.start()
        }

        /** Create Tun */
        val tun = Builder()

        /** Basic tun config */
        tun.setMetered(false)
        tun.setMtu(Settings.MTU)
        tun.setSession("tun0")
        tun.addAddress("10.10.10.10", 24)
        tun.addDnsServer(Settings.primaryDns)
        tun.addDnsServer(Settings.secondaryDns)

        /** Pass all traffic to the tun (Except private IP addresses) */
        resources.getStringArray(R.array.publicIpAddresses).forEach {
            val address = it.split('/')
            tun.addRoute(address[0], address[1].toInt())
        }

        /** Exclude the app itself */
        tun.addDisallowedApplication(applicationContext.packageName)

        /** Build tun device */
        tunDevice = tun.establish()

        /** Create, Update tun2socks config */
        val tun2socksConfig = arrayOf(
            "tunnel:",
            "  mtu: ${Settings.MTU}",
            "socks5:",
            "  address: ${Settings.socksAddress}",
            "  port: ${Settings.socksPort}",
            "  udp: 'udp'"
        )
        Settings.tun2socksConfig(applicationContext).writeText(tun2socksConfig.joinToString("\n"))

        /** Start tun2socks */
        TProxyStartService(Settings.tun2socksConfig(applicationContext).absolutePath, tunDevice!!.fd)
    }

    fun stopVPN() {
        isRunning = false
        if (socksProcess != null) {
            if (socksProcess!!.isAlive) socksProcess!!.destroy()
            socksProcess = null
        }
        if (tunDevice != null) {
            TProxyStopService()
            tunDevice!!.close()
            tunDevice = null
        }
        stopSelf()
    }

}
