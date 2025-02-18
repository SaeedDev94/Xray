package io.github.saeeddev94.xray.utils

import kotlinx.serialization.Serializable

@Serializable
data class TestXrayRequest(
    val datDir: String,
    val configPath: String
)
