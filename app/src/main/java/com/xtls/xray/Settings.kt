package com.xtls.xray

import android.content.Context
import android.content.SharedPreferences
import java.io.File

object Settings {
    var socksAddress: String = "127.0.0.1"
    var socksPort: String = "10808"
    var primaryDns: String = "1.1.1.1"
    var secondaryDns: String = "1.0.0.1"
    var useXray: Boolean = false
    var MTU: Int = 1500

    fun xrayConfig(context: Context): File = File(context.filesDir, "config.json")
    fun tun2socksConfig(context: Context): File = File(context.filesDir, "tun2socks.yml")
    fun sharedPref(context: Context): SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
}
