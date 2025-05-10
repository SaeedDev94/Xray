package io.github.saeeddev94.xray.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.saeeddev94.xray.R
import io.github.saeeddev94.xray.adapter.LinkAdapter
import io.github.saeeddev94.xray.database.Link
import io.github.saeeddev94.xray.databinding.ActivityLinksBinding
import io.github.saeeddev94.xray.viewmodel.LinkViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LinksActivity : AppCompatActivity() {

    private val linkViewModel: LinkViewModel by viewModels()
    private val adapter by lazy { LinkAdapter() }
    private val linksRecyclerView by lazy { findViewById<RecyclerView>(R.id.linksRecyclerView) }
    private var links: MutableList<Link> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.links)
        val binding = ActivityLinksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        adapter.onEditClick = { link -> openLink(link) }
        adapter.onDeleteClick = { link -> deleteLink(link) }
        linksRecyclerView.layoutManager = LinearLayoutManager(this)
        linksRecyclerView.itemAnimator = DefaultItemAnimator()
        linksRecyclerView.adapter = adapter
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                linkViewModel.links.collectLatest {
                    links = it.toMutableList()
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
            R.id.refreshLinks -> refreshLinks()
            R.id.newLink -> openLink()
            else -> finish()
        }
        return true
    }

    private fun refreshLinks() {
        val intent = LinksManagerActivity.refreshLinks(applicationContext)
        startActivity(intent)
    }

    private fun openLink(link: Link = Link()) {
        val intent = LinksManagerActivity.openLink(applicationContext, link)
        startActivity(intent)
    }

    private fun deleteLink(link: Link) {
        val intent = LinksManagerActivity.deleteLink(applicationContext, link)
        startActivity(intent)
    }
}
