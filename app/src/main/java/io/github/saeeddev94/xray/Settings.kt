package io.github.saeeddev94.xray

import android.content.Context
import android.content.SharedPreferences
import java.io.File

object Settings {
    var primaryDns: String = "1.1.1.1"
    var secondaryDns: String = "1.0.0.1"
    var socksAddress: String = "127.0.0.1"
    var socksPort: String = "10808"
    var socksUsername: String = ""
    var socksPassword: String = ""
    var excludedApps: String = ""
    var bypassLan: Boolean = true
    var socksUdp: Boolean = true
    var tunName: String = "tun0"
    var tunMtu: Int = 1500
    var tunGateway: String = "10.14.1.1"
    var tunAddress: String = "10.14.1.2"
    var tunPrefix: Int = 24
    var selectedProfile: Long = 0L
    var pingTimeout: Int = 5
    var pingAddress: String = "https://developers.google.com"
    var geoIpAddress: String = "https://github.com/v2fly/geoip/releases/latest/download/geoip.dat"
    var geoSiteAddress: String = "https://github.com/v2fly/domain-list-community/releases/latest/download/dlc.dat"

    fun testConfig(context: Context): File = File(context.filesDir, "test.json")
    fun xrayConfig(context: Context): File = File(context.filesDir, "config.json")
    fun tun2socksConfig(context: Context): File = File(context.filesDir, "tun2socks.yml")
    fun sharedPref(context: Context): SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
}
