package com.xtls.xray

import android.content.Context
import java.io.File

object Settings {
    var socksAddress: String = "127.0.0.1"
    var socksPort: String = "10808"
    var primaryDns: String = "1.1.1.1"
    var secondaryDns: String = "1.0.0.1"
    var useXray: Boolean = false

    fun configFile(context: Context): File = File(context.filesDir, "config.json")
}
