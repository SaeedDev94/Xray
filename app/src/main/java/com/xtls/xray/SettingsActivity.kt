package com.xtls.xray

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.xtls.xray.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.socksAddress.setText(Settings.socksAddress)
        binding.socksPort.setText(Settings.socksPort)
        binding.primaryDns.setText(Settings.primaryDns)
        binding.secondaryDns.setText(Settings.secondaryDns)
        binding.useXray.isChecked = Settings.useXray
        binding.socksUdp.isChecked = Settings.socksUdp
        binding.excludedApps.setText(Settings.excludedApps)
        binding.xrayConfig.setText(getXrayConfig())
        binding.saveSettings.setOnClickListener {
            saveSettings()
        }
        binding.useXray.setOnCheckedChangeListener { _, _ ->
            toggleSettings()
        }
        toggleSettings()
    }

    private fun toggleSettings() {
        val checked = binding.useXray.isChecked
        if (checked) {
            binding.xraySettings.visibility = View.VISIBLE
        } else {
            binding.xraySettings.visibility = View.GONE
        }
    }

    private fun getXrayConfig(): String {
        val configFile = Settings.xrayConfig(applicationContext)
        if (!configFile.exists() || !configFile.isFile) return ""
        val bufferedReader = configFile.bufferedReader()
        return bufferedReader.use { it.readText() }
    }

    private fun saveSettings() {
        Settings.socksAddress = binding.socksAddress.text.toString()
        Settings.socksPort = binding.socksPort.text.toString()
        Settings.primaryDns = binding.primaryDns.text.toString()
        Settings.secondaryDns = binding.secondaryDns.text.toString()
        Settings.excludedApps = binding.excludedApps.text.toString()
        Settings.useXray = binding.useXray.isChecked
        Settings.socksUdp = binding.socksUdp.isChecked
        val sharedPref = Settings.sharedPref(applicationContext)
        sharedPref.edit()
            .putString("socksAddress", Settings.socksAddress)
            .putString("socksPort", Settings.socksPort)
            .putString("primaryDns", Settings.primaryDns)
            .putString("secondaryDns", Settings.secondaryDns)
            .putString("excludedApps", Settings.excludedApps)
            .putBoolean("useXray", Settings.useXray)
            .putBoolean("socksUdp", Settings.socksUdp)
            .apply()
        Settings.xrayConfig(applicationContext).writeText(binding.xrayConfig.text.toString())
        finish()
    }
}
