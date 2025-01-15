package io.github.saeeddev94.xray

import android.app.Application
import io.github.saeeddev94.xray.database.XrayDatabase
import io.github.saeeddev94.xray.repository.LinkRepository

class Xray : Application() {

    private val xrayDatabase by lazy { XrayDatabase.ref(this) }
    val linkRepository by lazy { LinkRepository(xrayDatabase.linkDao()) }
}
