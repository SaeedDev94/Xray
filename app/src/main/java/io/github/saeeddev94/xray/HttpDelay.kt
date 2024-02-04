package io.github.saeeddev94.xray

import android.util.Base64.NO_WRAP
import android.util.Base64.encodeToString
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL

class HttpDelay {

    fun measure(): String {
        val start = System.currentTimeMillis()
        val connection = getConnection()

        try {
            BufferedReader(InputStreamReader(connection.inputStream)).use { it.readLine() }
        } catch (error: Exception) {
            return error.message ?: "Http delay measure failed"
        } finally {
            connection.disconnect()
        }

        val delay = System.currentTimeMillis() - start

        return "$delay ms"
    }

    private fun getConnection(): HttpURLConnection {
        val address = InetSocketAddress(Settings.socksAddress, Settings.socksPort.toInt())
        val proxy = Proxy(Proxy.Type.SOCKS, address)
        val username = Settings.socksUsername
        val password = Settings.socksPassword
        val timeout = Settings.pingTimeout.toInt() * 1000
        val connection = URL(Settings.pingAddress).openConnection(proxy) as HttpURLConnection

        connection.requestMethod = "HEAD"
        connection.connectTimeout = timeout
        connection.readTimeout = timeout

        if (username.trim().isNotEmpty() && password.trim().isNotEmpty()) {
            val credentials = "$username:$password"
            val auth = encodeToString(credentials.toByteArray(), NO_WRAP)
            connection.setRequestProperty("Proxy-Authorization", auth)
        }

        return connection
    }

}
