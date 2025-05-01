package io.github.saeeddev94.xray.helper

import io.github.saeeddev94.xray.BuildConfig
import io.github.saeeddev94.xray.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.net.Authenticator
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.URL

class HttpHelper(var scope: CoroutineScope) {

    companion object {
        suspend fun get(link: String, customUserAgent: String? = null): String {
            return withContext(Dispatchers.IO) {
                val url = URL(link)
                val userAgent = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}"
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", customUserAgent ?: userAgent)
                connection.connect()
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    throw Exception("HTTP Error: $responseCode")
                }
            }
        }
    }

    fun measureDelay(callback: (result: String) -> Unit) {
        scope.launch(Dispatchers.IO) {
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
            withContext(Dispatchers.Main) {
                callback(result.replace("{delay}", "$delay"))
            }
        }
    }

    private suspend fun getConnection(): HttpURLConnection {
        return withContext(Dispatchers.IO) {
            val address = InetSocketAddress(Settings.socksAddress, Settings.socksPort.toInt())
            val proxy = Proxy(Proxy.Type.SOCKS, address)
            val timeout = Settings.pingTimeout * 1000
            val connection = URL(Settings.pingAddress).openConnection(proxy) as HttpURLConnection

            connection.requestMethod = "HEAD"
            connection.connectTimeout = timeout
            connection.readTimeout = timeout
            connection.setRequestProperty("Connection", "close")

            connection
        }
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
