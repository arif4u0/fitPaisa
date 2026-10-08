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
     * Downloads the APK via DownloadManager and prompts install when done.
     * Returns immediately; the install prompt appears automatically.
     */
    fun downloadAndInstall(update: UpdateInfo) {
        val fileName = "fitpaisa-${update.latestVersion}.apk"
        val request = DownloadManager.Request(Uri.parse(update.apkUrl)).apply {
            setTitle("fitPaisa update ${update.latestVersion}")
            setDescription("Downloading new version…")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setMimeType("application/vnd.android.package-archive")
        }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) != downloadId) return
                ctx.unregisterReceiver(this)
                promptInstall(fileName)
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun promptInstall(fileName: String) {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        if (!file.exists()) return
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
