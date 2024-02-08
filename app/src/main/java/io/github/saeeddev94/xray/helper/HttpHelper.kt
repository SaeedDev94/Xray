package io.github.saeeddev94.xray.helper

import android.util.Base64.NO_WRAP
import android.util.Base64.encodeToString
import io.github.saeeddev94.xray.Settings
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL

class HttpHelper {

    fun measureDelay(): String {
        val start = System.currentTimeMillis()
        val connection = getConnection()
        var result = "HTTP {status}, {delay} ms"

        result = try {
            val responseCode = connection.responseCode
            result.replace("{status}", "$responseCode")
        } catch (error: Exception) {
            error.message ?: "Http delay measure failed"
        } finally {
            connection.disconnect()
        }

        val delay = System.currentTimeMillis() - start
        return result.replace("{delay}", "$delay")
    }

    private fun getConnection(): HttpURLConnection {
        val address = InetSocketAddress(Settings.socksAddress, Settings.socksPort.toInt())
        val proxy = Proxy(Proxy.Type.SOCKS, address)
        val username = Settings.socksUsername
        val password = Settings.socksPassword
        val timeout = Settings.pingTimeout * 1000
        val connection = URL(Settings.pingAddress).openConnection(proxy) as HttpURLConnection

        connection.requestMethod = "HEAD"
        connection.connectTimeout = timeout
        connection.readTimeout = timeout
        connection.setRequestProperty("Connection", "close")

        if (username.trim().isNotEmpty() && password.trim().isNotEmpty()) {
            val credentials = "$username:$password"
            val auth = encodeToString(credentials.toByteArray(), NO_WRAP)
            connection.setRequestProperty("Proxy-Authorization", auth)
        }

        return connection
    }

}
