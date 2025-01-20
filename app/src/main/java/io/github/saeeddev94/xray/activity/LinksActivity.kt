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
import io.github.saeeddev94.xray.R
import io.github.saeeddev94.xray.Settings
import io.github.saeeddev94.xray.adapter.LinkAdapter
import io.github.saeeddev94.xray.database.Link
import io.github.saeeddev94.xray.database.Profile
import io.github.saeeddev94.xray.databinding.ActivityLinksBinding
import io.github.saeeddev94.xray.helper.ConfigHelper
import io.github.saeeddev94.xray.helper.HttpHelper
import io.github.saeeddev94.xray.helper.LinkHelper
import io.github.saeeddev94.xray.viewmodel.LinkViewModel
import io.github.saeeddev94.xray.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                    val content = HttpHelper.get(link.address).trim()
                    val newProfiles = if (link.type == Link.Type.Json) {
                        jsonProfile(link, content)
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

    private suspend fun jsonProfile(link: Link, value: String): List<Profile> {
        val list = arrayListOf<Profile>()
        runCatching {
            val error = ConfigHelper.isValid(applicationContext, value)
            if (error.isEmpty()) {
                val name = LinkHelper.remark(URI(link.address))
                val config = JSONObject(value).toString(2)
                val profile = Profile()
                profile.linkId = link.id
                profile.name = name
                profile.config = config
                list.add(profile)
            }
        }
        return list.toList()
    }

    private suspend fun subscriptionProfiles(link: Link, value: String): List<Profile> {
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
                }.filter {
                    val error = ConfigHelper.isValid(applicationContext, it.config)
                    error.isEmpty()
                }
        }.getOrNull() ?: listOf()
    }

    private suspend fun manageProfiles(link: Link, linkProfiles: List<Profile>, newProfiles: List<Profile>) {
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
        isActiveSwitch.isChecked = link.isActive
        MaterialAlertDialogBuilder(this).apply {
            setTitle(title)
            setView(layout)
            setPositiveButton(confirm) { dialog, _ ->
                dialog.dismiss()
                val typeRadioButton = typeRadioGroup.findViewById<RadioButton>(
                    typeRadioGroup.checkedRadioButtonId
                )
                link.type = Link.Type::class.cast(typeRadioButton.tag)
                link.name = nameEditText.text.toString()
                link.address = addressEditText.text.toString()
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
