package io.github.saeeddev94.xray.dto

import android.graphics.drawable.Drawable

data class AppList(
    var appIcon: Drawable,
    var appName: String,
    var packageName: String,
)
