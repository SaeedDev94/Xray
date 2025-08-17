package io.github.saeeddev94.xray.activity

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.topjohnwu.superuser.Shell
import io.github.saeeddev94.xray.R
import io.github.saeeddev94.xray.Settings
import io.github.saeeddev94.xray.databinding.ActivityAssetsBinding
import io.github.saeeddev94.xray.helper.DownloadHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.text.toRegex

class AssetsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAssetsBinding
    private var downloading: Boolean = false

    private val settings by lazy { Settings(applicationContext) }
    private val geoIpLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
        writeToFile(it, geoIpFile())
    }
    private val geoSiteLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
        writeToFile(it, geoSiteFile())
    }
    private val xrayCoreLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
        val file = settings.xrayCoreFile()
        writeToFile(it, file) {
            Shell.cmd("chown root:root ${file.absolutePath}").exec()
            Shell.cmd("chmod +x ${file.absolutePath}").exec()
        }
    }
    private val xrayHelperLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
        val file = settings.xrayHelperFile()
        writeToFile(it, file) {
            Shell.cmd("chown root:root ${file.absolutePath}").exec()
            Shell.cmd("chmod +x ${file.absolutePath}").exec()
        }
    }

    private fun geoIpFile(): File = File(applicationContext.filesDir, "geoip.dat")
    private fun geoSiteFile(): File = File(applicationContext.filesDir, "geosite.dat")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mimeType = "application/octet-stream"
        title = getString(R.string.assets)
        binding = ActivityAssetsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setAssetStatus()

        // GeoIP
        binding.geoIpDownload.setOnClickListener {
            download(settings.geoIpAddress, geoIpFile(), binding.geoIpSetup, binding.geoIpProgress)
        }
        binding.geoIpFile.setOnClickListener { geoIpLauncher.launch(mimeType) }
        binding.geoIpDelete.setOnClickListener { delete(geoIpFile()) }

        // GeoSite
        binding.geoSiteDownload.setOnClickListener {
            download(
                settings.geoSiteAddress,
                geoSiteFile(),
                binding.geoSiteSetup,
                binding.geoSiteProgress
            )
        }
        binding.geoSiteFile.setOnClickListener { geoSiteLauncher.launch(mimeType) }
        binding.geoSiteDelete.setOnClickListener { delete(geoSiteFile()) }

        // XTLS/Xray-core
        binding.xrayCoreFile.setOnClickListener { runAsRoot { xrayCoreLauncher.launch(mimeType) } }
        binding.xrayCoreDelete.setOnClickListener { delete(settings.xrayCoreFile()) }

        // Asterisk4Magisk/XrayHelper
        binding.xrayHelperFile.setOnClickListener { runAsRoot { xrayHelperLauncher.launch(mimeType) } }
        binding.xrayHelperDelete.setOnClickListener { delete(settings.xrayHelperFile()) }
    }

    @SuppressLint("SimpleDateFormat")
    private fun getFileDate(file: File): String {
        return if (file.exists()) {
            val date = Date(file.lastModified())
            SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(date)
        } else {
            getString(R.string.noValue)
        }
    }

    private fun getXrayCoreVersion(file: File): String {
        return getExeVersion(file, "${file.absolutePath} version", "Xray")
    }

    private fun getXrayHelperVersion(file: File): String {
        return getExeVersion(file, "${file.absolutePath} --help", "XrayHelper")
    }

    private fun getExeVersion(file: File, cmd: String, name: String): String {
        val exists = file.exists()
        val invalid = {
            delete(file)
            "Invalid"
        }
        return if (exists) {
            val result = Shell.cmd(cmd).exec()
            if (result.isSuccess) {
                val txt = result.out.first()
                val match = "$name (.*?) ".toRegex().find(txt)
                match?.groups?.get(1)?.value ?: invalid()
            } else invalid()
        } else getString(R.string.noValue)
    }

    private fun setAssetStatus() {
        val geoIp = geoIpFile()
        val geoIpExists = geoIp.exists()
        binding.geoIpDate.text = getFileDate(geoIp)
        binding.geoIpSetup.visibility = if (geoIpExists) View.GONE else View.VISIBLE
        binding.geoIpInstalled.visibility = if (geoIpExists) View.VISIBLE else View.GONE
        binding.geoIpProgress.visibility = View.GONE

        val geoSite = geoSiteFile()
        val geoSiteExists = geoSite.exists()
        binding.geoSiteDate.text = getFileDate(geoSite)
        binding.geoSiteSetup.visibility = if (geoSiteExists) View.GONE else View.VISIBLE
        binding.geoSiteInstalled.visibility = if (geoSiteExists) View.VISIBLE else View.GONE
        binding.geoSiteProgress.visibility = View.GONE

        val xrayCore = settings.xrayCoreFile()
        val xrayCoreExists = xrayCore.exists()
        binding.xrayCoreVersion.text = getXrayCoreVersion(xrayCore)
        binding.xrayCoreSetup.isVisible = !xrayCoreExists
        binding.xrayCoreInstalled.isVisible = xrayCoreExists

        val xrayHelper = settings.xrayHelperFile()
        val xrayHelperExists = xrayHelper.exists()
        binding.xrayHelperVersion.text = getXrayHelperVersion(xrayHelper)
        binding.xrayHelperSetup.isVisible = !xrayHelperExists
        binding.xrayHelperInstalled.isVisible = xrayHelperExists
    }

    private fun download(url: String, file: File, setup: LinearLayout, progressBar: ProgressBar) {
        if (downloading) {
            Toast.makeText(
                applicationContext, "Another download is running, please wait", Toast.LENGTH_SHORT
            ).show()
            return
        }

        setup.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0

        downloading = true
        DownloadHelper(lifecycleScope, url, file, object : DownloadHelper.DownloadListener {
            override fun onProgress(progress: Int) {
                progressBar.progress = progress
            }

            override fun onError(exception: Exception) {
                downloading = false
                Toast.makeText(applicationContext, exception.message, Toast.LENGTH_SHORT).show()
                setAssetStatus()
            }

            override fun onComplete() {
                downloading = false
                setAssetStatus()
            }
        }).start()
    }

    private fun writeToFile(uri: Uri?, file: File, cb: (() -> Unit)? = null) {
        if (uri == null) return
        lifecycleScope.launch {
            contentResolver.openInputStream(uri).use { input ->
                FileOutputStream(file).use { output ->
                    input?.copyTo(output)
                }
            }
            if (cb != null) cb()
            withContext(Dispatchers.Main) {
                setAssetStatus()
            }
        }
    }

    private fun delete(file: File) {
        lifecycleScope.launch {
            file.delete()
            withContext(Dispatchers.Main) {
                setAssetStatus()
            }
        }
    }

    private fun runAsRoot(cb: () -> Unit) {
        val result = Shell.cmd("whoami").exec()
        if (result.isSuccess && result.out.first() == "root") {
            cb()
            return
        }
        Toast.makeText(this, "Root Required", Toast.LENGTH_SHORT).show()
    }

}
