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
        binding.useBepass.isChecked = Settings.useBepass
        binding.socksAddress.setText(Settings.socksAddress)
        binding.socksPort.setText(Settings.socksPort)
        binding.primaryDns.setText(Settings.primaryDns)
        binding.secondaryDns.setText(Settings.secondaryDns)
        binding.useXray.isChecked = Settings.useXray
        binding.xrayConfig.setText(getXrayConfig())
        binding.bepassConfig.setText(getBepassConfig())
        binding.saveSettings.setOnClickListener {
            saveSettings()
        }

        binding.useBepass.setOnCheckedChangeListener { _, _ ->
            toggleSettings()
        }

        toggleSettings()
    }

    private fun toggleSettings() {
        val checked = binding.useBepass.isChecked
        if (checked) {
            binding.bepassSettings.visibility = View.VISIBLE
            binding.xraySettings.visibility = View.GONE
        } else {
            binding.xraySettings.visibility = View.VISIBLE
            binding.bepassSettings.visibility = View.GONE
        }
    }

    private fun getXrayConfig(): String {
        val configFile = Settings.xrayConfig(applicationContext)
        if (!configFile.exists() || !configFile.isFile) return ""
        val bufferedReader = configFile.bufferedReader()
        return bufferedReader.use { it.readText() }
    }

    private fun getBepassConfig(): String {
        val configFile = Settings.bepassConfig(applicationContext)
        if (!configFile.exists() || !configFile.isFile) return ""
        val bufferedReader = configFile.bufferedReader()
        return bufferedReader.use { it.readText() }
    }

    private fun saveSettings() {
        Settings.useBepass = binding.useBepass.isChecked
        Settings.socksAddress = binding.socksAddress.text.toString()
        Settings.socksPort = binding.socksPort.text.toString()
        Settings.primaryDns = binding.primaryDns.text.toString()
        Settings.secondaryDns = binding.secondaryDns.text.toString()
        Settings.useXray = binding.useXray.isChecked
        val sharedPref = Settings.sharedPref(applicationContext)
        sharedPref.edit()
            .putBoolean("useBepass", Settings.useBepass)
            .putString("socksAddress", Settings.socksAddress)
            .putString("socksPort", Settings.socksPort)
            .putString("primaryDns", Settings.primaryDns)
            .putString("secondaryDns", Settings.secondaryDns)
            .putBoolean("useXray", Settings.useXray)
            .apply()
        Settings.xrayConfig(applicationContext).writeText(binding.xrayConfig.text.toString())
        Settings.bepassConfig(applicationContext).writeText(binding.bepassConfig.text.toString())
        finish()
    }
}
