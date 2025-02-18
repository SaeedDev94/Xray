package io.github.saeeddev94.xray.utils

import kotlinx.serialization.Serializable

@Serializable
data class RunXrayRequest(
    val datDir: String,
    val configPath: String,
    val maxMemory: Long
)
