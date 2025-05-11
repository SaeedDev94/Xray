package io.github.saeeddev94.xray.helper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadHelper(
    private val scope: CoroutineScope,
    private val url: String,
    private val file: File,
    private val callback: DownloadListener,
) {

    fun start() {
        scope.launch(Dispatchers.IO) {
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
                        withContext(Dispatchers.Main) {
                            callback.onProgress(progress)
                        }
                    }
                    output.write(data, 0, count)
                }
                withContext(Dispatchers.Main) {
                    callback.onComplete()
                }
            } catch (exception: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError(exception)
                }
            } finally {
                try {
                    output?.close()
                    input?.close()
                } catch (_: IOException) {
                }

                connection?.disconnect()
            }
        }
    }

    interface DownloadListener {
        fun onProgress(progress: Int)
        fun onError(exception: Exception)
        fun onComplete()
    }
}
