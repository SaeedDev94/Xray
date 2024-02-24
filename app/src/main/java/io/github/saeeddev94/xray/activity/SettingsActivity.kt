package io.github.saeeddev94.xray.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import com.google.android.material.materialswitch.MaterialSwitch
import io.github.saeeddev94.xray.R
import io.github.saeeddev94.xray.adapter.SettingAdapter
import io.github.saeeddev94.xray.Settings
import io.github.saeeddev94.xray.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var adapter: SettingAdapter
    private lateinit var basic: View
    private lateinit var advanced: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.settings)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val tabs = listOf("Basic", "Advanced")
        val layouts = listOf(R.layout.tab_basic_settings, R.layout.tab_advanced_settings)
        adapter = SettingAdapter(this, tabs, layouts, object : SettingAdapter.ViewsReady {
            override fun rendered(views: List<View>) {
                basic = views[0]
                advanced = views[1]
                setupBasic()
                setupAdvanced()
            }
        })
        binding.viewPager.adapter = adapter
        binding.tabLayout.setupWithViewPager(binding.viewPager)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_settings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.saveSettings -> saveSettings()
            else -> finish()
        }
        return true
    }

    private fun setupBasic() {
        basic.findViewById<EditText>(R.id.socksAddress).setText(Settings.socksAddress)
        basic.findViewById<EditText>(R.id.socksPort).setText(Settings.socksPort)
        basic.findViewById<EditText>(R.id.socksUsername).setText(Settings.socksUsername)
        basic.findViewById<EditText>(R.id.socksPassword).setText(Settings.socksPassword)
        basic.findViewById<EditText>(R.id.geoIpAddress).setText(Settings.geoIpAddress)
        basic.findViewById<EditText>(R.id.geoSiteAddress).setText(Settings.geoSiteAddress)
        basic.findViewById<EditText>(R.id.pingAddress).setText(Settings.pingAddress)
        basic.findViewById<EditText>(R.id.pingTimeout).setText(Settings.pingTimeout.toString())
        basic.findViewById<MaterialSwitch>(R.id.bypassLan).isChecked = Settings.bypassLan
        basic.findViewById<MaterialSwitch>(R.id.enableIpV6).isChecked = Settings.enableIpV6
        basic.findViewById<MaterialSwitch>(R.id.socksUdp).isChecked = Settings.socksUdp
    }

    private fun setupAdvanced() {
        advanced.findViewById<EditText>(R.id.primaryDns).setText(Settings.primaryDns)
        advanced.findViewById<EditText>(R.id.secondaryDns).setText(Settings.secondaryDns)
        advanced.findViewById<EditText>(R.id.primaryDnsV6).setText(Settings.primaryDnsV6)
        advanced.findViewById<EditText>(R.id.secondaryDnsV6).setText(Settings.secondaryDnsV6)
        advanced.findViewById<EditText>(R.id.tunName).setText(Settings.tunName)
        advanced.findViewById<EditText>(R.id.tunMtu).setText(Settings.tunMtu.toString())
        advanced.findViewById<EditText>(R.id.tunAddress).setText(Settings.tunAddress)
        advanced.findViewById<EditText>(R.id.tunPrefix).setText(Settings.tunPrefix.toString())
        advanced.findViewById<EditText>(R.id.tunAddressV6).setText(Settings.tunAddressV6)
        advanced.findViewById<EditText>(R.id.tunPrefixV6).setText(Settings.tunPrefixV6.toString())
    }

    private fun saveSettings() {
        /** Basic */
        Settings.socksAddress = basic.findViewById<EditText>(R.id.socksAddress).text.toString()
        Settings.socksPort = basic.findViewById<EditText>(R.id.socksPort).text.toString()
        Settings.socksUsername = basic.findViewById<EditText>(R.id.socksUsername).text.toString()
        Settings.socksPassword = basic.findViewById<EditText>(R.id.socksPassword).text.toString()
        Settings.geoIpAddress = basic.findViewById<EditText>(R.id.geoIpAddress).text.toString()
        Settings.geoSiteAddress = basic.findViewById<EditText>(R.id.geoSiteAddress).text.toString()
        Settings.pingAddress = basic.findViewById<EditText>(R.id.pingAddress).text.toString()
        Settings.pingTimeout = basic.findViewById<EditText>(R.id.pingTimeout).text.toString().toInt()
        Settings.bypassLan = basic.findViewById<MaterialSwitch>(R.id.bypassLan).isChecked
        Settings.enableIpV6 = basic.findViewById<MaterialSwitch>(R.id.enableIpV6).isChecked
        Settings.socksUdp = basic.findViewById<MaterialSwitch>(R.id.socksUdp).isChecked

        /** Advanced */
        Settings.primaryDns = advanced.findViewById<EditText>(R.id.primaryDns).text.toString()
        Settings.secondaryDns = advanced.findViewById<EditText>(R.id.secondaryDns).text.toString()
        Settings.primaryDnsV6 = advanced.findViewById<EditText>(R.id.primaryDnsV6).text.toString()
        Settings.secondaryDnsV6 = advanced.findViewById<EditText>(R.id.secondaryDnsV6).text.toString()
        Settings.tunName = advanced.findViewById<EditText>(R.id.tunName).text.toString()
        Settings.tunMtu = advanced.findViewById<EditText>(R.id.tunMtu).text.toString().toInt()
        Settings.tunAddress = advanced.findViewById<EditText>(R.id.tunAddress).text.toString()
        Settings.tunPrefix = advanced.findViewById<EditText>(R.id.tunPrefix).text.toString().toInt()
        Settings.tunAddressV6 = advanced.findViewById<EditText>(R.id.tunAddressV6).text.toString()
        Settings.tunPrefixV6 = advanced.findViewById<EditText>(R.id.tunPrefixV6).text.toString().toInt()

        Settings.save(applicationContext)
        finish()
    }
}
