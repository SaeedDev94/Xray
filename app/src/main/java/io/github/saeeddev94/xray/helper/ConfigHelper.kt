package io.github.saeeddev94.xray.helper

import io.github.saeeddev94.xray.Settings
import io.github.saeeddev94.xray.database.Config
import org.json.JSONObject

class ConfigHelper(
    settings: Settings,
    config: Config,
    base: String,
) {
    private val base: JSONObject = JsonHelper.makeObject(base)

    init {
        process("log", config.log, config.logMode)
        process("dns", config.dns, config.dnsMode)
        process("inbounds", config.inbounds, config.inboundsMode)
        process("outbounds", config.outbounds, config.outboundsMode)
        process("routing", config.routing, config.routingMode)
        if (settings.tproxyHotspot || settings.tproxyTethering) sharedInbounds()
    }

    override fun toString(): String {
        return base.toString(4)
    }

    private fun process(key: String, config: String, mode: Config.Mode) {
        if (mode == Config.Mode.Disable) return
        if (arrayOf("inbounds", "outbounds").contains(key)) {
            processArray(key, config, mode)
            return
        }
        processObject(key, config, mode)
    }

    private fun processObject(key: String, config: String, mode: Config.Mode) {
        val oldValue = JsonHelper.getObject(base, key)
        val newValue = JsonHelper.makeObject(config)
        val final = if (mode == Config.Mode.Replace) newValue
        else JsonHelper.mergeObjects(oldValue, newValue)
        base.put(key, final)
    }

    private fun processArray(key: String, config: String, mode: Config.Mode) {
        val oldValue = JsonHelper.getArray(base, key)
        val newValue = JsonHelper.makeArray(config)
        val final = if (mode == Config.Mode.Replace) newValue
        else JsonHelper.mergeArrays(oldValue, newValue, "protocol")
        base.put(key, final)
    }

    private fun sharedInbounds() {
        val key = "inbounds"
        val inbounds = JsonHelper.getArray(base, key)
        for (i in 0 until inbounds.length()) {
            val inbound = inbounds[i]
            if (inbound is JSONObject && inbound.has("listen")) {
                inbound.remove("listen")
                inbounds.put(i, inbound)
            }
        }
        base.put(key, inbounds)
    }
}
