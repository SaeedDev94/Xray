package io.github.saeeddev94.xray

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import io.github.saeeddev94.xray.database.ProfileList
import io.github.saeeddev94.xray.database.XrayDatabase
import io.github.saeeddev94.xray.databinding.ActivityMainBinding
import libXray.LibXray

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object {
        init {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var vpnService: TProxyService
    private var vpnLauncher = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode != RESULT_OK) return@registerForActivityResult
        toggleVpnService()
    }

    private lateinit var profilesList: RecyclerView
    private lateinit var profileAdapter: ProfileAdapter
    private lateinit var profiles: ArrayList<ProfileList>
    private var profileLauncher = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode != RESULT_OK || it.data == null) return@registerForActivityResult
        val id = it.data!!.getLongExtra("id", 0L)
        val index = it.data!!.getIntExtra("index", -1)
        onProfileActivityResult(id, index)
    }

    private var vpnServiceBound: Boolean = false
    private var serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TProxyService.ServiceBinder
            vpnService = binder.getService()
            vpnServiceBound = true
            setVpnServiceStatus()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            vpnServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setSettings()
        binding.toggleButton.setOnClickListener { onToggleButtonClick() }
        binding.pingBox.setOnClickListener { ping() }
        binding.navView.menu.findItem(R.id.appVersion).title = BuildConfig.VERSION_NAME
        binding.navView.menu.findItem(R.id.xrayVersion).title = LibXray.xrayVersion()
        binding.navView.setNavigationItemSelectedListener(this)
        ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.drawerOpen, R.string.drawerClose).also {
            binding.drawerLayout.addDrawerListener(it)
            it.syncState()
        }
        getProfiles()
    }

    override fun onStart() {
        super.onStart()
        Intent(this, TProxyService::class.java).also {
            bindService(it, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
        vpnServiceBound = false
    }

    override fun onResume() {
        super.onResume()
        setVpnServiceStatus()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 1) onToggleButtonClick()
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.newProfile -> {
                if (!canPerformCrud()) return true
                Intent(applicationContext, ProfileActivity::class.java).also {
                    it.putExtra("id", 0L)
                    it.putExtra("index", -1)
                    profileLauncher.launch(it)
                }
            }
        }
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.appFullName,
            R.id.appVersion,
            R.id.xrayLabel,
            R.id.xrayVersion,
            R.id.tun2socksLabel,
            R.id.tun2socksVersion -> {
                return false
            }
            R.id.settings -> {
                Intent(applicationContext, SettingsActivity::class.java).also {
                    startActivity(it)
                }
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun setSettings() {
        val sharedPref = Settings.sharedPref(applicationContext)
        Settings.primaryDns = sharedPref.getString("primaryDns", Settings.primaryDns)!!
        Settings.secondaryDns = sharedPref.getString("secondaryDns", Settings.secondaryDns)!!
        Settings.socksAddress = sharedPref.getString("socksAddress", Settings.socksAddress)!!
        Settings.socksPort = sharedPref.getString("socksPort", Settings.socksPort)!!
        Settings.socksUsername = sharedPref.getString("socksUsername", Settings.socksUsername)!!
        Settings.socksPassword = sharedPref.getString("socksPassword", Settings.socksPassword)!!
        Settings.excludedApps = sharedPref.getString("excludedApps", Settings.excludedApps)!!
        Settings.bypassLan = sharedPref.getBoolean("bypassLan", Settings.bypassLan)
        Settings.socksUdp = sharedPref.getBoolean("socksUdp", Settings.socksUdp)
        Settings.selectedProfile = sharedPref.getLong("selectedProfile", Settings.selectedProfile)
    }

    private fun setVpnServiceStatus() {
        if (!vpnServiceBound) return
        if (vpnService.getIsRunning()) {
            binding.toggleButton.text = getString(R.string.vpnStop)
            binding.toggleButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.active))
            binding.pingResult.text = getString(R.string.pingConnected)
        } else {
            binding.toggleButton.text = getString(R.string.vpnStart)
            binding.toggleButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.btnColor))
            binding.pingResult.text = getString(R.string.pingNotConnected)
        }
    }

    private fun onToggleButtonClick() {
        if (!vpnServiceBound || !hasPostNotification()) return
        val vpn = VpnService.prepare(this)
        if (vpn != null) {
            vpnLauncher.launch(vpn)
        } else {
            toggleVpnService()
        }
    }

    private fun toggleVpnService() {
        if (vpnService.getIsRunning()) {
            stopVPN()
            return
        }
        val selectedProfile = Settings.selectedProfile
        if (selectedProfile == 0L) {
            startVPN(false)
            return
        }
        Thread {
            val ref = XrayDatabase.ref(applicationContext).profileDao().find(selectedProfile)
            val configFile = Settings.xrayConfig(applicationContext)
            val configContent = if (configFile.exists()) configFile.bufferedReader().use { it.readText() } else ""
            if (ref.config != configContent) {
                configFile.writeText(ref.config)
            }
            runOnUiThread {
                startVPN(true)
            }
        }.start()
    }

    private fun stopVPN() {
        Toast.makeText(applicationContext, "Stop VPN", Toast.LENGTH_SHORT).show()
        vpnService.stopVPN()
        setVpnServiceStatus()
    }

    private fun startVPN(useXray: Boolean) {
        val error = vpnService.startVPN(useXray)
        Toast.makeText(applicationContext, error.ifEmpty { "Start VPN" }, Toast.LENGTH_SHORT).show()
        setVpnServiceStatus()
    }

    private fun profileSelect(index: Int, profile: ProfileList) {
        if (!canPerformCrud()) return
        val sharedPref = Settings.sharedPref(applicationContext)
        val selectedProfile = Settings.selectedProfile
        Thread {
            val ref = if (selectedProfile > 0L) XrayDatabase.ref(applicationContext).profileDao().find(selectedProfile) else null
            runOnUiThread {
                Settings.selectedProfile = if (selectedProfile == profile.id) 0L else profile.id
                sharedPref.edit().putLong("selectedProfile", Settings.selectedProfile).apply()
                profileAdapter.notifyItemChanged(index)
                if (ref != null && ref.index != index) profileAdapter.notifyItemChanged(ref.index)
            }
        }.start()
    }

    private fun profileEdit(index: Int, profile: ProfileList) {
        if (!canPerformCrud()) return
        Intent(applicationContext, ProfileActivity::class.java).also {
            it.putExtra("id", profile.id)
            it.putExtra("index", index)
            profileLauncher.launch(it)
        }
    }

    private fun profileDelete(index: Int, profile: ProfileList) {
        if (!canPerformCrud()) return
        val selectedProfile = Settings.selectedProfile
        if (selectedProfile == profile.id) {
            Toast.makeText(applicationContext, "You can't delete selected profile", Toast.LENGTH_SHORT).show()
            return
        }
        Thread {
            val db = XrayDatabase.ref(applicationContext)
            val ref = db.profileDao().find(profile.id)
            db.profileDao().delete(ref)
            db.profileDao().fixIndex(index)
            runOnUiThread {
                profiles.removeAt(index)
                profileAdapter.notifyItemRemoved(index)
                profileAdapter.notifyItemRangeChanged(index, profiles.size - index)
            }
        }.start()
    }

    private fun onProfileActivityResult(id: Long, index: Int) {
        if (index == -1) {
            Thread {
                val newProfile = XrayDatabase.ref(applicationContext).profileDao().find(id)
                runOnUiThread {
                    profiles.add(0, ProfileList.fromProfile(newProfile))
                    profileAdapter.notifyItemRangeChanged(0, profiles.size)
                }
            }.start()
            return
        }
        Thread {
            val profile = XrayDatabase.ref(applicationContext).profileDao().find(id)
            runOnUiThread {
                profiles[index] = ProfileList.fromProfile(profile)
                profileAdapter.notifyItemChanged(index)
            }
        }.start()
    }

    private fun getProfiles() {
        Thread {
            val list = XrayDatabase.ref(applicationContext).profileDao().all()
            runOnUiThread {
                profiles = ArrayList(list)
                profilesList = binding.profilesList
                profileAdapter = ProfileAdapter(applicationContext, profiles, object : ProfileClickListener {
                    override fun profileSelect(index: Int, profile: ProfileList) = this@MainActivity.profileSelect(index, profile)
                    override fun profileEdit(index: Int, profile: ProfileList) = this@MainActivity.profileEdit(index, profile)
                    override fun profileDelete(index: Int, profile: ProfileList) = this@MainActivity.profileDelete(index, profile)
                })
                profilesList.adapter = profileAdapter
                profilesList.layoutManager = LinearLayoutManager(applicationContext)
            }
        }.start()
    }

    private fun ping() {
        if (!vpnServiceBound || !vpnService.getIsRunning()) return
        binding.pingResult.text = getString(R.string.pingTesting)
        Thread {
            val delay = HttpDelay().measure()
            runOnUiThread {
                binding.pingResult.text = delay
            }
        }.start()
    }

    private fun canPerformCrud(): Boolean {
        if (!vpnServiceBound || vpnService.getIsRunning()) {
            Toast.makeText(applicationContext, "You can't perform CRUD while VpnService is running", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
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
