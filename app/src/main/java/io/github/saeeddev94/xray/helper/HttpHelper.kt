package io.github.saeeddev94.xray.helper

import io.github.saeeddev94.xray.Settings
import java.lang.Exception
import java.net.Authenticator
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.URL

class HttpHelper {

    fun measureDelay(): String {
        val start = System.currentTimeMillis()
        val connection = getConnection()
        var result = "HTTP {status}, {delay} ms"

        result = try {
            setSocksAuth(getSocksAuth())
            val responseCode = connection.responseCode
            result.replace("{status}", "$responseCode")
        } catch (error: Exception) {
            error.message ?: "Http delay measure failed"
        } finally {
            connection.disconnect()
            setSocksAuth(null)
        }

        val delay = System.currentTimeMillis() - start
        return result.replace("{delay}", "$delay")
    }

    private fun getConnection(): HttpURLConnection {
        val address = InetSocketAddress(Settings.socksAddress, Settings.socksPort.toInt())
        val proxy = Proxy(Proxy.Type.SOCKS, address)
        val timeout = Settings.pingTimeout * 1000
        val connection = URL(Settings.pingAddress).openConnection(proxy) as HttpURLConnection

        connection.requestMethod = "HEAD"
        connection.connectTimeout = timeout
        connection.readTimeout = timeout
        connection.setRequestProperty("Connection", "close")

        return connection
    }

    private fun getSocksAuth(): Authenticator? {
        if (Settings.socksUsername.trim().isEmpty() || Settings.socksPassword.trim().isEmpty()) return null
        return object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(Settings.socksUsername, Settings.socksPassword.toCharArray())
            }
        }
    }

    private fun setSocksAuth(auth: Authenticator?) {
        Authenticator.setDefault(auth)
    }

}
