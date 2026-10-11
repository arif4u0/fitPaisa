package com.fitpaisa.app.core.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.fitpaisa.app.BuildConfig
import com.fitpaisa.app.core.auth.SupabaseClientProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Info about an available app update (for sideloaded APKs, not Play Store). */
data class UpdateInfo(
    val latestVersion: String,
    val apkUrl: String,
    val changelog: String,
    val forceUpdate: Boolean
)

@Serializable
private data class ConfigRow(
    val key: String = "",
    val value: String = ""
)

/**
 * Checks Supabase app_config for a newer version. When the founder publishes
 * a new APK (updates the row + uploads to the app-releases bucket), users
 * see an "Update available" dialog with one-tap download and install.
 */
@Singleton
class UpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clientProvider: SupabaseClientProvider
) {
    /** Returns UpdateInfo if a newer version exists, null otherwise. */
    suspend fun checkForUpdate(): UpdateInfo? =
        withContext(Dispatchers.IO) {
            try {
                val client = clientProvider.client
                val rows = client.from("app_config")
                    .select()
                    .decodeList<ConfigRow>()
                val map = rows.associate { it.key to it.value }
                val latest = map["latest_version"] ?: return@withContext null
                if (!isNewer(latest, BuildConfig.VERSION_NAME)) return@withContext null
                val apkUrl = map["apk_url"] ?: ""
                if (apkUrl.isBlank()) return@withContext null
                UpdateInfo(
                    latestVersion = latest,
                    apkUrl = apkUrl,
                    changelog = map["changelog"] ?: "",
                    forceUpdate = map["force_update"] == "true"
                )
            } catch (e: Exception) {
                null
            }
        }

    /** True if [latest] is a newer semantic version than [current]. */
    private fun isNewer(latest: String, current: String): Boolean {
        val l = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(l.size, c.size)) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv != cv) return lv > cv
        }
        return false
    }

    /**
     * Google Drive serves a virus-scan warning page (HTML) instead of the
     * file when an executable is fetched via drive.google.com/uc?export=download.
     * DownloadManager would save that HTML as the "APK" and the install would
     * fail. The direct endpoint honors confirm=t and returns the real bytes
     * (verified: returns the raw APK from a fresh session).
     */
    private fun driveDirectUrl(url: String): String {
        val uri = Uri.parse(url) ?: return url
        if (uri.host != "drive.google.com") return url
        val id = uri.getQueryParameter("id") ?: return url
        return "https://drive.usercontent.google.com/download?id=$id&export=download&confirm=t"
    }

    /**
     * Opens the APK download link in the browser. We used to download via
     * DownloadManager, but Google Drive now serves a warning page (HTML)
     * instead of the file for large APKs, which corrupted the download.
     * The browser handles Drive's confirmation flow correctly.
     */
    fun downloadAndInstall(update: UpdateInfo) {
        // Convert to the Drive file view page (not the direct download URL).
        val browserUrl = driveViewUrl(update.apkUrl)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(browserUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Converts a drive.google.com/uc?export=download&id=XXX URL to the
     * file view page: drive.google.com/file/d/XXX/view
     */
    private fun driveViewUrl(url: String): String {
        val uri = Uri.parse(url) ?: return url
        // Already a view URL? Use as-is.
        if (url.contains("/file/d/")) return url
        val id = uri.getQueryParameter("id") ?: return url
        return "https://drive.google.com/file/d/$id/view"
    }

    private fun promptInstall(fileName: String) {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        if (!file.exists()) return
        // Sanity check: an APK is a ZIP, so it must start with the "PK"
        // magic bytes. If Drive ever serves an HTML warning page again,
        // don't prompt an install on the broken file.
        val magic = ByteArray(2)
        file.inputStream().use { it.read(magic) }
        if (!(magic[0] == 0x50.toByte() && magic[1] == 0x4B.toByte())) return
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } else {
            Uri.fromFile(file)
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}
