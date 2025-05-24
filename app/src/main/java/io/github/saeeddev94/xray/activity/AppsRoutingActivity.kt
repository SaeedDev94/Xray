package io.github.saeeddev94.xray.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.saeeddev94.xray.R
import io.github.saeeddev94.xray.Settings
import io.github.saeeddev94.xray.adapter.AppsRoutingAdapter
import io.github.saeeddev94.xray.databinding.ActivityAppsRoutingBinding
import io.github.saeeddev94.xray.dto.AppList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppsRoutingActivity : AppCompatActivity() {

    private val settings by lazy { Settings(applicationContext) }
    private lateinit var binding: ActivityAppsRoutingBinding
    private lateinit var appsList: RecyclerView
    private lateinit var appsRoutingAdapter: AppsRoutingAdapter
    private lateinit var apps: ArrayList<AppList>
    private lateinit var filtered: MutableList<AppList>
    private lateinit var appsRouting: MutableSet<String>
    private lateinit var menu: Menu
    private var appsRoutingMode: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = ""
        binding = ActivityAppsRoutingBinding.inflate(layoutInflater)
        appsRoutingMode = settings.appsRoutingMode
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.search.focusable = View.NOT_FOCUSABLE
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                search(newText)
                return false
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                binding.search.clearFocus()
                search(query)
                return false
            }
        })
        binding.search.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
            ?.setOnClickListener {
                binding.search.setQuery("", false)
                binding.search.clearFocus()
            }

        getApps()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.menu_apps_routing, menu)
        handleMode()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.appsRoutingSave -> saveAppsRouting()
            R.id.appsRoutingExcludeMode -> setMode(false)
            R.id.appsRoutingIncludeMode -> setMode(true)
            else -> finish()
        }
        return true
    }

    private fun setMode(appsRoutingMode: Boolean) {
        this.appsRoutingMode = appsRoutingMode
        handleMode().also { message ->
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleMode(): String {
        val excludeItem = menu.findItem(R.id.appsRoutingExcludeMode)
        val includeItem = menu.findItem(R.id.appsRoutingIncludeMode)
        return when (this.appsRoutingMode) {
            true -> {
                excludeItem.isVisible = true
                includeItem.isVisible = false
                getString(R.string.appsRoutingExcludeMode)
            }
            false -> {
                excludeItem.isVisible = false
                includeItem.isVisible = true
                getString(R.string.appsRoutingIncludeMode)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun search(query: String?) {
        val keyword = query?.trim()?.lowercase() ?: ""
        if (keyword.isEmpty()) {
            if (apps.size > filtered.size) {
                filtered.clear()
                filtered.addAll(apps.toMutableList())
                appsRoutingAdapter.notifyDataSetChanged()
            }
            return
        }
        val list = ArrayList<AppList>()
        apps.forEach {
            if (it.appName.lowercase().contains(keyword) || it.packageName.contains(keyword)) {
                list.add(it)
            }
        }
        filtered.clear()
        filtered.addAll(list.toMutableList())
        appsRoutingAdapter.notifyDataSetChanged()
    }

    private fun getApps() {
        lifecycleScope.launch {
            val selected = ArrayList<AppList>()
            val unselected = ArrayList<AppList>()
            packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS).forEach {
                val permissions = it.requestedPermissions
                if (
                    permissions == null || !permissions.contains(Manifest.permission.INTERNET)
                ) return@forEach
                val appIcon = it.applicationInfo!!.loadIcon(packageManager)
                val appName = it.applicationInfo!!.loadLabel(packageManager).toString()
                val packageName = it.packageName
                val app = AppList(appIcon, appName, packageName)
                val isSelected = settings.appsRouting.contains(packageName)
                if (isSelected) selected.add(app) else unselected.add(app)
            }
            withContext(Dispatchers.Main) {
                apps = ArrayList(selected + unselected)
                filtered = apps.toMutableList()
                appsRouting = settings.appsRouting.split("\n").toMutableSet()
                appsList = binding.appsList
                appsRoutingAdapter = AppsRoutingAdapter(
                    this@AppsRoutingActivity, filtered, appsRouting
                )
                appsList.adapter = appsRoutingAdapter
                appsList.layoutManager = LinearLayoutManager(applicationContext)
            }
        }
    }

    private fun saveAppsRouting() {
        binding.search.clearFocus()
        settings.appsRoutingMode = appsRoutingMode
        settings.appsRouting = appsRouting.joinToString("\n")
        finish()
    }

}
