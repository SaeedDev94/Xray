package io.github.saeeddev94.xray.helper

import java.io.File

class FileHelper {

    fun createOrUpdate(file: File, content: String) {
        val fileContent = if (file.exists()) file.bufferedReader().use { it.readText() } else ""
        if (content != fileContent) file.writeText(content)
    }

}
