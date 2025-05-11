package io.github.saeeddev94.xray.activity

import XrayCore.XrayCore
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
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
import io.github.saeeddev94.xray.databinding.ActivityMainBinding
import io.github.saeeddev94.xray.dto.ProfileList
import io.github.saeeddev94.xray.helper.HttpHelper
import io.github.saeeddev94.xray.helper.LinkHelper
import io.github.saeeddev94.xray.helper.ProfileTouchHelper
import io.github.saeeddev94.xray.service.TProxyService
import io.github.saeeddev94.xray.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URI
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.tabs.TabLayout
import io.github.saeeddev94.xray.viewmodel.LinkViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val clipboardManager by lazy { getSystemService(ClipboardManager::class.java) }
    private val linkViewModel: LinkViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private var isRunning: Boolean = false

    private lateinit var binding: ActivityMainBinding
    private lateinit var profileAdapter: ProfileAdapter
    private val profilesRecyclerView by lazy { findViewById<RecyclerView>(R.id.profilesRecyclerView) }

    private lateinit var list: List<ProfileList>
    private val profiles = arrayListOf<ProfileList>()

    private val notificationPermission = registerForActivityResult(RequestPermission()) {
        onToggleButtonClick()
    }
    private val linksManager = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode != RESULT_OK) return@registerForActivityResult
        refreshLinks()
    }
    private val vpnLauncher = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode != RESULT_OK) return@registerForActivityResult
        toggleVpnService()
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

    private val linksTabListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab?) {
            if (tab == null) return
            linkViewModel.activeTab = tab.tag.toString().toLong()
            profileViewModel.next(linkViewModel.activeTab)
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {
        }

        override fun onTabReselected(tab: TabLayout.Tab?) {
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
        profileAdapter = ProfileAdapter(
            lifecycleScope,
            profileViewModel,
            profiles,
            { index, profile -> profileSelect(index, profile) },
            { profile -> profileEdit(profile) },
            { profile -> profileDelete(profile) },
        )
        profilesRecyclerView.adapter = profileAdapter
        profilesRecyclerView.layoutManager = LinearLayoutManager(applicationContext)
        ItemTouchHelper(ProfileTouchHelper(profileAdapter)).also {
            it.attachToRecyclerView(profilesRecyclerView)
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                linkViewModel.tabs.collectLatest { onNewTabs(it) }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.filtered.collectLatest { onNewProfiles(it) }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.profiles.collectLatest { onNewList(it) }
            }
        }
        intent?.data?.let { deepLink ->
            val pathSegments = deepLink.pathSegments
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
            R.id.newProfile -> startActivity(ProfileActivity.getIntent(applicationContext))
            R.id.fromClipboard -> {
                runCatching {
                    clipboardManager.primaryClip!!.getItemAt(0).text.toString().trim()
                }.getOrNull()?.let { processLink(it) }
            }
        }
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.assets -> Intent(applicationContext, AssetsActivity::class.java)
            R.id.links -> Intent(applicationContext, LinksActivity::class.java)
            R.id.logs -> Intent(applicationContext, LogsActivity::class.java)
            R.id.appsRouting -> Intent(applicationContext, AppsRoutingActivity::class.java)
            R.id.settings -> Intent(applicationContext, SettingsActivity::class.java)
            else -> null
        }?.let { startActivity(it) }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun onNewTabs(value: List<Link>) {
        val isEmpty = value.isEmpty()
        binding.linksTab.removeOnTabSelectedListener(linksTabListener)
        binding.linksTab.removeAllTabs()
        binding.linksTab.isVisible = !isEmpty
        if (isEmpty) return
        val list = value.toMutableList()
        list.add(0, Link(name = "All"))
        list.forEach {
            val tab = binding.linksTab.newTab()
            tab.tag = it.id
            tab.text = it.name
            binding.linksTab.addTab(tab)
        }
        var selected = list.indexOfFirst { it.id == linkViewModel.activeTab }
        if (selected == -1) {
            selected = 0
            linkViewModel.activeTab = 0L
        }
        binding.linksTab.selectTab(binding.linksTab.getTabAt(selected))
        binding.linksTab.addOnTabSelectedListener(linksTabListener)
    }

    private fun onNewProfiles(value: List<ProfileList>) {
        profiles.clear()
        profiles.addAll(ArrayList(value))
        @Suppress("NotifyDataSetChanged")
        profileAdapter.notifyDataSetChanged()
    }

    private fun onNewList(value: List<ProfileList>) {
        list = value
        profileViewModel.next(linkViewModel.activeTab)
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
                if (ref == null || ref.id == profile.id) return@withContext
                profiles.indexOfFirst { it.id == ref.id }.let {
                    if (it != -1) profileAdapter.notifyItemChanged(it)
                }
            }
        }
    }

    private fun profileEdit(profile: ProfileList) {
        if (isRunning && Settings.selectedProfile == profile.id) return
        startActivity(ProfileActivity.getIntent(applicationContext, profile.id))
    }

    private fun profileDelete(profile: ProfileList) {
        if (isRunning && Settings.selectedProfile == profile.id) return
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Profile#${profile.index + 1} ?")
            .setMessage("\"${profile.name}\" will delete forever !!")
            .setNegativeButton("No", null)
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch {
                    val ref = profileViewModel.find(profile.id)
                    val id = ref.id
                    profileViewModel.remove(ref)
                    withContext(Dispatchers.Main) {
                        val selectedProfile = Settings.selectedProfile
                        if (selectedProfile == id) {
                            Settings.selectedProfile = 0L
                            Settings.save(applicationContext)
                        }
                    }
                }
            }.show()
    }

    private fun processLink(link: String) {
        val uri = runCatching { URI(link) }.getOrNull() ?: return
        if (uri.scheme == "http") {
            Toast.makeText(applicationContext, getString(R.string.forbiddenHttp), Toast.LENGTH_SHORT).show()
            return
        }
        if (uri.scheme == "https") {
            openLink(uri)
            return
        }
        val linkHelper = LinkHelper(link)
        if (!linkHelper.isValid()) {
            Toast.makeText(applicationContext, getString(R.string.invalidLink), Toast.LENGTH_SHORT).show()
            return
        }
        val json = linkHelper.json()
        val name = linkHelper.remark()
        startActivity(ProfileActivity.getIntent(applicationContext, name = name, config = json))
    }

    private fun refreshLinks() {
        startActivity(LinksManagerActivity.refreshLinks(applicationContext))
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
        HttpHelper(lifecycleScope).measureDelay { binding.pingResult.text = it }
    }

    private fun hasPostNotification(): Boolean {
        val sharedPref = getSharedPreferences("app", Context.MODE_PRIVATE)
        val key = "request_notification_permission"
        val askedBefore = sharedPref.getBoolean(key, false)
        if (askedBefore) return true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            sharedPref.edit { putBoolean(key, true) }
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            return false
        }
        return true
    }
}
