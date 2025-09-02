package io.github.saeeddev94.xray.activity

import XrayCore.XrayCore
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blacksquircle.ui.editorkit.plugin.autoindent.autoIndentation
import com.blacksquircle.ui.editorkit.plugin.base.PluginSupplier
import com.blacksquircle.ui.editorkit.plugin.delimiters.highlightDelimiters
import com.blacksquircle.ui.editorkit.plugin.linenumbers.lineNumbers
import com.blacksquircle.ui.language.json.JsonLanguage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.saeeddev94.xray.R
import io.github.saeeddev94.xray.Settings
import io.github.saeeddev94.xray.database.Config
import io.github.saeeddev94.xray.database.Profile
import io.github.saeeddev94.xray.databinding.ActivityProfileBinding
import io.github.saeeddev94.xray.helper.ConfigHelper
import io.github.saeeddev94.xray.helper.FileHelper
import io.github.saeeddev94.xray.viewmodel.ConfigViewModel
import io.github.saeeddev94.xray.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class ProfileActivity : AppCompatActivity() {

    companion object {
        private const val PROFILE_ID = "id"
        private const val PROFILE_NAME = "name"
        private const val PROFILE_CONFIG = "config"

        fun getIntent(
            context: Context, id: Long = 0L, name: String = "", config: String = ""
        ) = Intent(context, ProfileActivity::class.java).also {
            it.putExtra(PROFILE_ID, id)
            if (name.isNotEmpty()) it.putExtra(PROFILE_NAME, name)
            if (config.isNotEmpty()) it.putExtra(
                PROFILE_CONFIG,
                config.replace("\\/", "/")
            )
        }
    }

    private val settings by lazy { Settings(applicationContext) }
    private val configViewModel: ConfigViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private lateinit var binding: ActivityProfileBinding
    private lateinit var config: Config
    private lateinit var profile: Profile
    private var id: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        id = intent.getLongExtra(PROFILE_ID, 0L)
        title = if (isNew()) getString(R.string.newProfile) else getString(R.string.editProfile)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        lifecycleScope.launch {
            val config = configViewModel.get()
            withContext(Dispatchers.Main) {
                this@ProfileActivity.config = config
            }
        }

        val jsonUri = intent.data
        if (Intent.ACTION_VIEW == intent.action && jsonUri != null) {
            val profile = Profile()
            profile.config = readJsonFile(jsonUri)
            resolved(profile)
        } else if (isNew()) {
            val profile = Profile()
            profile.name = intent.getStringExtra(PROFILE_NAME) ?: ""
            profile.config = intent.getStringExtra(PROFILE_CONFIG) ?: ""
            resolved(profile)
        } else {
            lifecycleScope.launch {
                val profile = profileViewModel.find(id)
                withContext(Dispatchers.Main) {
                    resolved(profile)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_profile, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.saveProfile -> save()
            else -> finish()
        }
        return true
    }

    private fun isNew() = id == 0L

    private fun readJsonFile(uri: Uri): String {
        val content = StringBuilder()
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).forEachLine { content.append("$it\n") }
            }
        } catch (error: Exception) {
            error.printStackTrace()
        }
        return content.toString()
    }

    private fun resolved(value: Profile) {
        profile = value
        binding.profileName.setText(profile.name)

        val editor = binding.profileConfig
        val pluginSupplier = PluginSupplier.create {
            lineNumbers {
                lineNumbers = true
                highlightCurrentLine = true
            }
            highlightDelimiters()
            autoIndentation {
                autoIndentLines = true
                autoCloseBrackets = true
                autoCloseQuotes = true
            }
        }
        editor.language = JsonLanguage()
        editor.setTextContent(profile.config)
        editor.plugins(pluginSupplier)
    }

    private fun save(check: Boolean = true) {
        profile.name = binding.profileName.text.toString()
        profile.config = binding.profileConfig.text.toString()
        lifecycleScope.launch {
            val configHelper = ConfigHelper(settings, config, profile.config)
            val error = isValid(configHelper.toString())
            if (check && error.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    showError(error)
                }
                return@launch
            }
            if (profile.id == 0L) {
                profileViewModel.create(profile)
            } else {
                profileViewModel.update(profile)
            }
            withContext(Dispatchers.Main) {
                finish()
            }
        }
    }

    private suspend fun isValid(json: String): String {
        return withContext(Dispatchers.IO) {
            val pwd = filesDir.absolutePath
            val testConfig = settings.testConfig()
            FileHelper.createOrUpdate(testConfig, json)
            XrayCore.test(pwd, testConfig.absolutePath)
        }
    }

    private fun showError(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.invalidProfile))
            .setMessage(message)
            .setNegativeButton(getString(R.string.cancel)) { _, _ -> }
            .setPositiveButton(getString(R.string.ignore)) { _, _ -> save(false) }
            .show()
    }

}
