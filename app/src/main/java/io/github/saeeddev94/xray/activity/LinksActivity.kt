package io.github.saeeddev94.xray.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import io.github.saeeddev94.xray.R
import io.github.saeeddev94.xray.databinding.ActivityLinksBinding

class LinksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLinksBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.links)
        binding = ActivityLinksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        getLinks()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_links, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.newLink -> newLink()
            R.id.refreshLinks -> refreshLinks()
            else -> finish()
        }
        return true
    }

    private fun getLinks() {
    }

    private fun newLink() {
    }

    private fun refreshLinks() {
    }
}
