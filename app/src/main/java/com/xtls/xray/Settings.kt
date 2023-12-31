package com.xtls.xray

import android.content.Context
import android.content.SharedPreferences
import java.io.File

object Settings {
    var socksAddress: String = "127.0.0.1"
    var socksPort: String = "10808"
    var socksUdp: Boolean = true
    var primaryDns: String = "1.1.1.1"
    var secondaryDns: String = "1.0.0.1"
    var excludedApps: String = ""
    var useXray: Boolean = false
    var tunName: String = "tun0"
    var tunMtu: Int = 1500
    var tunGateway: String = "10.14.1.1"
    var tunAddress: String = "10.14.1.2"
    var tunPrefix: Int = 24

    fun xrayConfig(context: Context): File = File(context.filesDir, "config.json")
    fun tun2socksConfig(context: Context): File = File(context.filesDir, "tun2socks.yml")
    fun sharedPref(context: Context): SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
}
