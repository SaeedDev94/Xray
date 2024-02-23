package io.github.saeeddev94.xray

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.saeeddev94.xray.databinding.ActivityLogsBinding

class LogsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.logs)
        binding = ActivityLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        resolve()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_logs, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.deleteLogs -> resolve(true)
            R.id.copyLogs -> copyToClipboard(binding.logsTextView.text.toString())
            else -> finish()
        }
        return true
    }

    private fun resolve(flush: Boolean = false) {
        Thread {
            if (flush) flush()
            val logs = logs()
            runOnUiThread {
                binding.logsTextView.text = logs
                binding.logsTextView.movementMethod = ScrollingMovementMethod()
                Handler(Looper.getMainLooper()).post { binding.logsScrollView.fullScroll(View.FOCUS_DOWN) }
            }
        }.start()
    }

    private fun flush() {
        val command = listOf("logcat", "-c")
        val process = ProcessBuilder(command).start()
        process.waitFor()
    }

    private fun logs(): String {
        val command = listOf("logcat", "-d", "-s", "GoLog,${BuildConfig.APPLICATION_ID}")
        val process = ProcessBuilder(command).start()
        process.waitFor()
        return process.inputStream.bufferedReader().use { it.readText() }
    }

    private fun copyToClipboard(text: String) {
        try {
            val clipData = ClipData.newPlainText(null, text)
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(applicationContext, "Logs copied", Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            // ignore
        }
    }

}
