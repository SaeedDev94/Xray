package io.github.saeeddev94.xray.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.saeeddev94.xray.R
import io.github.saeeddev94.xray.Settings
import io.github.saeeddev94.xray.adapter.ExcludeAdapter
import io.github.saeeddev94.xray.databinding.ActivityExcludeBinding
import io.github.saeeddev94.xray.dto.AppList

class ExcludeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExcludeBinding
    private lateinit var appsList: RecyclerView
    private lateinit var excludeAdapter: ExcludeAdapter
    private lateinit var apps: ArrayList<AppList>
    private lateinit var filtered: MutableList<AppList>
    private lateinit var excludedApps: MutableSet<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = ""
        binding = ActivityExcludeBinding.inflate(layoutInflater)
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
        menuInflater.inflate(R.menu.menu_exclude, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.saveExcludedApps -> saveExcludedApps()
            else -> finish()
        }
        return true
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun search(query: String?) {
        val keyword = query?.trim()?.lowercase() ?: ""
        if (keyword.isEmpty()) {
            if (apps.size > filtered.size) {
                filtered.clear()
                filtered.addAll(apps.toMutableList())
                excludeAdapter.notifyDataSetChanged()
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
        excludeAdapter.notifyDataSetChanged()
    }

    private fun getApps() {
        Thread {
            val selected = ArrayList<AppList>()
            val unselected = ArrayList<AppList>()
            packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS).forEach {
                val permissions = it.requestedPermissions
                if (permissions == null || !permissions.contains(Manifest.permission.INTERNET)) return@forEach
                val appIcon = it.applicationInfo.loadIcon(packageManager)
                val appName = it.applicationInfo.loadLabel(packageManager).toString()
                val packageName = it.packageName
                val app = AppList(appIcon, appName, packageName)
                val isSelected = Settings.excludedApps.contains(packageName)
                if (isSelected) selected.add(app) else unselected.add(app)
            }
            runOnUiThread {
                apps = ArrayList(selected + unselected)
                filtered = apps.toMutableList()
                excludedApps = Settings.excludedApps.split("\n").toMutableSet()
                appsList = binding.appsList
                excludeAdapter = ExcludeAdapter(this@ExcludeActivity, filtered, excludedApps)
                appsList.adapter = excludeAdapter
                appsList.layoutManager = LinearLayoutManager(applicationContext)
            }
        }.start()
    }

    private fun saveExcludedApps() {
        binding.search.clearFocus()
        Settings.excludedApps = excludedApps.joinToString("\n")
        Settings.save(applicationContext)
        finish()
    }

}
