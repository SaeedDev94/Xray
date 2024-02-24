package io.github.saeeddev94.xray.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.saeeddev94.xray.BuildConfig
import io.github.saeeddev94.xray.R
import io.github.saeeddev94.xray.databinding.ActivityLogsBinding
import java.io.BufferedReader
import java.io.InputStreamReader

class LogsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogsBinding
    private var loggingThread: Thread? = null
    private var loggingProcess: Process? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.logs)
        binding = ActivityLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onStart() {
        super.onStart()
        startLoop()
    }

    override fun onStop() {
        super.onStop()
        stopLoop()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_logs, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.deleteLogs -> {
                stopLoop()
                flush()
                startLoop()
            }
            R.id.copyLogs -> copyToClipboard(binding.logsTextView.text.toString())
            else -> finish()
        }
        return true
    }

    private fun startLoop() {
        loggingThread = Thread {
            try {
                loggingProcess = ProcessBuilder("logcat", "-v", "raw", "-s", "GoLog,${BuildConfig.APPLICATION_ID}").start()
                val reader = BufferedReader(InputStreamReader(loggingProcess!!.inputStream))
                val logs = StringBuilder()
                while (!Thread.currentThread().isInterrupted && reader.readLine().also { logs.append("$it\n") } != null) {
                    runOnUiThread {
                        binding.logsTextView.text = logs.toString()
                        binding.logsScrollView.post {
                            binding.logsScrollView.fullScroll(ScrollView.FOCUS_DOWN)
                        }
                    }
                }
            } catch (error: Exception) {
                error.printStackTrace()
            } finally {
                loggingProcess?.destroy()
            }
        }
        loggingThread?.start()
    }

    private fun stopLoop() {
        loggingThread?.interrupt()
        loggingProcess?.destroy()
        loggingThread = null
        loggingProcess = null
    }

    private fun flush() {
        val command = listOf("logcat", "-c")
        val process = ProcessBuilder(command).start()
        process.waitFor()
        binding.logsTextView.text = ""
    }

    private fun copyToClipboard(text: String) {
        try {
            val clipData = ClipData.newPlainText(null, text)
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(applicationContext, "Logs copied", Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }

}
