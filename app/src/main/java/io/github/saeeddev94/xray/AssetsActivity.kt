package io.github.saeeddev94.xray

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.saeeddev94.xray.databinding.ActivityAssetsBinding

class AssetsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAssetsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.assets)
        binding = ActivityAssetsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

}
