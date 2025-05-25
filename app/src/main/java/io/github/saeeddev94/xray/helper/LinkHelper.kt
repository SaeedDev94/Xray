package io.github.saeeddev94.xray.helper

import XrayCore.XrayCore
import android.util.Base64
import io.github.saeeddev94.xray.Settings
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URI

class LinkHelper(
    private val settings: Settings,
    link: String
) {

    private val success: Boolean
    private val outbound: JSONObject?
    private var remark: String = REMARK_DEFAULT

    init {
        val base64: String = XrayCore.json(link)
        val decoded = tryDecodeBase64(base64)
        val response = try {
            JSONObject(decoded)
        } catch (error: JSONException) {
            JSONObject()
        }
        val data = response.optJSONObject("data") ?: JSONObject()
        val outbounds = data.optJSONArray("outbounds") ?: JSONArray()
        success = response.optBoolean("success", false)
        outbound = if (outbounds.length() > 0) outbounds[0] as JSONObject else null
    }

    companion object {
        const val REMARK_DEFAULT = "New Profile"
        const val LINK_DEFAULT = "New Link"

        fun remark(uri: URI, default: String = ""): String {
            val name = uri.fragment ?: ""
            return name.ifEmpty { default }
        }

        fun tryDecodeBase64(value: String): String {
            return runCatching {
                val byteArray = Base64.decode(value, Base64.DEFAULT)
                String(byteArray)
            }.getOrNull() ?: value
        }
    }

    fun isValid(): Boolean {
        return success && outbound != null
    }

    fun json(): String {
        return config().toString(2) + "\n"
    }

    fun remark(): String {
        return remark
    }

    private fun log(): JSONObject {
        val log = JSONObject()
        log.put("loglevel", "warning")
        return log
    }

    private fun dns(): JSONObject {
        val dns = JSONObject()
        val servers = JSONArray()
        servers.put(settings.primaryDns)
        servers.put(settings.secondaryDns)
        dns.put("servers", servers)
        return dns
    }

    private fun inbounds(): JSONArray {
        val inbounds = JSONArray()

        val socks = JSONObject()
        socks.put("listen", settings.socksAddress)
        socks.put("port", settings.socksPort.toInt())
        socks.put("protocol", "socks")

        val socksSettings = JSONObject()
        socksSettings.put("udp", true)
        if (
            settings.socksUsername.trim().isNotEmpty() &&
            settings.socksPassword.trim().isNotEmpty()
        ) {
            val account = JSONObject()
            account.put("user", settings.socksUsername)
            account.put("pass", settings.socksPassword)
            val accounts = JSONArray()
            accounts.put(account)

            socksSettings.put("auth", "password")
            socksSettings.put("accounts", accounts)
        }

        val sniffing = JSONObject()
        sniffing.put("enabled", true)
        val sniffingDestOverride = JSONArray()
        sniffingDestOverride.put("http")
        sniffingDestOverride.put("tls")
        sniffingDestOverride.put("quic")
        sniffing.put("destOverride", sniffingDestOverride)

        socks.put("settings", socksSettings)
        socks.put("sniffing", sniffing)
        socks.put("tag", "socks")

        inbounds.put(socks)

        return inbounds
    }

    private fun outbounds(): JSONArray {
        val outbounds = JSONArray()

        val proxy = JSONObject(outbound!!.toString())
        if (proxy.has("sendThrough")) {
            remark = proxy.optString("sendThrough", REMARK_DEFAULT)
            proxy.remove("sendThrough")
        }
        proxy.put("tag", "proxy")

        val direct = JSONObject()
        direct.put("protocol", "freedom")
        direct.put("tag", "direct")

        val block = JSONObject()
        block.put("protocol", "blackhole")
        block.put("tag", "block")

        outbounds.put(proxy)
        outbounds.put(direct)
        outbounds.put(block)

        return outbounds
    }

    private fun routing(): JSONObject {
        val routing = JSONObject()
        routing.put("domainStrategy", "IPIfNonMatch")

        val rules = JSONArray()

        val proxyDns = JSONObject()
        val proxyDnsIp = JSONArray()
        proxyDnsIp.put(settings.primaryDns)
        proxyDnsIp.put(settings.secondaryDns)
        proxyDns.put("ip", proxyDnsIp)
        proxyDns.put("port", 53)
        proxyDns.put("outboundTag", "proxy")

        val directPrivate = JSONObject()
        val directPrivateIp = JSONArray()
        directPrivateIp.put("geoip:private")
        directPrivate.put("ip", directPrivateIp)
        directPrivate.put("outboundTag", "direct")

        rules.put(proxyDns)
        rules.put(directPrivate)

        routing.put("rules", rules)

        return routing
    }

    private fun config(): JSONObject {
        val config = JSONObject()
        config.put("log", log())
        config.put("dns", dns())
        config.put("inbounds", inbounds())
        config.put("outbounds", outbounds())
        config.put("routing", routing())
        return config
    }

}
