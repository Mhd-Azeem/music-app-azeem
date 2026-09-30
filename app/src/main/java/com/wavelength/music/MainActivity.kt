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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateColor
import androidx.compose.foundation.background
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
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
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.Coil
import coil.request.ImageRequest
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.wavelength.music.ui.components.OfflineBanner
import com.wavelength.music.playback.FloatingIslandService
import com.wavelength.music.data.repository.VisualThemeMode
import com.wavelength.music.ui.nowplaying.PlayerViewModel
import com.wavelength.music.ui.navigation.WavelengthNavHost
import com.wavelength.music.ui.settings.AppSettingsViewModel
import com.wavelength.music.ui.splash.AzMusicSplashScreen
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

    override fun onStart() {
        super.onStart()
        getSharedPreferences(FloatingIslandService.VISIBILITY_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(FloatingIslandService.KEY_APP_VISIBLE, true).apply()
        sendBroadcast(
            Intent(FloatingIslandService.ACTION_APP_FOREGROUND).setPackage(packageName)
        )
    }

    override fun onStop() {
        getSharedPreferences(FloatingIslandService.VISIBILITY_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(FloatingIslandService.KEY_APP_VISIBLE, false).apply()
        sendBroadcast(
            Intent(FloatingIslandService.ACTION_APP_BACKGROUND).setPackage(packageName)
        )
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            val settingsViewModel: AppSettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.state.collectAsStateWithLifecycle()
            val playerViewModel: PlayerViewModel = hiltViewModel()
            val playbackState by playerViewModel.state.collectAsStateWithLifecycle()
            var albumAdaptiveAccent by remember { mutableStateOf<Color?>(null) }

            LaunchedEffect(
                playbackState.currentTrack?.albumArtUrl,
                settings.visualThemeMode
            ) {
                albumAdaptiveAccent = if (settings.visualThemeMode == VisualThemeMode.ALBUM_ADAPTIVE) {
                    loadAppAlbumAccent(this@MainActivity, playbackState.currentTrack?.albumArtUrl)
                } else {
                    null
                }
            }
            var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
            var updateDismissed by remember { mutableStateOf(false) }
            var showSplash by remember { mutableStateOf(savedInstanceState == null) }

            LaunchedEffect(Unit) {
                updateInfo = checkForUpdate()
            }

            LaunchedEffect(showSplash) {
                if (!showSplash && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            val targetAccent = albumAdaptiveAccent ?: Color(settings.customAccentArgb)
            val animatedAccent by animateColorAsState(
                targetValue = targetAccent,
                animationSpec = tween(if (settings.animateThemeTransitions) 500 else 0),
                label = "appThemeAccent"
            )

            WavelengthTheme(
                theme = settings.theme,
                customLiquidAccent = animatedAccent,
                glassmorphismEnabled = settings.visualThemeMode == VisualThemeMode.GLASSMORPHISM,
                neomorphismEnabled = settings.visualThemeMode == VisualThemeMode.NEOMORPHISM,
                amoledEnabled = settings.visualThemeMode == VisualThemeMode.AMOLED,
                visualThemeMode = settings.visualThemeMode,
                animateTransitions = settings.animateThemeTransitions
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    OfflineBanner()
                    val aurora = rememberInfiniteTransition(label = "auroraTheme")
                    val auroraA by aurora.animateColor(
                        initialValue = Color(0xFF0A2342),
                        targetValue = Color(0xFF4C1D95),
                        animationSpec = infiniteRepeatable(
                            animation = tween(4200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "auroraA"
                    )
                    val auroraB by aurora.animateColor(
                        initialValue = Color(0xFF0E7490),
                        targetValue = Color(0xFF312E81),
                        animationSpec = infiniteRepeatable(
                            animation = tween(5200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "auroraB"
                    )

                    val bgTopTarget = when (settings.visualThemeMode) {
                        VisualThemeMode.GLASSMORPHISM -> animatedAccent.copy(alpha = 0.32f)
                        VisualThemeMode.NEOMORPHISM -> Color(0xFF303846)
                        VisualThemeMode.AMOLED -> Color.Black
                        VisualThemeMode.ALBUM_ADAPTIVE -> animatedAccent.copy(alpha = 0.42f)
                        VisualThemeMode.AURORA -> auroraA
                        VisualThemeMode.LIQUID -> animatedAccent.copy(alpha = 0.22f)
                        VisualThemeMode.SOLID -> Color.Black
                    }
                    val bgBottomTarget = when (settings.visualThemeMode) {
                        VisualThemeMode.GLASSMORPHISM -> Color(0xFF0A1628)
                        VisualThemeMode.NEOMORPHISM -> Color(0xFF1B2029)
                        VisualThemeMode.AMOLED -> Color.Black
                        VisualThemeMode.ALBUM_ADAPTIVE -> Color(0xFF080B12)
                        VisualThemeMode.AURORA -> auroraB
                        VisualThemeMode.LIQUID -> Color(0xFF101522)
                        VisualThemeMode.SOLID -> Color.Black
                    }
                    val transitionMs = if (settings.animateThemeTransitions) 520 else 0
                    val bgTop by animateColorAsState(bgTopTarget, tween(transitionMs), label = "themeBgTop")
                    val bgBottom by animateColorAsState(bgBottomTarget, tween(transitionMs), label = "themeBgBottom")
                    val appSurface = Modifier.background(
                        Brush.verticalGradient(listOf(bgTop, bgBottom))
                    )
                    Box(modifier = Modifier.fillMaxWidth().weight(1f).then(appSurface)) {
                        when (settings.visualThemeMode) {
                            VisualThemeMode.LIQUID -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    animatedAccent.copy(alpha = 0.22f),
                                                    Color.Transparent
                                                ),
                                                radius = 900f
                                            )
                                        )
                                )
                            }
                            VisualThemeMode.GLASSMORPHISM -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.08f),
                                                    animatedAccent.copy(alpha = 0.12f),
                                                    Color.Transparent
                                                ),
                                                radius = 760f
                                            )
                                        )
                                )
                            }
                            VisualThemeMode.NEOMORPHISM -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    Color.White.copy(alpha = 0.035f),
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.18f)
                                                )
                                            )
                                        )
                                )
                            }
                            VisualThemeMode.ALBUM_ADAPTIVE -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    animatedAccent.copy(alpha = 0.38f),
                                                    animatedAccent.copy(alpha = 0.10f),
                                                    Color.Transparent
                                                ),
                                                radius = 1050f
                                            )
                                        )
                                )
                            }
                            VisualThemeMode.AURORA -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    auroraA.copy(alpha = 0.56f),
                                                    Color.Transparent,
                                                    auroraB.copy(alpha = 0.52f)
                                                )
                                            )
                                        )
                                )
                            }
                            VisualThemeMode.AMOLED, VisualThemeMode.SOLID -> Unit
                        }
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

                if (showSplash) {
                    AzMusicSplashScreen(
                        onFinished = { showSplash = false }
                    )
                }
                }
            }
        }
    }

    private suspend fun loadAppAlbumAccent(context: Context, url: String?): Color? {
        if (url.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .build()
                val drawable = Coil.imageLoader(context).execute(request).drawable ?: return@runCatching null
                val bitmap = drawable.toBitmap()
                val palette = Palette.from(bitmap).generate()
                val fallback = palette.dominantSwatch?.rgb ?: return@runCatching null
                Color(
                    palette.vibrantSwatch?.rgb
                        ?: palette.lightVibrantSwatch?.rgb
                        ?: palette.mutedSwatch?.rgb
                        ?: fallback
                )
            }.getOrNull()
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
