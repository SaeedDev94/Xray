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
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
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
import io.github.saeeddev94.xray.databinding.ActivityMainBinding
import io.github.saeeddev94.xray.dto.ProfileList
import io.github.saeeddev94.xray.helper.HttpHelper
import io.github.saeeddev94.xray.helper.LinkHelper
import io.github.saeeddev94.xray.helper.ProfileTouchHelper
import io.github.saeeddev94.xray.service.TProxyService
import io.github.saeeddev94.xray.viewmodel.LinkViewModel
import io.github.saeeddev94.xray.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
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
        val id = it.data!!.getLongExtra("id", 0L)
        onProfileActivityResult(id, index)
    }
    private val linkLauncher = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode != RESULT_OK) return@registerForActivityResult
        getProfiles(dataOnly = true)
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

    private fun onProfileActivityResult(id: Long, index: Int) {
        if (index == -1) {
            lifecycleScope.launch {
                val newProfile = profileViewModel.find(id)
                withContext(Dispatchers.Main) {
                    profiles.add(0, ProfileList.fromProfile(newProfile))
                    profileAdapter.notifyItemRangeChanged(0, profiles.size)
                }
            }
            return
        }
        lifecycleScope.launch {
            val profile = profileViewModel.find(id)
            withContext(Dispatchers.Main) {
                profiles[index] = ProfileList.fromProfile(profile)
                profileAdapter.notifyItemChanged(index)
            }
        }
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
            getConfig(uri)
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

    private fun getConfig(uri: URI) {
        val dialogView = LayoutInflater.from(this).inflate(
            R.layout.loading_dialog,
            LinearLayout(this)
        )
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.show()
        lifecycleScope.launch {
            try {
                val content = HttpHelper.get(uri.toString())
                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                    processHttpsContent(uri, content)
                }
            } catch (error: Exception) {
                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                    Toast.makeText(applicationContext, error.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun processHttpsContent(uri: URI, content: String) {
        // Try to add as subscription link (if content is base64)
        if (tryProcessAsSubscription(uri, content)) {
            return
        }
        
        // Try to add as JSON link (if content is JSON array)
        if (tryProcessAsJson(uri, content)) {
            return
        }
        
        // Default handling (process as direct profile)
        try {
            val name = LinkHelper.remark(uri)
            val json = JSONObject(content).toString(2)
            profileLauncher.launch(profileIntent(name = name, config = json))
        } catch (error: JSONException) {
            Toast.makeText(applicationContext, error.message, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun tryProcessAsSubscription(uri: URI, content: String): Boolean {
        return runCatching {
            // Try to decode as base64
            val decoded = LinkHelper.decodeBase64(content.trim())
            
            // Check if it's actually base64 (decoding gives a different result)
            // and contains at least one valid entry
            if (decoded.isNotBlank() && decoded != content.trim()) {
                val isValid = decoded.split("\n")
                    .any { line -> 
                        runCatching { LinkHelper(line.trim()).isValid() }.getOrNull() == true 
                    }
                
                if (isValid) {
                    // Create and save a new subscription link
                    val name = LinkHelper.remark(uri)
                    val link = Link(
                        name = name.ifEmpty { getString(R.string.newLink) },
                        address = uri.toString(),
                        type = Link.Type.Subscription,
                        isActive = true,
                        userAgent = "xray-${BuildConfig.VERSION_NAME}"
                    )
                    
                    val linkViewModel: LinkViewModel by viewModels()
                    linkViewModel.insert(link)
                    
                    Toast.makeText(
                        applicationContext, 
                        "Added as subscription link", 
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    val intent = Intent(applicationContext, LinksActivity::class.java)
                    intent.putExtra("auto_update", true)
                    linkLauncher.launch(intent)
                    return@runCatching true
                }
            }
            false
        }.getOrNull() ?: false
    }
    
    private fun tryProcessAsJson(uri: URI, content: String): Boolean {
        return runCatching {
            // Try to parse as JSON array
            val jsonArray = JSONArray(content)
            
            // Check if it's a valid array with at least one entry
            if (jsonArray.length() > 0) {
                // Verify at least one valid config in the array
                var hasValidConfig = false
                for (i in 0 until jsonArray.length()) {
                    runCatching { 
                        val obj = jsonArray.getJSONObject(i)
                        if (obj.has("outbounds") || obj.has("inbounds")) {
                            hasValidConfig = true
                        }
                    }
                    if (hasValidConfig) break
                }
                
                if (hasValidConfig) {
                    // Create and save a new JSON link
                    val name = LinkHelper.remark(uri)
                    val link = Link(
                        name = name.ifEmpty { getString(R.string.newLink) },
                        address = uri.toString(),
                        type = Link.Type.Json,
                        isActive = true,
                        userAgent = "xray-${BuildConfig.VERSION_NAME}"
                    )
                    
                    val linkViewModel: LinkViewModel by viewModels()
                    linkViewModel.insert(link)
                    
                    Toast.makeText(
                        applicationContext, 
                        "Added as JSON link", 
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Launch LinksActivity and automatically trigger update
                    val intent = Intent(applicationContext, LinksActivity::class.java)
                    intent.putExtra("auto_update", true)
                    linkLauncher.launch(intent)
                    return@runCatching true
                }
            }
            false
        }.getOrNull() ?: false
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
