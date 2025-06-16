package io.github.saeeddev94.xray.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch
import io.github.saeeddev94.xray.R
import io.github.saeeddev94.xray.Settings
import io.github.saeeddev94.xray.adapter.SettingAdapter
import io.github.saeeddev94.xray.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private val settings by lazy { Settings(applicationContext) }
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

    @SuppressLint("SetTextI18n")
    private fun setupBasic() {
        basic.findViewById<EditText>(R.id.socksAddress).setText(settings.socksAddress)
        basic.findViewById<EditText>(R.id.socksPort).setText(settings.socksPort)
        basic.findViewById<EditText>(R.id.socksUsername).setText(settings.socksUsername)
        basic.findViewById<EditText>(R.id.socksPassword).setText(settings.socksPassword)
        basic.findViewById<EditText>(R.id.geoIpAddress).setText(settings.geoIpAddress)
        basic.findViewById<EditText>(R.id.geoSiteAddress).setText(settings.geoSiteAddress)
        basic.findViewById<EditText>(R.id.pingAddress).setText(settings.pingAddress)
        basic.findViewById<EditText>(R.id.pingTimeout).setText(settings.pingTimeout.toString())
        basic.findViewById<EditText>(R.id.refreshLinksInterval)
            .setText(settings.refreshLinksInterval.toString())
        basic.findViewById<MaterialSwitch>(R.id.bypassLan).isChecked = settings.bypassLan
        basic.findViewById<MaterialSwitch>(R.id.enableIpV6).isChecked = settings.enableIpV6
        basic.findViewById<MaterialSwitch>(R.id.socksUdp).isChecked = settings.socksUdp
        basic.findViewById<MaterialSwitch>(R.id.bootAutoStart).isChecked = settings.bootAutoStart
        basic.findViewById<MaterialSwitch>(R.id.refreshLinksOnOpen).isChecked =
            settings.refreshLinksOnOpen
    }

    @SuppressLint("SetTextI18n")
    private fun setupAdvanced() {
        advanced.findViewById<EditText>(R.id.primaryDns).setText(settings.primaryDns)
        advanced.findViewById<EditText>(R.id.secondaryDns).setText(settings.secondaryDns)
        advanced.findViewById<EditText>(R.id.primaryDnsV6).setText(settings.primaryDnsV6)
        advanced.findViewById<EditText>(R.id.secondaryDnsV6).setText(settings.secondaryDnsV6)
        advanced.findViewById<EditText>(R.id.tunName).setText(settings.tunName)
        advanced.findViewById<EditText>(R.id.tunMtu).setText(settings.tunMtu.toString())
        advanced.findViewById<EditText>(R.id.tunAddress).setText(settings.tunAddress)
        advanced.findViewById<EditText>(R.id.tunPrefix).setText(settings.tunPrefix.toString())
        advanced.findViewById<EditText>(R.id.tunAddressV6).setText(settings.tunAddressV6)
        advanced.findViewById<EditText>(R.id.tunPrefixV6).setText(settings.tunPrefixV6.toString())
    }

    private fun saveSettings() {
        /** Basic */
        settings.socksAddress = basic.findViewById<EditText>(R.id.socksAddress).text.toString()
        settings.socksPort = basic.findViewById<EditText>(R.id.socksPort).text.toString()
        settings.socksUsername = basic.findViewById<EditText>(R.id.socksUsername).text.toString()
        settings.socksPassword = basic.findViewById<EditText>(R.id.socksPassword).text.toString()
        settings.geoIpAddress = basic.findViewById<EditText>(R.id.geoIpAddress).text.toString()
        settings.geoSiteAddress = basic.findViewById<EditText>(R.id.geoSiteAddress).text.toString()
        settings.pingAddress = basic.findViewById<EditText>(R.id.pingAddress).text.toString()
        settings.pingTimeout = basic.findViewById<EditText>(R.id.pingTimeout).text.toString().toInt()
        settings.refreshLinksInterval =
            basic.findViewById<EditText>(R.id.refreshLinksInterval).text.toString().toInt()
        settings.bypassLan = basic.findViewById<MaterialSwitch>(R.id.bypassLan).isChecked
        settings.enableIpV6 = basic.findViewById<MaterialSwitch>(R.id.enableIpV6).isChecked
        settings.socksUdp = basic.findViewById<MaterialSwitch>(R.id.socksUdp).isChecked
        settings.bootAutoStart = basic.findViewById<MaterialSwitch>(R.id.bootAutoStart).isChecked
        settings.refreshLinksOnOpen =
            basic.findViewById<MaterialSwitch>(R.id.refreshLinksOnOpen).isChecked

        /** Advanced */
        settings.primaryDns = advanced.findViewById<EditText>(R.id.primaryDns).text.toString()
        settings.secondaryDns = advanced.findViewById<EditText>(R.id.secondaryDns).text.toString()
        settings.primaryDnsV6 = advanced.findViewById<EditText>(R.id.primaryDnsV6).text.toString()
        settings.secondaryDnsV6 = advanced.findViewById<EditText>(R.id.secondaryDnsV6).text.toString()
        settings.tunName = advanced.findViewById<EditText>(R.id.tunName).text.toString()
        settings.tunMtu = advanced.findViewById<EditText>(R.id.tunMtu).text.toString().toInt()
        settings.tunAddress = advanced.findViewById<EditText>(R.id.tunAddress).text.toString()
        settings.tunPrefix = advanced.findViewById<EditText>(R.id.tunPrefix).text.toString().toInt()
        settings.tunAddressV6 = advanced.findViewById<EditText>(R.id.tunAddressV6).text.toString()
        settings.tunPrefixV6 = advanced.findViewById<EditText>(R.id.tunPrefixV6).text.toString().toInt()

        finish()
    }
}
