package com.mymod.playspoofer.util

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ConfigManager {
    private const val CONFIG_FILE = "/data/local/tmp/playspoofer_config.txt"
    private const val PLAY_STORE_PKG = "com.android.vending"
    
    data class PlayStoreVersion(
        val versionCode: Long,
        val versionName: String
    )
    
    data class SpoofConfig(
        val versionCode: String,
        val versionName: String
    )
    
    /**
     * Detect the actual Play Store version installed on the device
     */
    fun detectPlayStoreVersion(context: Context): PlayStoreVersion? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(PLAY_STORE_PKG, 0)
            PlayStoreVersion(
                versionCode = packageInfo.longVersionCode,
                versionName = packageInfo.versionName ?: "Unknown"
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
    
    /**
     * Read current config from file
     */
    suspend fun readConfig(): SpoofConfig? = withContext(Dispatchers.IO) {
        try {
            val file = File(CONFIG_FILE)
            if (!file.exists()) return@withContext null
            
            var versionCode: String? = null
            var versionName: String? = null
            
            file.readLines().forEach { line ->
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    when (parts[0].trim()) {
                        "version_code" -> versionCode = parts[1].trim()
                        "version_name" -> versionName = parts[1].trim()
                    }
                }
            }
            
            if (versionCode != null && versionName != null) {
                SpoofConfig(versionCode!!, versionName!!)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Write config to file using root/shell
     */
    suspend fun writeConfig(versionCode: String, versionName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val content = "version_code=$versionCode\nversion_name=$versionName"
            
            // Try to write using su (root)
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo '$content' > $CONFIG_FILE && chmod 644 $CONFIG_FILE"))
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                return@withContext true
            }
            
            // Fallback: try writing directly (might work on some devices)
            try {
                File(CONFIG_FILE).writeText(content)
                return@withContext true
            } catch (e: Exception) {
                // Ignore
            }
            
            false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Delete config file (reset to defaults)
     */
    suspend fun deleteConfig(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "rm -f $CONFIG_FILE"))
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}

