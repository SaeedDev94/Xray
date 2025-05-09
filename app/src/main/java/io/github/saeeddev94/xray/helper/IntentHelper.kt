package io.github.saeeddev94.xray.helper

import android.content.Intent
import android.os.Build

class IntentHelper {
    companion object {
        fun <T> getParcelable(intent: Intent, name: String, clazz: Class<T>): T? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(name, clazz)
            } else {
                @Suppress("deprecation")
                intent.getParcelableExtra(name)
            }
        }
    }
}
