package io.github.saeeddev94.xray

import android.content.Context
import androidx.core.content.edit
import java.io.File

class Settings(private val context: Context) {

    private val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Active Link ID */
    var selectedLink: Long
        get() = sharedPreferences.getLong("selectedLink", 0L)
        set(value) = sharedPreferences.edit { putLong("selectedLink", value) }

    /** Active Profile ID */
    var selectedProfile: Long
        get() = sharedPreferences.getLong("selectedProfile", 0L)
        set(value) = sharedPreferences.edit { putLong("selectedProfile", value) }

    /** The time of last refresh */
    var lastRefreshLinks: Long
        get() = sharedPreferences.getLong("lastRefreshLinks", 0L)
        set(value) = sharedPreferences.edit { putLong("lastRefreshLinks", value) }

    /**
     * Apps Routing
     * Mode: true -> exclude, false -> include
     * Default: exclude
     */
    var appsRoutingMode: Boolean
        get() = sharedPreferences.getBoolean("appsRoutingMode", true)
        set(value) = sharedPreferences.edit { putBoolean("appsRoutingMode", value) }
    var appsRouting: String
        get() = sharedPreferences.getString("excludedApps", "")!!
        set(value) = sharedPreferences.edit { putString("excludedApps", value) }

    /** Basic */
    var socksAddress: String
        get() = sharedPreferences.getString("socksAddress", "127.0.0.1")!!
        set(value) = sharedPreferences.edit { putString("socksAddress", value) }
    var socksPort: String
        get() = sharedPreferences.getString("socksPort", "10808")!!
        set(value) = sharedPreferences.edit { putString("socksPort", value) }
    var socksUsername: String
        get() = sharedPreferences.getString("socksUsername", "")!!
        set(value) = sharedPreferences.edit { putString("socksUsername", value) }
    var socksPassword: String
        get() = sharedPreferences.getString("socksPassword", "")!!
        set(value) = sharedPreferences.edit { putString("socksPassword", value) }
    var geoIpAddress: String
        get() = sharedPreferences.getString(
            "geoIpAddress",
            "https://github.com/v2fly/geoip/releases/latest/download/geoip.dat"
        )!!
        set(value) = sharedPreferences.edit { putString("geoIpAddress", value) }
    var geoSiteAddress: String
        get() = sharedPreferences.getString(
            "geoSiteAddress",
            "https://github.com/v2fly/domain-list-community/releases/latest/download/dlc.dat"
        )!!
        set(value) = sharedPreferences.edit { putString("geoSiteAddress", value) }
    var pingAddress: String
        get() = sharedPreferences.getString("pingAddress", "https://www.google.com")!!
        set(value) = sharedPreferences.edit { putString("pingAddress", value) }
    var pingTimeout: Int
        get() = sharedPreferences.getInt("pingTimeout", 5)
        set(value) = sharedPreferences.edit { putInt("pingTimeout", value) }
    var refreshLinksInterval: Int
        get() = sharedPreferences.getInt("refreshLinksInterval", 60)
        set(value) = sharedPreferences.edit { putInt("refreshLinksInterval", value) }
    var bypassLan: Boolean
        get() = sharedPreferences.getBoolean("bypassLan", true)
        set(value) = sharedPreferences.edit { putBoolean("bypassLan", value) }
    var enableIpV6: Boolean
        get() = sharedPreferences.getBoolean("enableIpV6", true)
        set(value) = sharedPreferences.edit { putBoolean("enableIpV6", value) }
    var socksUdp: Boolean
        get() = sharedPreferences.getBoolean("socksUdp", true)
        set(value) = sharedPreferences.edit { putBoolean("socksUdp", value) }
    var bootAutoStart: Boolean
        get() = sharedPreferences.getBoolean("bootAutoStart", false)
        set(value) = sharedPreferences.edit { putBoolean("bootAutoStart", value) }
    var refreshLinksOnOpen: Boolean
        get() = sharedPreferences.getBoolean("refreshLinksOnOpen", false)
        set(value) = sharedPreferences.edit { putBoolean("refreshLinksOnOpen", value) }

    /** Advanced */
    var primaryDns: String
        get() = sharedPreferences.getString("primaryDns", "1.1.1.1")!!
        set(value) = sharedPreferences.edit { putString("primaryDns", value) }
    var secondaryDns: String
        get() = sharedPreferences.getString("secondaryDns", "1.0.0.1")!!
        set(value) = sharedPreferences.edit { putString("secondaryDns", value) }
    var primaryDnsV6: String
        get() = sharedPreferences.getString("primaryDnsV6", "2606:4700:4700::1111")!!
        set(value) = sharedPreferences.edit { putString("primaryDnsV6", value) }
    var secondaryDnsV6: String
        get() = sharedPreferences.getString("secondaryDnsV6", "2606:4700:4700::1001")!!
        set(value) = sharedPreferences.edit { putString("secondaryDnsV6", value) }
    var tunName: String
        get() = sharedPreferences.getString("tunName", "tun0")!!
        set(value) = sharedPreferences.edit { putString("tunName", value) }
    var tunMtu: Int
        get() = sharedPreferences.getInt("tunMtu", 8500)
        set(value) = sharedPreferences.edit { putInt("tunMtu", value) }
    var tunAddress: String
        get() = sharedPreferences.getString("tunAddress", "10.10.10.10")!!
        set(value) = sharedPreferences.edit { putString("tunAddress", value) }
    var tunPrefix: Int
        get() = sharedPreferences.getInt("tunPrefix", 32)
        set(value) = sharedPreferences.edit { putInt("tunPrefix", value) }
    var tunAddressV6: String
        get() = sharedPreferences.getString("tunAddressV6", "fc00::1")!!
        set(value) = sharedPreferences.edit { putString("tunAddressV6", value) }
    var tunPrefixV6: Int
        get() = sharedPreferences.getInt("tunPrefixV6", 128)
        set(value) = sharedPreferences.edit { putInt("tunPrefixV6", value) }
    var tproxyAddress: String
        get() = sharedPreferences.getString("tproxyAddress", "127.0.0.1")!!
        set(value) = sharedPreferences.edit { putString("tproxyAddress", value) }
    var tproxyPort: Int
        get() = sharedPreferences.getInt("tproxyPort", 10888)
        set(value) = sharedPreferences.edit { putInt("tproxyPort", value) }
    var tproxyBypassWiFi: Set<String>
        get() = sharedPreferences.getStringSet("tproxyBypassWiFi", mutableSetOf<String>())!!
        set(value) = sharedPreferences.edit { putStringSet("tproxyBypassWiFi", value) }
    var tproxyAutoConnect: Boolean
        get() = sharedPreferences.getBoolean("tproxyAutoConnect", false)
        set(value) = sharedPreferences.edit { putBoolean("tproxyAutoConnect", value) }
    var transparentProxy: Boolean
        get() = sharedPreferences.getBoolean("transparentProxy", false)
        set(value) = sharedPreferences.edit { putBoolean("transparentProxy", value) }

    fun baseDir(): File = context.filesDir
    fun xrayCoreFile(): File = File(baseDir(), "xray")
    fun xrayHelperFile(): File = File(baseDir(), "xrayhelper")
    fun testConfig(): File = File(baseDir(), "test.json")
    fun xrayConfig(): File = File(baseDir(), "config.json")
    fun tun2socksConfig(): File = File(baseDir(), "tun2socks.yml")
    fun xrayHelperConfig(): File = File(baseDir(), "config.yml")
    fun xrayCorePid(): File = File(baseDir(), "core.pid")
    fun networkMonitorPid(): File = File(baseDir(), "monitor.pid")
    fun networkMonitorScript(): File = File(baseDir(), "monitor.sh")
}
