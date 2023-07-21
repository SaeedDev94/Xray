package com.xtls.xray

import android.content.Intent
import android.net.VpnService
import android.os.Binder
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.system.Os
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors.newFixedThreadPool

class XrayVpnService : VpnService() {

    inner class ServiceBinder : Binder() {
        fun getService(): XrayVpnService = this@XrayVpnService
    }

    private val binder: ServiceBinder = ServiceBinder()
    private var isRunning: Boolean = false
    private var xrayExecutor: ExecutorService? = null
    private var tun2socksExecutor: ExecutorService? = null
    private var tunDevice: ParcelFileDescriptor? = null

    override fun onDestroy() {
        stopVPN(stopSelf = false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun xrayPath(): String = "${applicationContext.applicationInfo.nativeLibraryDir}/libxray.so"

    fun getIsRunning(): Boolean = isRunning

    fun isConfigExists(): Boolean {
        val config = Settings.configFile(applicationContext)
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
            xrayExecutor = newFixedThreadPool(1)
            xrayExecutor!!.submit {
                Os.setenv("xray.location.asset", applicationContext.filesDir.absolutePath, true)
                val xrayCommand = arrayListOf(
                    xrayPath(), "run", "-c", Settings.configFile(applicationContext).absolutePath
                )
                val process = ProcessBuilder(xrayCommand)
                    .directory(applicationContext.filesDir)
                    .redirectErrorStream(true)
                    .start()
                process.waitFor()
            }
        }

        /** Create Tun */
        val tun = Builder()

        /** Basic tun config */
        tun.setMetered(false)
        tun.setMtu(1500)
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

        /** Start tun2socks */
        tun2socksExecutor = newFixedThreadPool(1)
        tun2socksExecutor!!.submit {
            val tun2socks = engine.Key()
            tun2socks.mark = 0L
            tun2socks.mtu = 0L
            tun2socks.`interface` = ""
            tun2socks.logLevel = "info"
            tun2socks.restAPI = ""
            tun2socks.tcpSendBufferSize = ""
            tun2socks.tcpReceiveBufferSize = ""
            tun2socks.tcpModerateReceiveBuffer = true
            tun2socks.device = "fd://${tunDevice!!.fd}"
            tun2socks.proxy = "socks5://${Settings.socksAddress}:${Settings.socksPort}"
            engine.Engine.insert(tun2socks)
            engine.Engine.start()
        }
    }

    fun stopVPN(stopSelf: Boolean = true) {
        isRunning = false
        if (xrayExecutor != null) {
            xrayExecutor!!.shutdown()
            xrayExecutor!!.shutdownNow()
            xrayExecutor = null
        }
        if (tun2socksExecutor != null) {
            tun2socksExecutor!!.shutdown()
            tun2socksExecutor!!.shutdownNow()
            tun2socksExecutor = null
        }
        if (tunDevice != null) {
            tunDevice!!.close()
            tunDevice = null
        }
        if (stopSelf) stopSelf()
    }

}
