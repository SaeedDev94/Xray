package io.github.saeeddev94.xray.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.radiobutton.MaterialRadioButton
import io.github.saeeddev94.xray.BuildConfig
import io.github.saeeddev94.xray.R
import io.github.saeeddev94.xray.Settings
import io.github.saeeddev94.xray.adapter.LinkAdapter
import io.github.saeeddev94.xray.database.Link
import io.github.saeeddev94.xray.database.Profile
import io.github.saeeddev94.xray.databinding.ActivityLinksBinding
import io.github.saeeddev94.xray.helper.HttpHelper
import io.github.saeeddev94.xray.helper.LinkHelper
import io.github.saeeddev94.xray.viewmodel.LinkViewModel
import io.github.saeeddev94.xray.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import kotlin.reflect.cast

class LinksActivity : AppCompatActivity() {

    private val linkViewModel: LinkViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private val adapter by lazy { LinkAdapter() }
    private val linksRecyclerView by lazy { findViewById<RecyclerView>(R.id.linksRecyclerView) }
    private var links: List<Link> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.links)
        val binding = ActivityLinksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        adapter.onEditClick = { index, link -> openLink(index, link) }
        adapter.onDeleteClick = { link -> deleteLink(link) }
        linksRecyclerView.layoutManager = LinearLayoutManager(this)
        linksRecyclerView.itemAnimator = DefaultItemAnimator()
        linksRecyclerView.adapter = adapter
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                linkViewModel.links.collectLatest {
                    links = it
                    adapter.submitList(it)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_links, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.newLink -> openLink()
            R.id.refreshLinks -> refreshLinks()
            else -> finish()
        }
        return true
    }

    private fun refreshLinks() {
        Toast.makeText(applicationContext, "Getting update", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val profiles = profileViewModel.activeLinks()
            links.filter { it.isActive }.forEach { link ->
                runCatching {
                    val content = HttpHelper.get(link.address, link.userAgent).trim()
                    val newProfiles = if (link.type == Link.Type.Json) {
                        jsonProfiles(link, content)
                    } else {
                        subscriptionProfiles(link, content)
                    }
                    if (newProfiles.isNotEmpty()) {
                        val linkProfiles = profiles.filter { it.linkId == link.id }
                        manageProfiles(link, linkProfiles, newProfiles)
                    }
                }
            }
            withContext(Dispatchers.Main) {
                setResult(RESULT_OK)
                Toast.makeText(applicationContext, "Done", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun jsonProfiles(link: Link, value: String): List<Profile> {
        val list = arrayListOf<Profile>()
        val configs = runCatching { JSONArray(value) }.getOrNull() ?: JSONArray()
        for (i in 0 until configs.length()) {
            runCatching { JSONObject::class.cast(configs[i]) }.getOrNull()?.let { configuration ->
                val label = if (configuration.has("remarks")) {
                    val remarks = configuration.getString("remarks")
                    configuration.remove("remarks")
                    remarks
                } else {
                    LinkHelper.REMARK_DEFAULT
                }
                val json = configuration.toString(2)
                val profile = Profile().apply {
                    linkId = link.id
                    name = label
                    config = json
                }
                list.add(profile)
            }
        }
        return list.reversed().toList()
    }

    private fun subscriptionProfiles(link: Link, value: String): List<Profile> {
        return runCatching {
            val decoded = LinkHelper.decodeBase64(value).trim()
            decoded.split("\n")
                .reversed()
                .map { LinkHelper(it) }
                .filter { it.isValid() }
                .map { linkHelper ->
                    val profile = Profile()
                    profile.linkId = link.id
                    profile.config = linkHelper.json()
                    profile.name = linkHelper.remark()
                    profile
                }
        }.getOrNull() ?: listOf()
    }

    private suspend fun manageProfiles(
        link: Link, linkProfiles: List<Profile>, newProfiles: List<Profile>
    ) {
        if (newProfiles.size >= linkProfiles.size) {
            newProfiles.forEachIndexed { index, newProfile ->
                if (index >= linkProfiles.size) {
                    newProfile.linkId = link.id
                    insertProfile(newProfile)
                } else {
                    val linkProfile = linkProfiles[index]
                    updateProfile(linkProfile, newProfile)
                }
            }
            return
        }
        linkProfiles.forEachIndexed { index, linkProfile ->
            if (index >= newProfiles.size) {
                deleteProfile(linkProfile)
            } else {
                val newProfile = newProfiles[index]
                updateProfile(linkProfile, newProfile)
            }
        }
    }

    private suspend fun insertProfile(newProfile: Profile) {
        profileViewModel.insert(newProfile)
        profileViewModel.fixInsertIndex()
    }

    private suspend fun updateProfile(linkProfile: Profile, newProfile: Profile) {
        linkProfile.name = newProfile.name
        linkProfile.config = newProfile.config
        profileViewModel.update(linkProfile)
    }

    private suspend fun deleteProfile(linkProfile: Profile) {
        profileViewModel.delete(linkProfile)
        profileViewModel.fixDeleteIndex(linkProfile.index)
        withContext(Dispatchers.Main) {
            val selectedProfile = Settings.selectedProfile
            if (selectedProfile == linkProfile.id) {
                Settings.selectedProfile = 0L
                Settings.save(applicationContext)
            }
        }
    }

    private fun openLink(index: Int = -1, link: Link = Link()) {
        var title = getString(R.string.newLink)
        var confirm = getString(R.string.createLink)
        val cancel = getString(R.string.closeLink)
        var type = Link.Type.Json
        if (link.id != 0L) {
            title = getString(R.string.editLink)
            confirm = getString(R.string.updateLink)
            type = link.type
        }
        val layout = LayoutInflater.from(this).inflate(
            R.layout.layout_link_form,
            LinearLayout(this)
        )
        val typeRadioGroup = layout.findViewById<RadioGroup>(R.id.typeRadioGroup)
        val nameEditText = layout.findViewById<EditText>(R.id.nameEditText)
        val addressEditText = layout.findViewById<EditText>(R.id.addressEditText)
        val userAgentEditText = layout.findViewById<EditText>(R.id.userAgentEditText)
        val isActiveSwitch = layout.findViewById<MaterialSwitch>(R.id.isActiveSwitch)
        Link.Type.entries.forEach {
            val radio = MaterialRadioButton(this)
            radio.text = it.name
            radio.tag = it
            typeRadioGroup.addView(radio)
            if (it == type) typeRadioGroup.check(radio.id)
        }
        nameEditText.setText(link.name)
        addressEditText.setText(link.address)
        
        if (link.id == 0L) {
            userAgentEditText.setText("xray-${BuildConfig.VERSION_NAME}")
            isActiveSwitch.isChecked = true
        } else {
            userAgentEditText.setText(link.userAgent)
            isActiveSwitch.isChecked = link.isActive
        }
        
        MaterialAlertDialogBuilder(this).apply {
            setTitle(title)
            setView(layout)
            setPositiveButton(confirm) { dialog, _ ->
                dialog.dismiss()
                val address = addressEditText.text.toString()
                val typeRadioButton = typeRadioGroup.findViewById<RadioButton>(
                    typeRadioGroup.checkedRadioButtonId
                )
                val uri = runCatching { URI(address) }.getOrNull()
                val invalidLink = getString(R.string.invalidLink)
                val onlyHttps = getString(R.string.onlyHttps)
                if (uri == null) {
                    Toast.makeText(applicationContext, invalidLink, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (uri.scheme != "https") {
                    Toast.makeText(applicationContext, onlyHttps, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                link.type = Link.Type::class.cast(typeRadioButton.tag)
                link.name = nameEditText.text.toString()
                link.address = address
                link.userAgent = userAgentEditText.text.toString().ifBlank { null }
                link.isActive = isActiveSwitch.isChecked
                lifecycleScope.launch {
                    if (link.id == 0L) {
                        linkViewModel.insert(link)
                    } else {
                        linkViewModel.update(link)
                        adapter.notifyItemChanged(index)
                    }
                }
            }
            setNegativeButton(cancel) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

    private fun deleteLink(link: Link) {
        lifecycleScope.launch {
            profileViewModel.linkProfiles(link.id)
                .forEach { linkProfile ->
                    deleteProfile(linkProfile)
                }
            linkViewModel.delete(link)
            withContext(Dispatchers.Main) {
                setResult(RESULT_OK)
            }
        }
    }
}
