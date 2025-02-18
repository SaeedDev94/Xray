package io.github.saeeddev94.xray.utils

import libXray.LibXray
import java.util.Base64
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object XrayCore {
    private inline fun <reified T> encodeRequest(request: T): String {
        val jsonRequest = Json.encodeToString(request)
        return Base64.getEncoder().encodeToString(jsonRequest.toByteArray())
    }

    private fun decodeResponse(base64Response: String): CallResponse {
        val decodedResponse = Base64.getDecoder().decode(base64Response).decodeToString()
        return Json.decodeFromString<CallResponse>(decodedResponse)
    }

    fun test(dir: String, config: String): String {
        val request = encodeRequest(TestXrayRequest(datDir = dir, configPath = config))
        val response = decodeResponse(LibXray.testXray(request))
        return if (response.success) response.data else response.err
    }

    fun start(dir: String, config: String, memory: Long): String {
        val request = encodeRequest(RunXrayRequest(datDir = dir, configPath = config, maxMemory = memory))
        val response = decodeResponse(LibXray.runXray(request))
        return if (response.success) response.data else response.err
    }

    fun stop(): String {
        val response = decodeResponse(LibXray.stopXray())
        return if (response.success) response.data else response.err
    }

    fun version(): String {
        val response = decodeResponse(LibXray.xrayVersion())
        return if (response.success) response.data else response.err
    }

    fun json(link: String): String {
        val encodedLink = Base64.getEncoder().encodeToString(link.toByteArray())
        val response = decodeResponse(LibXray.convertShareLinksToXrayJson(encodedLink))
        return if (response.success) response.data else response.err
    }
}
