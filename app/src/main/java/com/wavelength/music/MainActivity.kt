package com.wavelength.music

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wavelength.music.ui.components.OfflineBanner
import com.wavelength.music.ui.navigation.WavelengthNavHost
import com.wavelength.music.ui.settings.AppSettingsViewModel
import com.wavelength.music.ui.theme.WavelengthTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val updaterClient by lazy { OkHttpClient() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val settingsViewModel: AppSettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.state.collectAsStateWithLifecycle()
            var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
            var updateDismissed by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                updateInfo = checkForUpdate()
            }

            WavelengthTheme(
                theme = settings.theme,
                customLiquidAccent = Color(settings.customAccentArgb)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    OfflineBanner()
                    Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black)) {
                        WavelengthNavHost()
                    }
                }

                val available = updateInfo
                if (available != null && !updateDismissed) {
                    AlertDialog(
                        onDismissRequest = { updateDismissed = true },
                        title = { Text("AzMusic update available") },
                        text = {
                            Text(
                                "A newer GitHub release is available (build ${available.buildNumber}). " +
                                    "Installed build: ${BuildConfig.VERSION_CODE}."
                            )
                        },
                        dismissButton = {
                            TextButton(onClick = { updateDismissed = true }) {
                                Text("Later")
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                if (canInstallPackages()) {
                                    downloadAndInstall(available)
                                    updateDismissed = true
                                } else {
                                    openUnknownAppsSettings()
                                }
                            }) {
                                Text("Download & install")
                            }
                        }
                    )
                }
            }
        }
    }

    private suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://api.github.com/repos/Mhd-Azeem/music-app-azeem/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "AzMusic-Updater")
                .header("Cache-Control", "no-cache")
                .build()

            updaterClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val json = JSONObject(response.body?.string().orEmpty())
                val releaseBody = json.optString("body")
                val buildNumber = Regex("Build number:\\s*(\\d+)")
                    .find(releaseBody)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toLongOrNull()
                    ?: return@use null

                if (buildNumber <= BuildConfig.VERSION_CODE.toLong()) return@use null

                val assets = json.optJSONArray("assets") ?: return@use null
                var apkUrl: String? = null
                for (index in 0 until assets.length()) {
                    val asset = assets.optJSONObject(index) ?: continue
                    val name = asset.optString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        if (apkUrl.isNotBlank()) break
                    }
                }

                val url = apkUrl?.takeIf { it.isNotBlank() } ?: return@use null
                UpdateInfo(buildNumber = buildNumber, apkUrl = url)
            }
        }.getOrNull()
    }

    private fun canInstallPackages(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || packageManager.canRequestPackageInstalls()
    }

    private fun openUnknownAppsSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Toast.makeText(
                this,
                "Allow AzMusic to install updates, then return and tap Download & install again.",
                Toast.LENGTH_LONG
            ).show()
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    private fun downloadAndInstall(info: UpdateInfo) {
        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val fileName = "AzMusic-update-${info.buildNumber}.apk"
        val request = DownloadManager.Request(Uri.parse(info.apkUrl))
            .setTitle("AzMusic update")
            .setDescription("Downloading build ${info.buildNumber}")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, fileName)

        val downloadId = downloadManager.enqueue(request)
        Toast.makeText(this, "AzMusic update downloading…", Toast.LENGTH_SHORT).show()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) != downloadId) return

                runCatching { unregisterReceiver(this) }

                val uri = downloadManager.getUriForDownloadedFile(downloadId)
                if (uri == null) {
                    Toast.makeText(
                        this@MainActivity,
                        "Update download failed. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(installIntent)
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(receiver, filter)
        }
    }

    private data class UpdateInfo(
        val buildNumber: Long,
        val apkUrl: String
    )
}
