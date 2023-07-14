package com.xtls.xray

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.xtls.xray.databinding.ActivitySettingsBinding
import java.io.File

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
        binding.xrayConfig.setText(getXrayConfig())
        binding.saveSettings.setOnClickListener {
            saveSettings()
        }
    }

    private fun configFile(): File = File(applicationContext.getExternalFilesDir(null), "config.json")

    private fun getXrayConfig(): String {
        val configFile = configFile()
        if (!configFile.exists() || !configFile.isFile) return ""
        val bufferedReader = configFile.bufferedReader()
        return bufferedReader.use { it.readText() }
    }

    private fun saveSettings() {
        Settings.socksAddress = binding.socksAddress.text.toString()
        Settings.socksPort = binding.socksPort.text.toString()
        Settings.primaryDns = binding.primaryDns.text.toString()
        Settings.secondaryDns = binding.secondaryDns.text.toString()
        Settings.useXray = binding.useXray.isChecked
        val sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE)
        sharedPref.edit()
            .putString("socksAddress", Settings.socksAddress)
            .putString("socksPort", Settings.socksPort)
            .putString("primaryDns", Settings.primaryDns)
            .putString("secondaryDns", Settings.secondaryDns)
            .putBoolean("useXray", Settings.useXray)
            .apply()
        configFile().writeText(binding.xrayConfig.text.toString())
        finish()
    }
}
