package io.github.saeeddev94.xray.activity

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.saeeddev94.xray.BuildConfig
import io.github.saeeddev94.xray.R
import io.github.saeeddev94.xray.databinding.ActivityLogsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class LogsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogsBinding

    companion object {
        private const val MAX_BUFFERED_LINES = (1 shl 14) - 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.logs)
        binding = ActivityLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        lifecycleScope.launch(Dispatchers.IO) { streamingLog() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_logs, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.deleteLogs -> {
                flush()
            }
            R.id.copyLogs -> copyToClipboard(binding.logsTextView.text.toString())
            else -> finish()
        }
        return true
    }

    private fun flush() {
        lifecycleScope.launch(Dispatchers.IO) {
            val command = listOf("logcat", "-c")
            val process = ProcessBuilder(command).start()
            process.waitFor()
            withContext(Dispatchers.Main) {
                binding.logsTextView.text = ""
            }
        }
    }

    private fun copyToClipboard(text: String) {
        if (text.isBlank()) return
        try {
            val clipData = ClipData.newPlainText(null, text)
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(applicationContext, "Logs copied", Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun streamingLog() = withContext(Dispatchers.IO) {
        val builder = ProcessBuilder("logcat", "-v", "time", "-s", "GoLog,${BuildConfig.APPLICATION_ID}")
        builder.environment()["LC_ALL"] = "C"
        var process: Process? = null
        try {
            process = try {
                builder.start()
            } catch (e: IOException) {
                Log.e(packageName, Log.getStackTraceString(e))
                return@withContext
            }
            val stdout = BufferedReader(InputStreamReader(process!!.inputStream, StandardCharsets.UTF_8))

            var timeLastNotify = System.nanoTime()
            val bufferedLogLines = arrayListOf<String>()
            var timeout = 1000000000L / 2 // The timeout is initially small so that the view gets populated immediately.

            while (true) {
                val line = stdout.readLine() ?: break
                bufferedLogLines.add(line)
                val timeNow = System.nanoTime()
                if (bufferedLogLines.size < MAX_BUFFERED_LINES && (timeNow - timeLastNotify) < timeout && stdout.ready())
                    continue
                timeout = 1000000000L * 5 / 2 // Increase the timeout after the initial view has something in it.
                timeLastNotify = timeNow

                withContext(Dispatchers.Main) {
                    val contentHeight = binding.logsTextView.height
                    val scrollViewHeight = binding.logsScrollView.height
                    val isScrolledToBottomAlready = (binding.logsScrollView.scrollY + scrollViewHeight) >= contentHeight * 0.95
                    binding.logsTextView.text = binding.logsTextView.text.toString() + bufferedLogLines.joinToString(separator = "\n", postfix = "\n")
                    bufferedLogLines.clear()
                    if (isScrolledToBottomAlready) {
                        binding.logsScrollView.post {
                            binding.logsScrollView.fullScroll(View.FOCUS_DOWN)
                        }
                    }
                }
            }
        } finally {
            process?.destroy()
        }
    }
}
