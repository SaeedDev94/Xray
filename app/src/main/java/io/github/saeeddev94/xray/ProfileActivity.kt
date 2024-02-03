package io.github.saeeddev94.xray

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.saeeddev94.xray.database.Profile
import io.github.saeeddev94.xray.database.XrayDatabase
import io.github.saeeddev94.xray.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

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
        binding.profileSave.setOnClickListener { save() }
        if (isNew()) {
            resolved(Profile())
        } else {
            Thread {
                val profile = XrayDatabase.ref(applicationContext).profileDao().find(id)
                runOnUiThread {
                    resolved(profile)
                }
            }.start()
        }
    }

    private fun isNew() = id == 0L

    private fun resolved(value: Profile) {
        profile = value
        binding.profileName.setText(profile.name)
        binding.profileConfig.setText(profile.config)
    }

    private fun save() {
        profile.name = binding.profileName.text.toString()
        profile.config = binding.profileConfig.text.toString()
        Thread {
            val testConfig = Settings.testConfig(applicationContext)
            testConfig.writeText(profile.config)
            val error = libXray.LibXray.testXray(applicationContext.filesDir.absolutePath, testConfig.absolutePath)
            if (error.isNotEmpty()) {
                runOnUiThread {
                    Toast.makeText(applicationContext, error, Toast.LENGTH_SHORT).show()
                }
                return@Thread
            }
            val db = XrayDatabase.ref(applicationContext)
            if (profile.id == 0L) {
                profile.id = db.profileDao().insert(profile)
                db.profileDao().shiftIndex()
            } else {
                db.profileDao().update(profile)
            }
            runOnUiThread {
                Intent().also {
                    it.putExtra("id", profile.id)
                    it.putExtra("index", index)
                    setResult(RESULT_OK, it)
                    finish()
                }
            }
        }.start()
    }

}
