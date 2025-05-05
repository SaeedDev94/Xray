package io.github.saeeddev94.xray.repository

import io.github.saeeddev94.xray.database.Link
import io.github.saeeddev94.xray.database.LinkDao

class LinkRepository(private val linkDao: LinkDao) {

    val all = linkDao.all()

    suspend fun activeLinks(): List<Link> {
        return linkDao.activeLinks()
    }

    suspend fun insert(link: Link) {
        linkDao.insert(link)
    }

    suspend fun update(link: Link) {
        linkDao.update(link)
    }

    suspend fun delete(link: Link) {
        linkDao.delete(link)
    }
}
