package com.xtls.xray

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import com.xtls.xray.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        init {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    private lateinit var vpnService: XrayVpnService
    private var vpnServiceBound: Boolean = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as XrayVpnService.ServiceBinder
            vpnService = binder.getService()
            vpnServiceBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            vpnServiceBound = false
        }
    }

    private lateinit var binding: ActivityMainBinding
    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            toggleVpnService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setAppVersion()
        setXrayVersion()
        setSettings()
        binding.toggleButton.setOnClickListener { onToggleButtonClick() }
        binding.launchSettings.setOnClickListener {
            val intent = Intent(applicationContext, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, XrayVpnService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        vpnServiceBound = false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 1) onToggleButtonClick()
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun sharedPref(): SharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)

    private fun setAppVersion() {
        binding.appVersion.text = BuildConfig.VERSION_NAME
    }

    private fun setXrayVersion() {
        val sharedPref = sharedPref()
        binding.xrayVersion.text = sharedPref.getString("xrayVersion", "-")
    }

    private fun setSettings() {
        val sharedPref = sharedPref()
        Settings.socksAddress = sharedPref.getString("socksAddress", Settings.socksAddress)!!
        Settings.socksPort = sharedPref.getString("socksPort", Settings.socksPort)!!
        Settings.primaryDns = sharedPref.getString("primaryDns", Settings.primaryDns)!!
        Settings.secondaryDns = sharedPref.getString("secondaryDns", Settings.secondaryDns)!!
        Settings.useXray = sharedPref.getBoolean("useXray", Settings.useXray)
    }

    private fun onToggleButtonClick() {
        if (!vpnServiceBound) return
        val sharedPref = sharedPref()
        val isXrayUpToDate = sharedPref.getInt("xrayAppVersionCode", 0) == BuildConfig.VERSION_CODE
        if (!isXrayUpToDate) {
            Toast.makeText(applicationContext, "Updating XTLS/Xray-core", Toast.LENGTH_SHORT).show()
            Thread {
                vpnService.installXray()
                val process = Runtime.getRuntime().exec(arrayOf(vpnService.xrayPath(), "version"))
                val xrayVersion = process.inputStream.bufferedReader().readText()
                sharedPref.edit()
                    .putInt("xrayAppVersionCode", BuildConfig.VERSION_CODE)
                    .putString("xrayVersion", xrayVersion)
                    .apply()
                runOnUiThread {
                    setXrayVersion()
                }
            }.start()
            return
        }
        if (Settings.useXray) {
            if (!vpnService.isConfigExists()) {
                Toast.makeText(applicationContext, "Xray config file missed", Toast.LENGTH_SHORT).show()
                return
            }
        }
        if (!hasPostNotification()) return
        val vpn = VpnService.prepare(this)
        if (vpn != null) {
            resultLauncher.launch(vpn)
        } else {
            toggleVpnService()
        }
    }

    private fun toggleVpnService() {
        if (vpnService.getIsRunning()) {
            Toast.makeText(applicationContext, "Stop VPN", Toast.LENGTH_SHORT).show()
            vpnService.stopVPN()
        } else {
            Toast.makeText(applicationContext, "Start VPN", Toast.LENGTH_SHORT).show()
            vpnService.startVPN()
        }
    }

    private fun hasPostNotification(): Boolean {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            return false
        }
        return true
    }
}
