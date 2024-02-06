package io.github.saeeddev94.xray

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.github.saeeddev94.xray.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.settings)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.primaryDns.setText(Settings.primaryDns)
        binding.secondaryDns.setText(Settings.secondaryDns)
        binding.tunName.setText(Settings.tunName)
        binding.tunMtu.setText(Settings.tunMtu.toString())
        binding.socksAddress.setText(Settings.socksAddress)
        binding.socksPort.setText(Settings.socksPort)
        binding.socksUsername.setText(Settings.socksUsername)
        binding.socksPassword.setText(Settings.socksPassword)
        binding.pingAddress.setText(Settings.pingAddress)
        binding.excludedApps.setText(Settings.excludedApps)
        binding.bypassLan.isChecked = Settings.bypassLan
        binding.socksUdp.isChecked = Settings.socksUdp
        binding.saveSettings.setOnClickListener {
            saveSettings()
        }
    }

    private fun saveSettings() {
        Settings.primaryDns = binding.primaryDns.text.toString()
        Settings.secondaryDns = binding.secondaryDns.text.toString()
        Settings.tunName = binding.tunName.text.toString()
        Settings.tunMtu = binding.tunMtu.text.toString().toInt()
        Settings.socksAddress = binding.socksAddress.text.toString()
        Settings.socksPort = binding.socksPort.text.toString()
        Settings.socksUsername = binding.socksUsername.text.toString()
        Settings.socksPassword = binding.socksPassword.text.toString()
        Settings.pingAddress = binding.pingAddress.text.toString()
        Settings.excludedApps = binding.excludedApps.text.toString()
        Settings.bypassLan = binding.bypassLan.isChecked
        Settings.socksUdp = binding.socksUdp.isChecked
        val sharedPref = Settings.sharedPref(applicationContext)
        sharedPref.edit()
            .putString("primaryDns", Settings.primaryDns)
            .putString("secondaryDns", Settings.secondaryDns)
            .putString("tunName", Settings.tunName)
            .putInt("tunMtu", Settings.tunMtu)
            .putString("socksAddress", Settings.socksAddress)
            .putString("socksPort", Settings.socksPort)
            .putString("socksUsername", Settings.socksUsername)
            .putString("socksPassword", Settings.socksPassword)
            .putString("pingAddress", Settings.pingAddress)
            .putString("excludedApps", Settings.excludedApps)
            .putBoolean("bypassLan", Settings.bypassLan)
            .putBoolean("socksUdp", Settings.socksUdp)
            .apply()
        finish()
    }
}
