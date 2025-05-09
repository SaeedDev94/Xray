package io.github.saeeddev94.xray.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.saeeddev94.xray.R
import io.github.saeeddev94.xray.database.Profile
import io.github.saeeddev94.xray.databinding.ActivityProfileBinding
import android.net.Uri
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.blacksquircle.ui.editorkit.plugin.autoindent.autoIndentation
import com.blacksquircle.ui.editorkit.plugin.base.PluginSupplier
import com.blacksquircle.ui.editorkit.plugin.delimiters.highlightDelimiters
import com.blacksquircle.ui.editorkit.plugin.linenumbers.lineNumbers
import com.blacksquircle.ui.language.json.JsonLanguage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.saeeddev94.xray.helper.ConfigHelper
import io.github.saeeddev94.xray.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class ProfileActivity : AppCompatActivity() {

    private val profileViewModel: ProfileViewModel by viewModels()
    private lateinit var binding: ActivityProfileBinding
    private lateinit var profile: Profile
    private var id: Long = 0L
    private var index: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        id = intent.getLongExtra("id", 0L)
        index = intent.getIntExtra("index", -1)
        title = if (isNew()) getString(R.string.newProfile) else getString(R.string.editProfile)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val jsonUri = intent.data
        if (Intent.ACTION_VIEW == intent.action && jsonUri != null) {
            val profile = Profile()
            profile.config = readJsonFile(jsonUri)
            resolved(profile)
        } else if (isNew()) {
            val profile = Profile()
            profile.name = intent.getStringExtra("name") ?: ""
            profile.config = intent.getStringExtra("config") ?: ""
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
            val error = ConfigHelper.isValid(applicationContext, profile.config)
            if (check && error.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    showError(error)
                }
                return@launch
            }
            if (profile.id == 0L) {
                profile.id = profileViewModel.insert(profile)
                profileViewModel.fixInsertIndex()
            } else {
                profileViewModel.update(profile)
            }
            withContext(Dispatchers.Main) {
                Intent().also {
                    it.putExtra("index", index)
                    it.putExtra("profile", profile)
                    setResult(RESULT_OK, it)
                    finish()
                }
            }
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
