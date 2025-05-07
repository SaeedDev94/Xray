package io.github.saeeddev94.xray.helper

import io.github.saeeddev94.xray.BuildConfig
import io.github.saeeddev94.xray.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Authenticator
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.URL

class HttpHelper(var scope: CoroutineScope) {

    companion object {
        private fun getConnection(
            link: String,
            method: String = "GET",
            proxy: Proxy? = null,
            timeout: Int = 5000,
            userAgent: String? = null,
        ): HttpURLConnection {
            val url = URL(link)
            val connection = if (proxy == null) {
                url.openConnection() as HttpURLConnection
            } else {
                url.openConnection(proxy) as HttpURLConnection
            }
            connection.requestMethod = method
            connection.connectTimeout = timeout
            connection.readTimeout = timeout
            userAgent?.let { connection.setRequestProperty("User-Agent", it) }
            connection.setRequestProperty("Connection", "close")
            return connection
        }

        suspend fun get(link: String, userAgent: String? = null): String {
            return withContext(Dispatchers.IO) {
                val defaultUserAgent = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}"
                val connection = getConnection(link, userAgent = userAgent ?: defaultUserAgent)
                var responseCode = 0
                val responseBody = try {
                    connection.connect()
                    responseCode = connection.responseCode
                    connection.inputStream.bufferedReader().use { it.readText() }
                } catch (error: Exception) {
                    null
                } finally {
                    connection.disconnect()
                }
                if (responseCode != HttpURLConnection.HTTP_OK || responseBody == null) {
                    throw Exception("HTTP Error: $responseCode")
                }
                responseBody
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
            val link = Settings.pingAddress
            val method = "HEAD"
            val address = InetSocketAddress(Settings.socksAddress, Settings.socksPort.toInt())
            val proxy = Proxy(Proxy.Type.SOCKS, address)
            val timeout = Settings.pingTimeout * 1000

            getConnection(link, method, proxy, timeout)
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
