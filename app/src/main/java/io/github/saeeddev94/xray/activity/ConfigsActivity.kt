package io.github.saeeddev94.xray.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blacksquircle.ui.editorkit.plugin.autoindent.autoIndentation
import com.blacksquircle.ui.editorkit.plugin.base.PluginSupplier
import com.blacksquircle.ui.editorkit.plugin.delimiters.highlightDelimiters
import com.blacksquircle.ui.editorkit.plugin.linenumbers.lineNumbers
import com.blacksquircle.ui.editorkit.widget.TextProcessor
import com.blacksquircle.ui.language.json.JsonLanguage
import com.google.android.material.radiobutton.MaterialRadioButton
import io.github.saeeddev94.xray.R
import io.github.saeeddev94.xray.adapter.ConfigAdapter
import io.github.saeeddev94.xray.database.Config
import io.github.saeeddev94.xray.databinding.ActivityConfigsBinding
import io.github.saeeddev94.xray.viewmodel.ConfigViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.getValue
import kotlin.reflect.cast

class ConfigsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfigsBinding
    private lateinit var config: Config
    private lateinit var adapter: ConfigAdapter
    private val configViewModel: ConfigViewModel by viewModels()
    private val radioGroup = mutableMapOf<String, RadioGroup>()
    private val configEditor = mutableMapOf<String, TextProcessor>()
    private val indentSpaces = 4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.configs)
        binding = ActivityConfigsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        lifecycleScope.launch {
            config = configViewModel.get()
            withContext(Dispatchers.Main) {
                val context = this@ConfigsActivity
                val tabs = listOf("log", "dns", "inbounds", "outbounds", "routing")
                adapter = ConfigAdapter(context, tabs) { tab, view -> setup(tab, view) }
                binding.viewPager.adapter = adapter
                binding.tabLayout.setupWithViewPager(binding.viewPager)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_configs, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.saveConfigs -> saveConfigs()
            else -> finish()
        }
        return true
    }

    private fun setup(tab: String, view: View) {
        val mode = getMode(tab)
        val config = getConfig(tab)

        val modeRadioGroup = view.findViewById<RadioGroup>(R.id.modeRadioGroup)
        modeRadioGroup.removeAllViews()
        Config.Mode.entries.forEach {
            val radio = MaterialRadioButton(this)
            radio.text = it.name
            radio.tag = it
            modeRadioGroup.addView(radio)
            if (it == mode) modeRadioGroup.check(radio.id)
        }
        radioGroup.put(tab, modeRadioGroup)

        val editor = view.findViewById<TextProcessor>(R.id.config)
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
        editor.setTextContent(config)
        editor.plugins(pluginSupplier)
        configEditor.put(tab, editor)
    }

    private fun getConfig(tab: String): String {
        val editor = configEditor[tab]
        val default = ""
        return if (editor == null) {
            when (tab) {
                "log" -> config.log
                "dns" -> config.dns
                "inbounds" -> config.inbounds
                "outbounds" -> config.outbounds
                "routing" -> config.routing
                else -> default
            }
        } else getViewConfig(tab, default)
    }

    private fun getMode(tab: String): Config.Mode {
        val group = configEditor[tab]
        val default = Config.Mode.Disable
        return if (group == null) {
            when (tab) {
                "log" -> config.logMode
                "dns" -> config.dnsMode
                "inbounds" -> config.inboundsMode
                "outbounds" -> config.outboundsMode
                "routing" -> config.routingMode
                else -> default
            }
        } else getViewMode(tab, default)
    }

    private fun getViewConfig(tab: String, default: String): String {
        val editor = configEditor[tab]
        if (editor == null) return default
        return editor.text.toString()
    }

    private fun getViewMode(tab: String, default: Config.Mode): Config.Mode {
        val group = radioGroup[tab]
        if (group == null) return default
        val modeRadioButton = group.findViewById<RadioButton>(
            group.checkedRadioButtonId
        )
        return Config.Mode::class.cast(modeRadioButton.tag)
    }

    private fun formatConfig(tab: String, default: String): String {
        val json = getViewConfig(tab, default)
        if (arrayOf("inbounds", "outbounds").contains(tab)) return JSONArray(json).toString(indentSpaces)
        return JSONObject(json).toString(indentSpaces)
    }

    private fun saveConfigs() {
        runCatching {
            config.log = formatConfig("log", config.log)
            config.dns = formatConfig("dns", config.dns)
            config.inbounds = formatConfig("inbounds", config.inbounds)
            config.outbounds = formatConfig("outbounds", config.outbounds)
            config.routing = formatConfig("routing", config.routing)
            config.logMode = getViewMode("log", config.logMode)
            config.dnsMode = getViewMode("dns", config.dnsMode)
            config.inboundsMode = getViewMode("inbounds", config.inboundsMode)
            config.outboundsMode = getViewMode("outbounds", config.outboundsMode)
            config.routingMode = getViewMode("routing", config.routingMode)
            config
        }.onSuccess {
            configViewModel.update(it)
            finish()
        }.onFailure {
            Toast.makeText(
                this, "Invalid config", Toast.LENGTH_SHORT
            ).show()
        }
    }
}
