package io.github.saeeddev94.xray.activity

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.saeeddev94.xray.R
import io.github.saeeddev94.xray.Settings
import io.github.saeeddev94.xray.database.Link
import io.github.saeeddev94.xray.database.Profile
import io.github.saeeddev94.xray.fragment.LinkFormFragment
import io.github.saeeddev94.xray.helper.HttpHelper
import io.github.saeeddev94.xray.helper.IntentHelper
import io.github.saeeddev94.xray.helper.LinkHelper
import io.github.saeeddev94.xray.service.TProxyService
import io.github.saeeddev94.xray.viewmodel.LinkViewModel
import io.github.saeeddev94.xray.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.reflect.cast

class LinksManagerActivity : AppCompatActivity() {

    companion object {
        private const val LINK_REF = "ref"
        private const val DELETE_ACTION = "delete"

        fun refreshLinks(context: Context): Intent {
            return Intent(context, LinksManagerActivity::class.java)
        }

        fun openLink(context: Context, link: Link = Link()): Intent {
            return Intent(context, LinksManagerActivity::class.java).apply {
                putExtra(LINK_REF, link)
            }
        }

        fun deleteLink(context: Context, link: Link): Intent {
            return Intent(context, LinksManagerActivity::class.java).apply {
                putExtra(LINK_REF, link)
                putExtra(DELETE_ACTION, true)
            }
        }
    }

    private val settings by lazy { Settings(applicationContext) }
    private val linkViewModel: LinkViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val link: Link? = IntentHelper.getParcelable(intent, LINK_REF, Link::class.java)
        val deleteAction = intent.getBooleanExtra(DELETE_ACTION, false)

        if (link == null) {
            refreshLinks()
            return
        }

        if (deleteAction) {
            deleteLink(link)
            return
        }

        LinkFormFragment(link) {
            if (link.id == 0L) {
                linkViewModel.insert(link)
            } else {
                linkViewModel.update(link)
            }
            setResult(RESULT_OK)
            finish()
        }.show(supportFragmentManager, null)
    }

    private fun loadingDialog(): Dialog {
        val dialogView = LayoutInflater.from(this).inflate(
            R.layout.loading_dialog,
            LinearLayout(this)
        )
        return MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
    }

    private fun refreshLinks() {
        val loadingDialog = loadingDialog()
        loadingDialog.show()
        lifecycleScope.launch {
            val links = linkViewModel.activeLinks()
            links.forEach { link ->
                val profiles = profileViewModel.linkProfiles(link.id)
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
                settings.lastRefreshLinks = System.currentTimeMillis()
                TProxyService.newConfig(applicationContext)
                loadingDialog.dismiss()
                finish()
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
        val decoded = runCatching { LinkHelper.tryDecodeBase64(value).trim() }.getOrNull() ?: ""
        return decoded.split("\n")
            .reversed()
            .map { LinkHelper(settings, it) }
            .filter { it.isValid() }
            .map { linkHelper ->
                val profile = Profile()
                profile.linkId = link.id
                profile.config = linkHelper.json()
                profile.name = linkHelper.remark()
                profile
            }
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
        profileViewModel.create(newProfile)
    }

    private suspend fun updateProfile(linkProfile: Profile, newProfile: Profile) {
        linkProfile.name = newProfile.name
        linkProfile.config = newProfile.config
        profileViewModel.update(linkProfile)
    }

    private suspend fun deleteProfile(linkProfile: Profile) {
        profileViewModel.remove(linkProfile)
        withContext(Dispatchers.Main) {
            val selectedProfile = settings.selectedProfile
            if (selectedProfile == linkProfile.id) {
                settings.selectedProfile = 0L
            }
        }
    }

    private fun deleteLink(link: Link) {
        lifecycleScope.launch {
            profileViewModel.linkProfiles(link.id)
                .forEach { linkProfile ->
                    deleteProfile(linkProfile)
                }
            linkViewModel.delete(link)
            withContext(Dispatchers.Main) {
                finish()
            }
        }
    }
}
