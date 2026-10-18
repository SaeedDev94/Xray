package io.github.saeeddev94.xray.helper

import XrayCore.XrayCore
import android.util.Base64
import io.github.saeeddev94.xray.Settings
import io.github.saeeddev94.xray.extensions.decodeToJsonObject
import io.github.saeeddev94.xray.extensions.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.URI

class LinkHelper(
    private val settings: Settings,
    link: String,
) {

    private val success: Boolean
    private val outbound: JsonObject?
    private var remark: String = REMARK_DEFAULT

    init {
        val base64: String = XrayCore.json(link)
        val response = runCatching {
            tryDecodeBase64(base64).decodeToJsonObject()
        }.getOrDefault(JsonObject(emptyMap()))

        success = response["success"]
            ?.jsonPrimitive
            ?.booleanOrNull
            ?: false

        outbound = response["data"]
            ?.jsonObject
            ?.get("outbounds")
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
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

    fun isValid(): Boolean = success && outbound != null

    fun json(): String = config().encodeToString() + "\n"

    fun remark(): String = remark

    private fun log(): JsonObject {
        return buildJsonObject {
            put("loglevel", "warning")
        }
    }

    private fun dns(): JsonObject {
        return buildJsonObject {
            put(
                "servers",
                buildJsonArray {
                    add(JsonPrimitive(settings.primaryDns))
                    add(JsonPrimitive(settings.secondaryDns))
                }
            )
        }
    }

    private fun inbounds(): JsonArray {
        val sniffing = buildJsonObject {
            put("enabled", true)

            put(
                "destOverride",
                buildJsonArray {
                    add(JsonPrimitive("http"))
                    add(JsonPrimitive("tls"))
                    add(JsonPrimitive("quic"))
                }
            )
        }

        val tproxy = buildJsonObject {
            put("listen", settings.tproxyAddress)
            put("port", settings.tproxyPort)
            put("protocol", "dokodemo-door")

            put(
                "settings",
                buildJsonObject {
                    put("network", "tcp,udp")
                    put("followRedirect", true)
                }
            )

            put("sniffing", sniffing)

            put(
                "streamSettings",
                buildJsonObject {
                    put(
                        "sockopt",
                        buildJsonObject {
                            put("tproxy", "tproxy")
                        }
                    )
                }
            )

            put("tag", "all-in")
        }

        val socksSettings = buildJsonObject {
            put("udp", true)

            if (
                settings.socksUsername.trim().isNotEmpty() &&
                settings.socksPassword.trim().isNotEmpty()
            ) {
                put("auth", "password")

                put(
                    "accounts",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("user", settings.socksUsername)
                                put("pass", settings.socksPassword)
                            }
                        )
                    }
                )
            }
        }

        val socks = buildJsonObject {
            put("listen", settings.socksAddress)
            put("port", settings.socksPort.toInt())
            put("protocol", "socks")
            put("settings", socksSettings)
            put("sniffing", sniffing)
            put("tag", "socks")
        }

        return buildJsonArray {
            when (settings.transparentProxy) {
                true -> add(tproxy)
                false -> add(socks)
            }
        }
    }

    private fun outbounds(): JsonArray {
        val outbound = this@LinkHelper.outbound!!

        remark = outbound["sendThrough"]?.jsonPrimitive?.contentOrNull ?:
        outbound["tag"]?.jsonPrimitive?.contentOrNull ?:
        REMARK_DEFAULT

        val proxy = buildJsonObject {
            for ((key, value) in outbound) {
                if (key != "sendThrough" && key != "tag") {
                    put(key, value)
                }
            }

            put("tag", "proxy")
        }

        val direct = buildJsonObject {
            put("protocol", "freedom")
            put("tag", "direct")
        }

        val block = buildJsonObject {
            put("protocol", "blackhole")
            put("tag", "block")
        }

        val dns = buildJsonObject {
            put("protocol", "dns")
            put("tag", "dns-out")
        }

        return buildJsonArray {
            add(proxy)
            add(direct)
            add(block)
            if (settings.transparentProxy) add(dns)
        }
    }

    private fun routing(): JsonObject {
        val proxyDns = buildJsonObject {
            if (settings.transparentProxy) {
                put("network", "udp")
                put("port", 53)
                put(
                    "inboundTag",
                    buildJsonArray {
                        add(JsonPrimitive("all-in"))
                    }
                )
                put("outboundTag", "dns-out")
            } else {
                put(
                    "ip",
                    buildJsonArray {
                        add(JsonPrimitive(settings.primaryDns))
                        add(JsonPrimitive(settings.secondaryDns))
                    }
                )
                put("port", 53)
                put("outboundTag", "proxy")
            }
        }

        val directPrivate = buildJsonObject {
            put(
                "ip",
                buildJsonArray {
                    add(JsonPrimitive("geoip:private"))
                }
            )
            put("outboundTag", "direct")
        }

        return buildJsonObject {
            put("domainStrategy", "IPIfNonMatch")
            put(
                "rules",
                buildJsonArray {
                    add(proxyDns)
                    add(directPrivate)
                }
            )
        }
    }

    private fun config(): JsonObject {
        return buildJsonObject {
            put("log", log())
            put("dns", dns())
            put("inbounds", inbounds())
            put("outbounds", outbounds())
            put("routing", routing())
        }
    }
}
