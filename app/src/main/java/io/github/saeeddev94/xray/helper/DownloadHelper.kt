package io.github.saeeddev94.xray.helper

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

class DownloadHelper(private val url: String, private val file: File, private val callback: DownloadListener) {

    fun start() {
        var input: InputStream? = null
        var output: OutputStream? = null
        var connection: HttpURLConnection? = null

        try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Expected HTTP ${HttpURLConnection.HTTP_OK} but received HTTP ${connection.responseCode}")
            }

            input = connection.inputStream
            output = FileOutputStream(file)

            val fileLength = connection.contentLength
            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            while (input.read(data).also { count = it } != -1) {
                total += count.toLong()
                if (fileLength > 0) {
                    val progress = (total * 100 / fileLength).toInt()
                    callback.onProgress(progress)
                }
                output.write(data, 0, count)
            }
            callback.onComplete()
        } catch (exception: Exception) {
            callback.onError(exception)
        } finally {
            try {
                output?.close()
                input?.close()
            } catch (_: IOException) {
            }

            connection?.disconnect()
        }
    }

    interface DownloadListener {
        fun onProgress(progress: Int)
        fun onError(exception: Exception)
        fun onComplete()
    }
}
