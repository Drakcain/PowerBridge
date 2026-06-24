package com.powerbridge.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

data class AppUpdateOffer(
    val tagName: String,
    val versionName: String,
    val assetName: String,
    val assetUrl: String,
    val releaseUrl: String,
    val notes: String
)

object AppUpdater {
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/Drakcain/PowerBridge/releases/latest"
    private val httpClient = OkHttpClient()

    fun checkForUpdates(
        activity: Activity,
        currentVersion: String,
        manual: Boolean,
        onStatus: (String, Int) -> Unit
    ) {
        onStatus(
            if (manual) "Checking GitHub Releases for a newer PowerBridge build..." else "Checking for app updates...",
            R.color.cp_cyan
        )

        Thread {
            try {
                val offer = fetchLatestOffer(currentVersion)
                activity.runOnUiThread {
                    if (offer == null) {
                        onStatus(
                            if (manual) "You already have the latest PowerBridge build." else "App updates: current build is already latest.",
                            R.color.cp_success
                        )
                    } else {
                        onStatus("Update available: ${offer.tagName}", R.color.cp_warning)
                        showUpdateDialog(activity, offer, onStatus)
                    }
                }
            } catch (exc: Exception) {
                activity.runOnUiThread {
                    onStatus(
                        if (manual) "Update check failed: ${exc.message}" else "App update check unavailable right now.",
                        R.color.cp_error
                    )
                }
            }
        }.start()
    }

    private fun showUpdateDialog(
        activity: Activity,
        offer: AppUpdateOffer,
        onStatus: (String, Int) -> Unit
    ) {
        val message = buildString {
            appendLine("Installed build: ${appVersion(activity)}")
            appendLine("Latest build: ${offer.tagName}")
            appendLine()
            appendLine("PowerBridge can download the newer APK and hand it to the Android package installer.")
            appendLine("Android still requires your confirmation before replacing the current app.")
            if (offer.notes.isNotBlank()) {
                appendLine()
                append(offer.notes.take(1200))
            }
        }

        AlertDialog.Builder(activity)
            .setTitle("PowerBridge update available")
            .setMessage(message)
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton("Open release page") { _, _ ->
                activity.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(offer.releaseUrl))
                )
            }
            .setPositiveButton("Download APK") { _, _ ->
                downloadAndInstall(activity, offer, onStatus)
            }
            .show()
    }

    private fun downloadAndInstall(
        activity: Activity,
        offer: AppUpdateOffer,
        onStatus: (String, Int) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivity(intent)
            onStatus(
                "Enable Install unknown apps for PowerBridge, then run Check for Updates again.",
                R.color.cp_warning
            )
            return
        }

        onStatus("Downloading ${offer.assetName}...", R.color.cp_cyan)
        Thread {
            try {
                val apkFile = downloadApk(activity, offer)
                activity.runOnUiThread {
                    launchInstaller(activity, apkFile)
                    onStatus(
                        "Downloaded ${offer.assetName}. Android installer opened for confirmation.",
                        R.color.cp_success
                    )
                }
            } catch (exc: Exception) {
                activity.runOnUiThread {
                    onStatus("APK download failed: ${exc.message}", R.color.cp_error)
                }
            }
        }.start()
    }

    private fun fetchLatestOffer(currentVersion: String): AppUpdateOffer? {
        val request = Request.Builder()
            .url(LATEST_RELEASE_URL)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "PowerBridge-Updater")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("GitHub API returned HTTP ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            val json = JSONObject(body)
            val tagName = json.optString("tag_name").trim()
            val releaseUrl = json.optString("html_url").ifBlank { "https://github.com/Drakcain/PowerBridge/releases" }
            val versionName = tagName.removePrefix("v")
            if (tagName.isBlank() || compareVersions(versionName, currentVersion) <= 0) {
                return null
            }
            val assets = json.optJSONArray("assets")
            var selectedName = ""
            var selectedUrl = ""
            if (assets != null) {
                for (index in 0 until assets.length()) {
                    val asset = assets.getJSONObject(index)
                    val name = asset.optString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        selectedName = name
                        selectedUrl = asset.optString("browser_download_url")
                        break
                    }
                }
            }
            if (selectedName.isBlank() || selectedUrl.isBlank()) {
                throw IllegalStateException("Latest release does not include an APK asset.")
            }
            return AppUpdateOffer(
                tagName = tagName,
                versionName = versionName,
                assetName = selectedName,
                assetUrl = selectedUrl,
                releaseUrl = releaseUrl,
                notes = json.optString("body").trim()
            )
        }
    }

    private fun downloadApk(activity: Activity, offer: AppUpdateOffer): File {
        val request = Request.Builder()
            .url(offer.assetUrl)
            .header("User-Agent", "PowerBridge-Updater")
            .build()
        val targetDir = File(activity.cacheDir, "updates").apply { mkdirs() }
        val safeName = offer.assetName.ifBlank { "PowerBridge-${offer.versionName}.apk" }
        val targetFile = File(targetDir, safeName)
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Download returned HTTP ${response.code}")
            }
            val body = response.body ?: throw IllegalStateException("Download response was empty.")
            targetFile.outputStream().use { output ->
                body.byteStream().use { input -> input.copyTo(output) }
            }
        }
        return targetFile
    }

    private fun launchInstaller(activity: Activity, apkFile: File) {
        val contentUri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(intent)
    }

    private fun compareVersions(left: String, right: String): Int {
        val leftParts = normalizeVersionParts(left)
        val rightParts = normalizeVersionParts(right)
        val max = maxOf(leftParts.size, rightParts.size)
        for (index in 0 until max) {
            val l = leftParts.getOrElse(index) { 0 }
            val r = rightParts.getOrElse(index) { 0 }
            if (l != r) return l.compareTo(r)
        }
        return 0
    }

    private fun normalizeVersionParts(raw: String): List<Int> {
        return raw.trim().removePrefix("v").split('.').map {
            it.filter(Char::isDigit).ifBlank { "0" }.toInt()
        }
    }

    fun appVersion(activity: Activity): String {
        return try {
            val pkg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.packageManager.getPackageInfo(activity.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                activity.packageManager.getPackageInfo(activity.packageName, 0)
            }
            pkg.versionName ?: "Unknown"
        } catch (_: Exception) {
            "Unknown"
        }
    }
}
