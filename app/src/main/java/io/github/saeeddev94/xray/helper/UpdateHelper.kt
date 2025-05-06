package io.github.saeeddev94.xray.helper

import io.github.saeeddev94.xray.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class UpdateHelper {
    companion object {
        private const val GITHUB_RELEASES_URL = "https://api.github.com/repos/saeeddev94/Xray/releases"
        
        suspend fun checkForUpdates(): UpdateInfo? {
            return withContext(Dispatchers.IO) {
                try {
                    val url = URL(GITHUB_RELEASES_URL)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.connect()
                    
                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val releases = JSONArray(response)
                        
                        if (releases.length() > 0) {
                            val latestRelease = releases.getJSONObject(0)
                            val tagName = latestRelease.getString("tag_name")
                            val releaseUrl = latestRelease.getString("html_url")
                            
                            val latestVersion = tagName.replace("v", "").trim()
                            val currentVersion = BuildConfig.VERSION_NAME
                            
                            if (isNewer(latestVersion, currentVersion)) {
                                return@withContext UpdateInfo(latestVersion, releaseUrl)
                            }
                        }
                    }
                    null
                } catch (e: Exception) {
                    null
                }
            }
        }
        
        private fun isNewer(latestVersion: String, currentVersion: String): Boolean {
            try {
                val latest = latestVersion.split(".").map { it.toInt() }
                val current = currentVersion.split(".").map { it.toInt() }
                
                for (i in 0 until minOf(latest.size, current.size)) {
                    if (latest[i] > current[i]) return true
                    if (latest[i] < current[i]) return false
                }
                
                return latest.size > current.size
            } catch (e: Exception) {
                return false
            }
        }
    }
    
    data class UpdateInfo(val version: String, val downloadUrl: String)
} 
