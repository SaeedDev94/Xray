package io.github.saeeddev94.xray.activity

import XrayCore.XrayCore
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import io.github.saeeddev94.xray.BuildConfig
import io.github.saeeddev94.xray.R
import io.github.saeeddev94.xray.Settings
import io.github.saeeddev94.xray.adapter.ProfileAdapter
import io.github.saeeddev94.xray.database.Link
import io.github.saeeddev94.xray.database.Profile
import io.github.saeeddev94.xray.databinding.ActivityMainBinding
import io.github.saeeddev94.xray.dto.ProfileList
import io.github.saeeddev94.xray.helper.HttpHelper
import io.github.saeeddev94.xray.helper.IntentHelper
import io.github.saeeddev94.xray.helper.LinkHelper
import io.github.saeeddev94.xray.helper.ProfileTouchHelper
import io.github.saeeddev94.xray.service.TProxyService
import io.github.saeeddev94.xray.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URI

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val clipboardManager by lazy { getSystemService(ClipboardManager::class.java) }
    private val profileViewModel: ProfileViewModel by viewModels()
    private var isRunning: Boolean = false

    private lateinit var binding: ActivityMainBinding
    private lateinit var profilesList: RecyclerView
    private lateinit var profileAdapter: ProfileAdapter
    private lateinit var profiles: ArrayList<ProfileList>

    private val profileLauncher = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode != RESULT_OK || it.data == null) return@registerForActivityResult
        val index = it.data!!.getIntExtra("index", -1)
        val profile = IntentHelper.getParcelable(it.data!!, "profile", Profile::class.java)
        onProfileActivityResult(index, profile!!)
    }
    private val linkLauncher = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode != RESULT_OK) return@registerForActivityResult
        getProfiles(dataOnly = true)
    }
    private val linksManager = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode != RESULT_OK || it.data == null) return@registerForActivityResult
        val link: Link? = LinksManagerActivity.getLink(it.data!!)
        val refresh = LinksManagerActivity.getRefresh(it.data!!)
        if (link != null) refreshLinks()
        if (refresh) getProfiles(dataOnly = true)
    }
    private val vpnLauncher = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode != RESULT_OK) return@registerForActivityResult
        toggleVpnService()
    }
    private val vpnServiceNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            onToggleButtonClick()
        }
    private val vpnServiceEventReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context == null || intent == null) return
            when (intent.action) {
                TProxyService.START_VPN_SERVICE_ACTION_NAME -> vpnStartStatus()
                TProxyService.STOP_VPN_SERVICE_ACTION_NAME -> vpnStopStatus()
                TProxyService.STATUS_VPN_SERVICE_ACTION_NAME -> {
                    intent.getBooleanExtra("isRunning", false).let { isRunning ->
                        if (isRunning) vpnStartStatus()
                        else vpnStopStatus()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        Settings.sync(applicationContext)
        binding.toggleButton.setOnClickListener { onToggleButtonClick() }
        binding.pingBox.setOnClickListener { ping() }
        binding.navView.menu.findItem(R.id.appVersion).title = BuildConfig.VERSION_NAME
        binding.navView.menu.findItem(R.id.xrayVersion).title = XrayCore.version()
        binding.navView.setNavigationItemSelectedListener(this)
        ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.drawerOpen, R.string.drawerClose
        ).also {
            binding.drawerLayout.addDrawerListener(it)
            it.syncState()
        }
        getProfiles()
        val deepLink: Uri? = intent?.data
        deepLink?.let {
            val pathSegments = it.pathSegments
            if (pathSegments.size > 0) processLink(pathSegments[0])
        }
    }

    override fun onStart() {
        super.onStart()
        IntentFilter().also {
            it.addAction(TProxyService.START_VPN_SERVICE_ACTION_NAME)
            it.addAction(TProxyService.STOP_VPN_SERVICE_ACTION_NAME)
            it.addAction(TProxyService.STATUS_VPN_SERVICE_ACTION_NAME)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(vpnServiceEventReceiver, it, RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(vpnServiceEventReceiver, it)
            }
        }
        Intent(this, TProxyService::class.java).also {
            it.action = TProxyService.STATUS_VPN_SERVICE_ACTION_NAME
            startService(it)
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(vpnServiceEventReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.refreshLinks -> refreshLinks()
            R.id.newProfile -> {
                profileLauncher.launch(profileIntent())
            }
            R.id.fromClipboard -> {
                val clipData: ClipData? = clipboardManager.primaryClip
                val clipText: String = if (clipData != null && clipData.itemCount > 0) {
                    clipData.getItemAt(0).text.toString().trim()
                } else ""
                processLink(clipText)
            }
        }
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.assets -> Intent(applicationContext, AssetsActivity::class.java)
            R.id.logs -> Intent(applicationContext, LogsActivity::class.java)
            R.id.appsRouting -> Intent(applicationContext, AppsRoutingActivity::class.java)
            R.id.settings -> Intent(applicationContext, SettingsActivity::class.java)
            else -> null
        }.also {
            if (it != null) startActivity(it)
        }
        if (item.itemId == R.id.links) {
            linkLauncher.launch(Intent(applicationContext, LinksActivity::class.java))
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun vpnStartStatus() {
        isRunning = true
        binding.toggleButton.text = getString(R.string.vpnStop)
        binding.toggleButton.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.primaryColor)
        )
        binding.pingResult.text = getString(R.string.pingConnected)
    }

    private fun vpnStopStatus() {
        isRunning = false
        binding.toggleButton.text = getString(R.string.vpnStart)
        binding.toggleButton.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.btnColor)
        )
        binding.pingResult.text = getString(R.string.pingNotConnected)
    }

    private fun onToggleButtonClick() {
        if (!hasPostNotification()) return
        VpnService.prepare(this).also {
            if (it == null) {
                toggleVpnService()
                return
            }
            vpnLauncher.launch(it)
        }
    }

    private fun toggleVpnService() {
        if (isRunning) {
            TProxyService.stop(applicationContext)
            return
        }
        TProxyService.start(applicationContext, false)
    }

    private fun profileSelect(index: Int, profile: ProfileList) {
        if (isRunning) return
        val selectedProfile = Settings.selectedProfile
        lifecycleScope.launch {
            val ref = if (selectedProfile > 0L) profileViewModel.find(selectedProfile) else null
            withContext(Dispatchers.Main) {
                Settings.selectedProfile = if (selectedProfile == profile.id) 0L else profile.id
                Settings.save(applicationContext)
                profileAdapter.notifyItemChanged(index)
                if (ref != null && ref.index != index) profileAdapter.notifyItemChanged(ref.index)
            }
        }
    }

    private fun profileEdit(index: Int, profile: ProfileList) {
        if (isRunning && Settings.selectedProfile == profile.id) return
        profileLauncher.launch(profileIntent(index, profile.id))
    }

    private fun profileDelete(index: Int, profile: ProfileList) {
        if (isRunning && Settings.selectedProfile == profile.id) return
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Profile#${profile.index + 1} ?")
            .setMessage("\"${profile.name}\" will delete forever !!")
            .setNegativeButton("No", null)
            .setPositiveButton("Yes") { dialog, _ ->
                dialog?.dismiss()
                lifecycleScope.launch {
                    val ref = profileViewModel.find(profile.id)
                    val id = ref.id
                    profileViewModel.delete(ref)
                    profileViewModel.fixDeleteIndex(index)
                    withContext(Dispatchers.Main) {
                        val selectedProfile = Settings.selectedProfile
                        if (selectedProfile == id) {
                            Settings.selectedProfile = 0L
                            Settings.save(applicationContext)
                        }
                        profiles.removeAt(index)
                        profileAdapter.notifyItemRemoved(index)
                        profileAdapter.notifyItemRangeChanged(index, profiles.size - index)
                    }
                }
            }.show()
    }

    private fun profileIntent(
        index: Int = -1, id: Long = 0L, name: String = "", config: String = ""
    ): Intent {
        return Intent(applicationContext, ProfileActivity::class.java).also {
            it.putExtra("index", index)
            it.putExtra("id", id)
            if (name.isNotEmpty()) it.putExtra("name", name)
            if (config.isNotEmpty()) it.putExtra(
                "config",
                config.replace("\\/", "/")
            )
        }
    }

    private fun onProfileActivityResult(index: Int, profile: Profile) {
        if (index == -1) {
            profiles.add(0, ProfileList.fromProfile(profile))
            profileAdapter.notifyItemRangeChanged(0, profiles.size)
            return
        }
        profiles[index] = ProfileList.fromProfile(profile)
        profileAdapter.notifyItemChanged(index)
    }

    private fun getProfiles(dataOnly: Boolean = false) {
        lifecycleScope.launch {
            val list = profileViewModel.all()
            withContext(Dispatchers.Main) {
                if (dataOnly) {
                    profiles.clear()
                    profiles.addAll(ArrayList(list))
                    @Suppress("NotifyDataSetChanged")
                    profileAdapter.notifyDataSetChanged()
                    return@withContext
                }
                profiles = ArrayList(list)
                profilesList = binding.profilesList
                profileAdapter = ProfileAdapter(
                    lifecycleScope,
                    profileViewModel,
                    profiles, object : ProfileAdapter.ProfileClickListener {
                        override fun profileSelect(index: Int, profile: ProfileList) =
                            this@MainActivity.profileSelect(index, profile)
                        override fun profileEdit(index: Int, profile: ProfileList) =
                            this@MainActivity.profileEdit(index, profile)
                        override fun profileDelete(index: Int, profile: ProfileList) =
                            this@MainActivity.profileDelete(index, profile)
                    })
                profilesList.adapter = profileAdapter
                profilesList.layoutManager = LinearLayoutManager(applicationContext)
                ItemTouchHelper(ProfileTouchHelper(profileAdapter)).also {
                    it.attachToRecyclerView(profilesList)
                }
            }
        }
    }

    private fun processLink(link: String) {
        val uri = runCatching { URI(link) }.getOrNull()
        val invalidLink = getString(R.string.invalidLink)
        val forbiddenHttp = getString(R.string.forbiddenHttp)
        if (uri == null) {
            Toast.makeText(applicationContext, invalidLink, Toast.LENGTH_SHORT).show()
            return
        }
        if (uri.scheme == "http") {
            Toast.makeText(applicationContext, forbiddenHttp, Toast.LENGTH_SHORT).show()
            return
        }
        if (uri.scheme == "https") {
            openLink(uri)
            return
        }
        val linkHelper = LinkHelper(link)
        if (!linkHelper.isValid()) {
            Toast.makeText(applicationContext, invalidLink, Toast.LENGTH_SHORT).show()
            return
        }
        val json = linkHelper.json()
        val name = linkHelper.remark()
        profileLauncher.launch(profileIntent(name = name, config = json))
    }

    private fun refreshLinks() {
        val intent = LinksManagerActivity.refreshLinks(applicationContext)
        linksManager.launch(intent)
    }

    private fun openLink(uri: URI) {
        val link = Link()
        link.name = LinkHelper.remark(uri, LinkHelper.LINK_DEFAULT)
        link.address = uri.toString()
        val intent = LinksManagerActivity.openLink(applicationContext, link)
        linksManager.launch(intent)
    }

    private fun ping() {
        if (!isRunning) return
        binding.pingResult.text = getString(R.string.pingTesting)
        HttpHelper(lifecycleScope).measureDelay { delay ->
            binding.pingResult.text = delay
        }
    }

    private fun hasPostNotification(): Boolean {
        val sharedPref = getSharedPreferences("app", Context.MODE_PRIVATE)
        val key = "request_notification_permission"
        val askedBefore = sharedPref.getBoolean(key, false)
        if (askedBefore) return true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            sharedPref.edit().putBoolean(key, true).apply()
            vpnServiceNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            return false
        }
        return true
    }
}
