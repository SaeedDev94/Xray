package io.github.saeeddev94.xray.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
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
import io.github.saeeddev94.xray.adapter.LinkAdapter
import io.github.saeeddev94.xray.database.Link
import io.github.saeeddev94.xray.databinding.ActivityLinksBinding
import io.github.saeeddev94.xray.viewmodel.LinkViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.reflect.cast

class LinksActivity : AppCompatActivity() {

    private val linkViewModel: LinkViewModel by viewModels()
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
                    withContext(Dispatchers.Main) {
                        links = it
                        adapter.submitList(it)
                    }
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
                        withContext(Dispatchers.Main) {
                            adapter.notifyItemChanged(index)
                        }
                    }
                }
            }
            setNegativeButton(cancel) { dialog, _ ->
                dialog.dismiss()
            }
        }.create().show()
    }

    private fun deleteLink(link: Link) {
        lifecycleScope.launch {
            linkViewModel.delete(link)
        }
    }
}
