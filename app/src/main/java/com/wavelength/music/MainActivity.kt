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
import android.widget.VideoView
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.wavelength.music.ui.components.OfflineBanner
import com.wavelength.music.ui.navigation.WavelengthNavHost
import com.wavelength.music.ui.settings.AppSettingsViewModel
import com.wavelength.music.ui.theme.WavelengthTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val updaterClient by lazy { OkHttpClient() }
    private val updaterPrefs by lazy {
        getSharedPreferences("azmusic_updater", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            val settingsViewModel: AppSettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.state.collectAsStateWithLifecycle()
            var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
            var updateDismissed by remember { mutableStateOf(false) }
            var showIntroVideo by remember { mutableStateOf(savedInstanceState == null) }

            LaunchedEffect(Unit) {
                updateInfo = checkForUpdate()
            }

            LaunchedEffect(showIntroVideo) {
                if (!showIntroVideo && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            WavelengthTheme(
                theme = settings.theme,
                customLiquidAccent = Color(settings.customAccentArgb),
                glassmorphismEnabled = settings.glassmorphismNowPlaying
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    OfflineBanner()
                    val appSurface = if (settings.glassmorphismNowPlaying) {
                        Modifier.background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(settings.customAccentArgb).copy(alpha = 0.24f),
                                    Color(0xFF0A1628),
                                    Color(0xFF111827)
                                )
                            )
                        )
                    } else {
                        Modifier.background(Color.Black)
                    }
                    Box(modifier = Modifier.fillMaxWidth().weight(1f).then(appSurface)) {
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
                                } else {
                                    updaterPrefs.edit()
                                        .putString(KEY_PENDING_APK_URL, available.apkUrl)
                                        .putLong(KEY_PENDING_BUILD_NUMBER, available.buildNumber)
                                        .apply()
                                    openUnknownAppsSettings()
                                }
                                updateDismissed = true
                            }) {
                                Text("Download & install")
                            }
                        }
                    )
                }

                if (showIntroVideo) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(
                                    Uri.parse(
                                        "android.resource://${ctx.packageName}/${R.raw.azmusic_intro}"
                                    )
                                )
                                setOnPreparedListener { mediaPlayer ->
                                    mediaPlayer.setVolume(0f, 0f)
                                    mediaPlayer.isLooping = false
                                    start()
                                }
                                setOnCompletionListener {
                                    showIntroVideo = false
                                }
                                setOnErrorListener { _, _, _ ->
                                    showIntroVideo = false
                                    true
                                }
                                setOnClickListener {
                                    stopPlayback()
                                    showIntroVideo = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!canInstallPackages()) return

        val pendingDownloadId = updaterPrefs.getLong(KEY_PENDING_DOWNLOAD_ID, -1L)
        if (pendingDownloadId != -1L) {
            tryLaunchDownloadedUpdate(pendingDownloadId)
            return
        }

        val pendingUrl = updaterPrefs.getString(KEY_PENDING_APK_URL, null)
        val pendingBuild = updaterPrefs.getLong(KEY_PENDING_BUILD_NUMBER, -1L)
        if (!pendingUrl.isNullOrBlank() && pendingBuild > 0L) {
            updaterPrefs.edit()
                .remove(KEY_PENDING_APK_URL)
                .remove(KEY_PENDING_BUILD_NUMBER)
                .apply()
            downloadAndInstall(UpdateInfo(pendingBuild, pendingUrl))
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
                "Allow AzMusic to install updates. When you return, the update will continue automatically.",
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
        updaterPrefs.edit().putLong(KEY_PENDING_DOWNLOAD_ID, downloadId).apply()
        Toast.makeText(this, "AzMusic update downloading…", Toast.LENGTH_SHORT).show()

        // Keep a foreground watcher as the primary path. Some Android builds deliver the
        // DownloadManager completion broadcast late, or won't allow the receiver to launch an
        // activity immediately. While AzMusic is visible, poll this exact download and open the
        // installer the moment it becomes successful. The broadcast receiver and onResume()
        // logic below remain as fallbacks.
        watchDownloadAndLaunchInstaller(downloadId)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) != downloadId) return

                runCatching { unregisterReceiver(this) }

                tryLaunchDownloadedUpdate(downloadId)
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

    private fun watchDownloadAndLaunchInstaller(downloadId: Long) {
        lifecycleScope.launch {
            // Stay lightweight: DownloadManager status changes are slow compared with UI frames.
            while (!isFinishing && updaterPrefs.getLong(KEY_PENDING_DOWNLOAD_ID, -1L) == downloadId) {
                if (tryLaunchDownloadedUpdate(downloadId)) return@launch
                delay(500L)
            }
        }
    }

    private fun tryLaunchDownloadedUpdate(downloadId: Long): Boolean {
        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
            ?: return false

        cursor.use {
            if (!it.moveToFirst()) return false
            val statusColumn = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (statusColumn < 0) return false

            when (it.getInt(statusColumn)) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val uri = downloadManager.getUriForDownloadedFile(downloadId) ?: return false
                    updaterPrefs.edit().remove(KEY_PENDING_DOWNLOAD_ID).apply()
                    launchPackageInstaller(uri)
                    return true
                }
                DownloadManager.STATUS_FAILED -> {
                    updaterPrefs.edit().remove(KEY_PENDING_DOWNLOAD_ID).apply()
                    Toast.makeText(
                        this,
                        "Update download failed. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        return false
    }

    private fun launchPackageInstaller(uri: Uri) {
        val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            startActivity(installIntent)
        }.recoverCatching {
            startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }.onFailure {
            Toast.makeText(
                this,
                "Download finished. Tap the completed AzMusic download to install it.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private data class UpdateInfo(
        val buildNumber: Long,
        val apkUrl: String
    )

    private companion object {
        const val KEY_PENDING_DOWNLOAD_ID = "pending_update_download_id"
        const val KEY_PENDING_APK_URL = "pending_update_apk_url"
        const val KEY_PENDING_BUILD_NUMBER = "pending_update_build_number"
    }
}
