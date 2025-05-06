package io.github.saeeddev94.xray.activity

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Build
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
import io.github.saeeddev94.xray.helper.LinkHelper
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
        private const val LINK_INDEX = "index"
        private const val REFRESH_ACTION = "refresh"
        private const val DELETE_ACTION = "delete"

        fun refreshLinks(context: Context): Intent {
            return Intent(context, LinksManagerActivity::class.java)
        }

        fun openLink(context: Context, link: Link = Link(), index: Int = -1): Intent {
            return Intent(context, LinksManagerActivity::class.java).apply {
                putExtra(LINK_REF, link)
                putExtra(LINK_INDEX, index)
            }
        }

        fun deleteLink(context: Context, link: Link): Intent {
            return Intent(context, LinksManagerActivity::class.java).apply {
                putExtra(LINK_REF, link)
                putExtra(DELETE_ACTION, true)
            }
        }

        fun getLink(intent: Intent): Link? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(LINK_REF, Link::class.java)
            } else {
                @Suppress("deprecation")
                intent.getParcelableExtra(LINK_REF)
            }
        }

        fun getIndex(intent: Intent): Int {
            return intent.getIntExtra(LINK_INDEX, -1)
        }

        fun getRefresh(intent: Intent): Boolean {
            return intent.getBooleanExtra(REFRESH_ACTION, false)
        }
    }

    private val linkViewModel: LinkViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val link: Link? = getLink(intent)
        val index: Int = getIndex(intent)
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
            Intent().also {
                it.putExtra(LINK_INDEX, index)
                it.putExtra(LINK_REF, link)
                setResult(RESULT_OK, it)
            }
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
            val profiles = profileViewModel.activeLinks()
            links.forEach { link ->
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
                Intent().also {
                    it.putExtra(REFRESH_ACTION, true)
                    setResult(RESULT_OK, it)
                }
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
        return runCatching {
            val decoded = LinkHelper.tryDecodeBase64(value).trim()
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

    private fun deleteLink(link: Link) {
        lifecycleScope.launch {
            profileViewModel.linkProfiles(link.id)
                .forEach { linkProfile ->
                    deleteProfile(linkProfile)
                }
            linkViewModel.delete(link)
            withContext(Dispatchers.Main) {
                Intent().also {
                    it.putExtra(REFRESH_ACTION, true)
                    setResult(RESULT_OK, it)
                }
                finish()
            }
        }
    }
}
