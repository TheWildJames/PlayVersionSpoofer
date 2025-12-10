package com.mymod.playspoofer.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.mymod.playspoofer.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String,
    val htmlUrl: String,
    val downloadUrl: String?,
    val isPrerelease: Boolean,
    val publishedAt: String
)

object UpdateChecker {
    private const val GITHUB_API_URL = "https://api.github.com/repos/TheWildJames/PlayVersionSpoofer/releases"
    
    // Current version code from BuildConfig
    val currentVersionCode: Int get() = BuildConfig.VERSION_CODE
    val currentVersionName: String get() = BuildConfig.VERSION_NAME
    
    /**
     * Fetches the latest release from GitHub
     * @param includePrerelease Whether to include pre-releases (test builds)
     */
    suspend fun checkForUpdates(includePrerelease: Boolean = true): Result<ReleaseInfo?> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(GITHUB_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    setRequestProperty("User-Agent", "PlayVersionSpoofer/${currentVersionName}")
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext Result.failure(Exception("HTTP ${connection.responseCode}"))
                }
                
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val releases = JSONArray(response)
                
                if (releases.length() == 0) {
                    return@withContext Result.success(null)
                }
                
                // Find the latest applicable release
                for (i in 0 until releases.length()) {
                    val release = releases.getJSONObject(i)
                    val isPrerelease = release.getBoolean("prerelease")
                    
                    // Skip prereleases if not wanted
                    if (isPrerelease && !includePrerelease) continue
                    
                    val tagName = release.getString("tag_name")
                    val name = release.optString("name", tagName)
                    val body = release.optString("body", "")
                    val htmlUrl = release.getString("html_url")
                    val publishedAt = release.getString("published_at")
                    
                    // Find APK download URL from assets
                    var downloadUrl: String? = null
                    val assets = release.optJSONArray("assets")
                    if (assets != null) {
                        for (j in 0 until assets.length()) {
                            val asset = assets.getJSONObject(j)
                            val assetName = asset.getString("name")
                            if (assetName.endsWith(".apk")) {
                                downloadUrl = asset.getString("browser_download_url")
                                break
                            }
                        }
                    }
                    
                    val releaseInfo = ReleaseInfo(
                        tagName = tagName,
                        name = name,
                        body = body,
                        htmlUrl = htmlUrl,
                        downloadUrl = downloadUrl,
                        isPrerelease = isPrerelease,
                        publishedAt = publishedAt
                    )
                    
                    // Check if this is a newer version
                    if (isNewerVersion(tagName, isPrerelease)) {
                        return@withContext Result.success(releaseInfo)
                    }
                    // Continue checking other releases - there might be a newer stable release
                }
                
                Result.success(null)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Check if the given tag represents a newer version
     */
    private fun isNewerVersion(tagName: String, isPrerelease: Boolean): Boolean {
        // Handle test builds like "test-123"
        if (tagName.startsWith("test-")) {
            val buildNum = tagName.removePrefix("test-").toIntOrNull() ?: return false
            // Compare against current version code for test builds
            // Test builds are newer if their build number is higher than current version code
            return buildNum > currentVersionCode
        }
        
        // Handle version tags like "v1.3" or "1.3"
        val versionStr = tagName.removePrefix("v")
        val parts = versionStr.split(".")
        
        try {
            val remoteMajor = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val remoteMinor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val remotePatch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            
            val currentParts = currentVersionName.split(".")
            val currentMajor = currentParts.getOrNull(0)?.toIntOrNull() ?: 0
            val currentMinor = currentParts.getOrNull(1)?.toIntOrNull() ?: 0
            val currentPatch = currentParts.getOrNull(2)?.toIntOrNull() ?: 0
            
            return when {
                remoteMajor > currentMajor -> true
                remoteMajor < currentMajor -> false
                remoteMinor > currentMinor -> true
                remoteMinor < currentMinor -> false
                remotePatch > currentPatch -> true
                else -> false
            }
        } catch (e: Exception) {
            // If parsing fails, assume it's newer (to be safe)
            return true
        }
    }
    
    /**
     * Open the release page in browser
     */
    fun openReleasePage(context: Context, releaseInfo: ReleaseInfo) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(releaseInfo.htmlUrl))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
    
    /**
     * Open the direct download link in browser
     */
    fun downloadApk(context: Context, releaseInfo: ReleaseInfo) {
        val url = releaseInfo.downloadUrl ?: releaseInfo.htmlUrl
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

