package com.wavelength.music.ui.settings

import android.net.Uri
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.animate
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wavelength.music.BuildConfig
import com.wavelength.music.R
import com.wavelength.music.playback.EqualizerMode
import com.wavelength.music.playback.FloatingIslandService
import com.wavelength.music.playback.EqualizerPreset
import com.wavelength.music.ui.components.CircularKnob
import com.wavelength.music.ui.components.ImageCropDialog
import com.wavelength.music.ui.theme.AppTheme
import com.wavelength.music.ui.theme.swatchColor
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import com.wavelength.music.data.repository.AlbumArtStyle
import com.wavelength.music.data.repository.BuiltInWallpaper
import com.wavelength.music.data.repository.VisualThemeMode
import com.wavelength.music.ui.components.wallpaperBrush

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onStatisticsClick: () -> Unit = {},
    onEmailActivationClick: () -> Unit = {},
    viewModel: AppSettingsViewModel = hiltViewModel(),
    equalizerViewModel: EqualizerViewModel = hiltViewModel()
) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val downloadsSummary by viewModel.downloadsSummary.collectAsStateWithLifecycle()
    val usageState by viewModel.usageState.collectAsStateWithLifecycle()
    var showClearDownloadsConfirm by remember { mutableStateOf(false) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showAlbumArtStyleMenu by remember { mutableStateOf(false) }
    var showThemeColorPicker by remember { mutableStateOf(false) }
    var expandedSettingsCategory by remember { mutableStateOf<String?>(null) }
    var showGlassTimelineColorPicker by remember { mutableStateOf(false) }
    var showGlassGlowColorPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val floatingIslandPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(context)) {
            viewModel.setFloatingIslandEnabled(true)
            context.startService(Intent(context, FloatingIslandService::class.java))
            Toast.makeText(context, "Floating Island enabled", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.setFloatingIslandEnabled(false)
            Toast.makeText(context, "Display over other apps permission is required", Toast.LENGTH_LONG).show()
        }
    }

    val eqSupported by equalizerViewModel.isSupported.collectAsStateWithLifecycle()
    val eqEnabled by equalizerViewModel.enabled.collectAsStateWithLifecycle()
    val eqBands by equalizerViewModel.bands.collectAsStateWithLifecycle()
    val bassSupported by equalizerViewModel.bassBoostSupported.collectAsStateWithLifecycle()
    val bassStrength by equalizerViewModel.bassBoostStrength.collectAsStateWithLifecycle()
    val eqMode by equalizerViewModel.mode.collectAsStateWithLifecycle()
    val volumeBoostSupported by equalizerViewModel.volumeBoostSupported.collectAsStateWithLifecycle()
    val volumeBoostEnabled by equalizerViewModel.volumeBoostEnabled.collectAsStateWithLifecycle()
    val volumeBoostPercent by equalizerViewModel.volumeBoostPercent.collectAsStateWithLifecycle()

    var pendingBackgroundCropUri by remember { mutableStateOf<Uri?>(null) }
    var pendingIconCropUri by remember { mutableStateOf<Uri?>(null) }
    var pendingFavoriteWallpaperCropUri by remember { mutableStateOf<Uri?>(null) }
    val favoriteWallpapers by viewModel.favoriteWallpapers.collectAsStateWithLifecycle()

    if (showClearDownloadsConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDownloadsConfirm = false },
            title = { Text("Clear all downloads?") },
            text = { Text("This deletes every downloaded song from this device. You can re-download them anytime.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllDownloads()
                    showClearDownloadsConfirm = false
                }) { Text("Clear all") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDownloadsConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showAlbumArtStyleMenu) {
        AlertDialog(
            onDismissRequest = { showAlbumArtStyleMenu = false },
            title = { Text("Album Art Style") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AlbumArtStyle.entries.forEach { style ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setAlbumArtStyle(style)
                                    showAlbumArtStyleMenu = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = style.label,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = style.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (settings.albumArtStyle == style) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAlbumArtStyleMenu = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text("Clear streamed-song cache?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("This removes cached streamed audio only. Downloads, playlists and favorites are not deleted.")
                    CacheSwipeToConfirm(
                        onConfirmed = {
                            viewModel.clearStreamCache()
                            showClearCacheConfirm = false
                            Toast.makeText(context, "Stream cache cleared", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAbout) {
        AboutSheet(onDismiss = { showAbout = false })
    }

    pendingBackgroundCropUri?.let { uri ->
        ImageCropDialog(
            imageUri = uri,
            aspectRatio = 9f / 19.5f,
            onDismiss = { pendingBackgroundCropUri = null },
            onCropped = { bitmap ->
                viewModel.pickBackground(bitmap)
                pendingBackgroundCropUri = null
            }
        )
    }

    pendingIconCropUri?.let { uri ->
        ImageCropDialog(
            imageUri = uri,
            aspectRatio = 1f,
            onDismiss = { pendingIconCropUri = null },
            onCropped = { bitmap ->
                if (HomeScreenShortcut.isSupported(context)) {
                    HomeScreenShortcut.pinPhotoAsShortcut(context, bitmap, "AzMusic")
                } else {
                    Toast.makeText(
                        context,
                        "Your home screen doesn't support pinned shortcuts.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                pendingIconCropUri = null
            }
        )
    }

    pendingFavoriteWallpaperCropUri?.let { uri ->
        ImageCropDialog(
            imageUri = uri,
            aspectRatio = 9f / 19.5f,
            onDismiss = { pendingFavoriteWallpaperCropUri = null },
            onCropped = { bitmap ->
                viewModel.addFavoriteWallpaper(bitmap)
                pendingFavoriteWallpaperCropUri = null
            }
        )
    }

    val pickBackgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { pendingBackgroundCropUri = it } }

    val pickIconPhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { pendingIconCropUri = it } }

    val pickFavoriteWallpaperLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { pendingFavoriteWallpaperCropUri = it } }

    val scope = rememberCoroutineScope()

    val pickFavoriteWallpapersBulkLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val count = viewModel.addFavoriteWallpapers(uris)
                Toast.makeText(context, "Added $count wallpaper${if (count == 1) "" else "s"}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                viewModel.exportBackup(uri).fold(
                    onSuccess = {
                        Toast.makeText(context, "Backup saved", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { e ->
                        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                viewModel.importBackup(uri).fold(
                    onSuccess = { summary ->
                        val parts = mutableListOf(
                            "${summary.favoriteCount} favorites",
                            "${summary.playlistCount} playlists"
                        )
                        if (summary.wallpaperCount > 0) parts += "${summary.wallpaperCount} wallpapers"
                        if (summary.settingsRestored) parts += "settings"
                        Toast.makeText(
                            context,
                            "Restored ${parts.joinToString(", ")}",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    onFailure = { e ->
                        Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(padding)) {
            item {
                SettingsCategoryHeader(
                    title = "Appearance & Interface",
                    subtitle = "App icon, backgrounds, colors, Now Playing and Glass theme",
                    expanded = expandedSettingsCategory == "Appearance & Interface",
                    onClick = {
                        expandedSettingsCategory =
                            if (expandedSettingsCategory == "Appearance & Interface") null else "Appearance & Interface"
                    }
                )
            }
            if (expandedSettingsCategory == "Appearance & Interface") {
            item {
                SettingsSection(title = "App icon") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(IconPreset.entries, key = { it.name }) { preset ->
                            IconPresetOption(
                                preset = preset,
                                selected = settings.iconPreset == preset,
                                onClick = { viewModel.selectIcon(preset) }
                            )
                        }
                    }
                    Text(
                        text = "Changing the icon may take a few seconds to show on your home screen.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    OutlinedButton(
                        onClick = {
                            pickIconPhotoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text("Use a gallery photo instead")
                    }
                    Text(
                        text = "Android can't replace the app's real icon with an arbitrary photo, " +
                            "so this adds a separate pinned icon to your home screen using that " +
                            "photo (your launcher will ask you to confirm).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            item {
                SettingsSection(title = "Background") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (settings.hasCustomBackground) {
                            val backgroundFile = viewModel.customBackgroundFile
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(backgroundFile)
                                    .memoryCacheKey("${backgroundFile.absolutePath}_${backgroundFile.lastModified()}")
                                    .diskCacheKey("${backgroundFile.absolutePath}_${backgroundFile.lastModified()}")
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.bg_default),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Button(onClick = {
                                pickBackgroundLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }) {
                                Text("Choose photo")
                            }
                        }
                        if (settings.hasCustomBackground) {
                            OutlinedButton(onClick = viewModel::resetBackground) {
                                Text("Reset")
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Background opacity: ${(settings.backgroundOpacity * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = settings.backgroundOpacity,
                            onValueChange = viewModel::setBackgroundOpacity,
                            valueRange = 0f..1f
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Built-in Wallpapers") {
                    Text(
                        text = "Gradient and shining backgrounds included with AZ Music.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(BuiltInWallpaper.entries, key = { it.name }) { wallpaper ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { viewModel.setBuiltInWallpaper(wallpaper) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 92.dp, height = 132.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .then(
                                            if (wallpaper == BuiltInWallpaper.DEFAULT) {
                                                Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                            } else {
                                                Modifier.background(wallpaperBrush(wallpaper))
                                            }
                                        )
                                        .border(
                                            width = if (!settings.hasCustomBackground && settings.builtInWallpaper == wallpaper) 2.dp else 1.dp,
                                            color = if (!settings.hasCustomBackground && settings.builtInWallpaper == wallpaper) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outline
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (wallpaper == BuiltInWallpaper.DEFAULT) {
                                        Image(
                                            painter = painterResource(R.drawable.bg_default),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    if (!settings.hasCustomBackground && settings.builtInWallpaper == wallpaper) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                                .padding(3.dp)
                                        )
                                    }
                                }
                                Text(
                                    wallpaper.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "Theme Accent Color") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { showThemeColorPicker = !showThemeColorPicker }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(settings.customAccentArgb))
                                    .border(2.dp, Color.White.copy(alpha = 0.65f), CircleShape)
                            )
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(text = "#%08X".format(settings.customAccentArgb))
                                Text(
                                    text = if (showThemeColorPicker) {
                                        "Tap to collapse color picker"
                                    } else {
                                        "Tap to customize all theme colors"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { showThemeColorPicker = !showThemeColorPicker }) {
                            Icon(Icons.Filled.ExpandMore, contentDescription = "Expand theme color picker")
                        }
                        TextButton(onClick = { viewModel.setCustomAccentArgb(0xFF22D3EE.toInt()) }) {
                            Text("Reset")
                        }
                    }
                    if (showThemeColorPicker) {
                        Text(
                            text = "Tap or drag anywhere in the color box. This accent applies to both solid and Liquid themes.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        LiquidColorPicker(
                            selectedArgb = settings.customAccentArgb,
                            onColorSelected = viewModel::setCustomAccentArgb,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Floating Island") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AzMusic Floating Island")
                            Text(
                                "Show a draggable mini Now Playing island above other apps. Tap it to expand playback controls.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.floatingIslandEnabled,
                            onCheckedChange = { enabled ->
                                if (!enabled) {
                                    viewModel.setFloatingIslandEnabled(false)
                                    context.stopService(Intent(context, FloatingIslandService::class.java))
                                } else if (Settings.canDrawOverlays(context)) {
                                    viewModel.setFloatingIslandEnabled(true)
                                    context.startService(Intent(context, FloatingIslandService::class.java))
                                } else {
                                    floatingIslandPermissionLauncher.launch(
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    )
                                }
                            }
                        )
                    }
                    Text(
                        "Android will ask for Display over other apps permission the first time. This is AzMusic's own overlay and does not replace your phone's native Live Island.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }



            item {
                SettingsSection(title = "Favorite Wallpapers") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Save a few pictures here to switch your background instantly, " +
                                "without picking and cropping from your gallery each time.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            pickFavoriteWallpapersBulkLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }) {
                            Text("Add multiple")
                        }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        pickFavoriteWallpaperLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add favorite wallpaper")
                            }
                        }
                        items(favoriteWallpapers, key = { it.absolutePath }) { file ->
                            Box(modifier = Modifier.size(72.dp)) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(file)
                                        .memoryCacheKey("${file.absolutePath}_${file.lastModified()}")
                                        .diskCacheKey("${file.absolutePath}_${file.lastModified()}")
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.applyFavoriteWallpaper(file) }
                                )
                                IconButton(
                                    onClick = { viewModel.removeFavoriteWallpaper(file) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.45f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove favorite wallpaper",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "Theme Style") {
                    Text(
                        text = "Choose the complete visual style for AzMusic.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    VisualThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewModel.setVisualThemeMode(mode) }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.visualThemeMode == mode,
                                onClick = { viewModel.setVisualThemeMode(mode) }
                            )
                            Column(modifier = Modifier.padding(start = 6.dp).weight(1f)) {
                                Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    mode.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Animate theme transitions")
                            Text(
                                "Crossfade theme and accent colors instead of changing instantly",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.animateThemeTransitions,
                            onCheckedChange = viewModel::setAnimateThemeTransitions
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Now Playing") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Expand \"Up next\" to half screen when scrolled",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = settings.expandUpNextOnScroll,
                            onCheckedChange = { viewModel.setExpandUpNextOnScroll(it) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dynamic accent color from album art",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = settings.dynamicThemeFromAlbumArt,
                            onCheckedChange = { viewModel.setDynamicThemeFromAlbumArt(it) }
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Album art style",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = settings.albumArtStyle.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )
                        OutlinedButton(
                            onClick = { showAlbumArtStyleMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = settings.albumArtStyle.label,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Start
                            )
                            Icon(Icons.Filled.ExpandMore, contentDescription = "Choose album art style")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Animate album art/title when the track changes",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = settings.trackTransitionEnabled,
                            onCheckedChange = { viewModel.setTrackTransitionEnabled(it) }
                        )
                    }
                    if (settings.trackTransitionEnabled) {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Text(
                                text = "Transition speed: ${settings.trackTransitionDurationMs}ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = settings.trackTransitionDurationMs.toFloat(),
                                onValueChange = { viewModel.setTrackTransitionDurationMs(it.roundToInt()) },
                                valueRange = 100f..1000f
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sync volume slider with system volume",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = settings.syncVolumeWithSystem,
                            onCheckedChange = { viewModel.setSyncVolumeWithSystem(it) }
                        )
                    }
                    Text(
                        text = "When on, the Now Playing volume slider controls the same volume as " +
                            "your device's hardware buttons. When off, it's a separate in-app level.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            item {
                SettingsSection(title = "Neomorphism Theme") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Full App Neomorphism")
                            Text(
                                text = if (settings.neomorphismEnabled) {
                                    "On • soft raised surfaces, rounded tiles and depth shadows"
                                } else {
                                    "Off • use the selected standard appearance"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.neomorphismEnabled,
                            onCheckedChange = viewModel::setNeomorphismEnabled
                        )
                    }
                    Text(
                        text = "Applies a soft dark Neomorphism style across Home, Search, Library, Settings and player surfaces. Enabling it turns Glassmorphism off so the two depth systems do not conflict.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (settings.neomorphismEnabled) Color(0xFF252B36)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                1.dp,
                                if (settings.neomorphismEnabled) Color.White.copy(alpha = 0.10f)
                                else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(24.dp)
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            Text(
                                "Neomorphic preview",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "Soft elevated card • subtle highlight • deep rounded surface",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "Now Playing Style") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Glassmorphism Now Playing")
                            Text(
                                text = if (settings.glassmorphismNowPlaying) {
                                    "On • use the Glassmorphism player with your current theme"
                                } else {
                                    "Off • use the Now Playing style of the selected theme"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.glassmorphismNowPlaying,
                            onCheckedChange = viewModel::setGlassmorphismNowPlaying
                        )
                    }
                    Text(
                        text = "Keeps your selected app theme unchanged and applies the Glassmorphism reference style only to Now Playing, including its curved queue, seek arc and connected transport control.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    Text(
                        text = if (settings.glassmorphismNowPlaying) "Glass timeline colors" else "Glass timeline colors • enable Glassmorphism to edit",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (settings.glassmorphismNowPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = settings.glassmorphismNowPlaying) {
                                showGlassTimelineColorPicker = !showGlassTimelineColorPicker
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(settings.glassTimelineArgb))
                                .border(1.5.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(
                                "Timeline color",
                                color = if (settings.glassmorphismNowPlaying) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Base curved seek line",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            enabled = settings.glassmorphismNowPlaying,
                            onClick = { viewModel.setGlassTimelineArgb(0xFF14B8E6.toInt()) }
                        ) { Text("Reset") }
                    }
                    if (settings.glassmorphismNowPlaying && showGlassTimelineColorPicker) {
                        LiquidColorPicker(
                            selectedArgb = settings.glassTimelineArgb,
                            onColorSelected = viewModel::setGlassTimelineArgb,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = settings.glassmorphismNowPlaying) {
                                showGlassGlowColorPicker = !showGlassGlowColorPicker
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(settings.glassPlayedGlowArgb))
                                .border(1.5.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(
                                "Played portion glow",
                                color = if (settings.glassmorphismNowPlaying) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Glow and bright border around the played arc",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            enabled = settings.glassmorphismNowPlaying,
                            onClick = { viewModel.setGlassPlayedGlowArgb(0xFF54E8FF.toInt()) }
                        ) { Text("Reset") }
                    }
                    if (settings.glassmorphismNowPlaying && showGlassGlowColorPicker) {
                        LiquidColorPicker(
                            selectedArgb = settings.glassPlayedGlowArgb,
                            onColorSelected = viewModel::setGlassPlayedGlowArgb,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            }

            item {
                SettingsCategoryHeader(
                    title = "Playback",
                    subtitle = "Queue behavior, gapless playback, crossfade and visualizer",
                    expanded = expandedSettingsCategory == "Playback",
                    onClick = {
                        expandedSettingsCategory =
                            if (expandedSettingsCategory == "Playback") null else "Playback"
                    }
                )
            }
            if (expandedSettingsCategory == "Playback") {
            item {
                SettingsSection(title = "Smart Queue") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Automatically continue Up Next", modifier = Modifier.weight(1f))
                        Switch(
                            checked = settings.aiDjEnabled,
                            onCheckedChange = { viewModel.setAiDjEnabled(it) }
                        )
                    }
                    Text(
                        text = "When the queue is about to run out, automatically adds more songs " +
                            "similar to what you've been listening to, so playback never stops.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            item {
                SettingsSection(title = "Gapless Playback") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Seamless track transitions")
                            Text(
                                text = if (settings.gaplessPlaybackEnabled) {
                                    "On • next track starts without an added pause"
                                } else {
                                    "Off • adds a short separation between tracks"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.gaplessPlaybackEnabled,
                            onCheckedChange = viewModel::setGaplessPlaybackEnabled
                        )
                    }
                    if (settings.crossfadeDurationMs > 0) {
                        Text(
                            text = "Crossfade takes priority while it is enabled.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Crossfade") {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Text(
                            text = if (settings.crossfadeDurationMs == 0) {
                                "Off"
                            } else {
                                "${settings.crossfadeDurationMs / 1000f}s"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = settings.crossfadeDurationMs.toFloat(),
                            onValueChange = { viewModel.setCrossfadeDurationMs(it.roundToInt()) },
                            valueRange = 0f..8000f,
                            steps = 7
                        )
                        Text(
                            text = "Fades the ending track out and the next one in, instead of " +
                                "switching abruptly.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Audio Visualizer") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show a waveform on Now Playing", modifier = Modifier.weight(1f))
                        Switch(
                            checked = settings.audioVisualizerEnabled,
                            onCheckedChange = { viewModel.setAudioVisualizerEnabled(it) }
                        )
                    }
                    Text(
                        text = "Needs microphone access to read the playback audio (nothing is " +
                            "recorded or leaves your device).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            }

            item {
                SettingsCategoryHeader(
                    title = "Library & Data",
                    subtitle = "Downloads, listening statistics, backup and restore",
                    expanded = expandedSettingsCategory == "Library & Data",
                    onClick = {
                        expandedSettingsCategory =
                            if (expandedSettingsCategory == "Library & Data") null else "Library & Data"
                    }
                )
            }
            if (expandedSettingsCategory == "Library & Data") {
            item {
                SettingsSection(title = "Downloads") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (downloadsSummary.count == 0) {
                                "No downloads yet"
                            } else {
                                "${downloadsSummary.count} songs · ${formatStorageSize(downloadsSummary.totalSizeBytes)}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (downloadsSummary.count > 0) {
                            OutlinedButton(onClick = { showClearDownloadsConfirm = true }) {
                                Text("Clear all")
                            }
                        }
                    }
                }
            }


            item {
                SettingsSection(title = "Stream Cache") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = when (settings.streamCacheLimitMb) {
                                0 -> "Cache limit: Off (0 MB)"
                                1024 -> "Cache limit: 1.0 GB"
                                else -> "Cache limit: ${settings.streamCacheLimitMb} MB"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = settings.streamCacheLimitMb.toFloat(),
                            onValueChange = { raw ->
                                val snapped = ((raw / 64f).roundToInt() * 64).coerceIn(0, 1024)
                                viewModel.setStreamCacheLimitMb(snapped)
                            },
                            valueRange = 0f..1024f,
                            steps = 15
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "0 MB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "1 GB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Limits persistent streamed-song cache storage. Lowering the limit immediately evicts older cached audio. 0 MB disables retained stream caching.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
                        )
                        OutlinedButton(
                            onClick = { showClearCacheConfirm = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Clear cache")
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "Statistics") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onStatisticsClick)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("View listening statistics", modifier = Modifier.weight(1f))
                    }
                }
            }

            item {
                SettingsSection(title = "Backup & Restore") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = { exportBackupLauncher.launch("wavelength_backup.json") }) {
                            Text("Export backup")
                        }
                        OutlinedButton(onClick = { importBackupLauncher.launch(arrayOf("application/json")) }) {
                            Text("Restore backup")
                        }
                    }
                    Text(
                        text = "Saves your playlists and favorites to a file you choose — back it up " +
                            "anywhere (Drive, email, a computer) and restore it after reinstalling, on " +
                            "any version of the app.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            }

            item {
                SettingsCategoryHeader(
                    title = "Audio",
                    subtitle = "Equalizer, bass boost and volume booster",
                    expanded = expandedSettingsCategory == "Audio",
                    onClick = {
                        expandedSettingsCategory =
                            if (expandedSettingsCategory == "Audio") null else "Audio"
                    }
                )
            }
            if (expandedSettingsCategory == "Audio") {
            item {
                SettingsSection(title = "Equalizer") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable equalizer", modifier = Modifier.weight(1f))
                        if (eqSupported || bassSupported) {
                            TextButton(onClick = { equalizerViewModel.reset() }) {
                                Text("Reset")
                            }
                        }
                        Switch(
                            checked = eqEnabled,
                            onCheckedChange = { equalizerViewModel.setEnabled(it) },
                            enabled = eqSupported || bassSupported
                        )
                    }
                    if (!eqSupported && !bassSupported) {
                        Text(
                            text = "Play a song first to set up the equalizer — some devices don't support it.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = eqMode == EqualizerMode.SIMPLE,
                                onClick = { equalizerViewModel.setMode(EqualizerMode.SIMPLE) },
                                label = { Text("Simple") }
                            )
                            FilterChip(
                                selected = eqMode == EqualizerMode.ADVANCED,
                                onClick = { equalizerViewModel.setMode(EqualizerMode.ADVANCED) },
                                label = { Text("Advanced") }
                            )
                        }

                        if (eqBands.isNotEmpty()) {
                            Text(
                                text = "Presets",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(EqualizerPreset.entries, key = { it.name }) { preset ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { equalizerViewModel.applyPreset(preset) },
                                        label = { Text(preset.label) }
                                    )
                                }
                            }
                        }

                        if (eqMode == EqualizerMode.SIMPLE) {
                            val simpleBands = listOfNotNull(
                                eqBands.firstOrNull()?.let { "Bass" to it },
                                eqBands.getOrNull(eqBands.size / 2)?.let { "Vocals" to it },
                                eqBands.lastOrNull()?.let { "Treble" to it }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                simpleBands.forEach { (label, band) ->
                                    CircularKnob(
                                        label = label,
                                        valueLabel = "${if (band.levelMillibel >= 0) "+" else ""}${band.levelMillibel / 100}dB",
                                        value = band.levelMillibel.toFloat(),
                                        valueRange = band.minLevelMillibel.toFloat()..band.maxLevelMillibel.toFloat(),
                                        onValueChange = { equalizerViewModel.setBandLevel(band.index, it.toInt()) },
                                        enabled = eqEnabled
                                    )
                                }
                            }
                        } else {
                            eqBands.forEach { band ->
                                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                    Text(
                                        text = formatBandFrequency(band.centerFreqHz),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Slider(
                                        value = band.levelMillibel.toFloat(),
                                        onValueChange = {
                                            equalizerViewModel.setBandLevel(band.index, it.toInt())
                                        },
                                        valueRange = band.minLevelMillibel.toFloat()..band.maxLevelMillibel.toFloat(),
                                        enabled = eqEnabled
                                    )
                                }
                            }
                        }

                        if (bassSupported) {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                Text(
                                    text = "Bass boost",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Slider(
                                    value = bassStrength.toFloat(),
                                    onValueChange = { equalizerViewModel.setBassBoostStrength(it.toInt()) },
                                    valueRange = 0f..1000f,
                                    enabled = eqEnabled
                                )
                            }
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "Volume Booster") {
                    if (!volumeBoostSupported) {
                        Text(
                            text = "Play a song first to set up the volume booster — some devices don't support it.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable volume booster", modifier = Modifier.weight(1f))
                            Switch(
                                checked = volumeBoostEnabled,
                                onCheckedChange = { equalizerViewModel.setVolumeBoostEnabled(it) }
                            )
                        }
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Text(
                                text = "Boost: $volumeBoostPercent%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = volumeBoostPercent.toFloat(),
                                onValueChange = { equalizerViewModel.setVolumeBoostPercent(it.roundToInt()) },
                                valueRange = 100f..400f,
                                enabled = volumeBoostEnabled
                            )
                            Text(
                                text = "Boosting past 100% can distort audio, especially at higher device volume.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            }

            item {
                SettingsCategoryHeader(
                    title = "System & Account",
                    subtitle = "Cloudflare usage, email activation and app information",
                    expanded = expandedSettingsCategory == "System & Account",
                    onClick = {
                        expandedSettingsCategory =
                            if (expandedSettingsCategory == "System & Account") null else "System & Account"
                    }
                )
            }
            if (expandedSettingsCategory == "System & Account") {
            item {
                SettingsSection(title = "Cloudflare Usage") {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        when {
                            usageState.isLoading -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Text(
                                        text = "Checking usage…",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                }
                            }
                            usageState.error != null -> {
                                Text(
                                    text = usageState.error.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = { viewModel.refreshUsage() },
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text("Retry")
                                }
                            }
                            else -> {
                                val used = usageState.used ?: 0
                                val limit = usageState.limit ?: 1
                                val fraction = (used.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${used.formatThousands()} / ${limit.formatThousands()} requests today",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    IconButton(onClick = { viewModel.refreshUsage() }) {
                                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh usage")
                                    }
                                }
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                )
                                usageState.date?.let { date ->
                                    Text(
                                        text = "As of $date (UTC)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onEmailActivationClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Email Activation",
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }

            item {
                SettingsSection(title = "About") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { showAbout = true })
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_classic),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                        )
                        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Version ${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ExpandMore,
                            contentDescription = "Show app details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            }

            item {
                Text(
                    text = "CREATED AND DEVELOPED BY MOHAMMED AZEEM©",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun CacheSwipeToConfirm(onConfirmed: () -> Unit) {
    var offsetPx by remember { mutableFloatStateOf(0f) }
    var maxOffsetPx by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(29.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .onSizeChanged { size ->
                maxOffsetPx = (size.width - size.height.toFloat()).coerceAtLeast(0f)
                offsetPx = offsetPx.coerceIn(0f, maxOffsetPx)
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "Swipe to clear cache  →",
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge
        )
        androidx.compose.material3.Surface(
            modifier = Modifier
                .padding(4.dp)
                .size(50.dp)
                .offset { IntOffset(offsetPx.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offsetPx = (offsetPx + delta).coerceIn(0f, maxOffsetPx)
                    },
                    onDragStopped = {
                        if (maxOffsetPx > 0f && offsetPx >= maxOffsetPx * 0.85f) {
                            offsetPx = maxOffsetPx
                            onConfirmed()
                        } else {
                            val start = offsetPx
                            scope.launch {
                                animate(start, 0f) { value, _ -> offsetPx = value }
                            }
                        }
                    }
                ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "✓",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

private fun formatBandFrequency(hz: Int): String =
    if (hz >= 1000) "%.1f kHz".format(hz / 1000.0) else "$hz Hz"

private fun formatStorageSize(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

private fun Int.formatThousands(): String = "%,d".format(this)

@Composable
private fun SettingsCategoryHeader(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.82f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (expanded) "Collapse $title" else "Expand $title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 28.dp, top = 12.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
        )
    }
}

@Composable
private fun IconPresetOption(preset: IconPreset, selected: Boolean, onClick: () -> Unit) {
    val previewRes = when (preset) {
        IconPreset.CLASSIC -> R.drawable.ic_launcher_classic
        IconPreset.HEADPHONES -> R.drawable.ic_launcher_headphones
        IconPreset.VINYL -> R.drawable.ic_launcher_vinyl
        IconPreset.EQUALIZER -> R.drawable.ic_launcher_equalizer
        IconPreset.WAVEFORM -> R.drawable.ic_launcher_waveform
        IconPreset.NOTE -> R.drawable.ic_launcher_note
        IconPreset.PLAY_BUTTON -> R.drawable.ic_launcher_play_button
        IconPreset.LETTER_A_PURPLE -> R.drawable.ic_launcher_letter_a_purple
        IconPreset.LETTER_A_DARK -> R.drawable.ic_launcher_letter_a_dark
        IconPreset.LETTER_A_BOLD -> R.drawable.ic_launcher_letter_a_bold
        IconPreset.LETTER_A_ROUNDED -> R.drawable.ic_launcher_letter_a_rounded
        IconPreset.LETTER_A_MONO -> R.drawable.ic_launcher_letter_a_mono
        IconPreset.MONOGRAM_AZ -> R.drawable.ic_launcher_monogram_az
    }
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(previewRes),
            contentDescription = preset.label,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = if (selected) 2.dp else 0.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                )
        )
        Text(preset.label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ThemeOption(theme: AppTheme, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(theme.swatchColor())
                .border(
                    width = if (selected) 2.dp else 0.dp,
                    color = Color.White,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
            }
        }
        Text(theme.label, style = MaterialTheme.typography.labelSmall)
    }
}


@Composable
private fun LiquidColorPicker(
    selectedArgb: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialHsv = remember { FloatArray(3).also { AndroidColor.colorToHSV(selectedArgb, it) } }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    var alpha by remember { mutableFloatStateOf(AndroidColor.alpha(selectedArgb) / 255f) }
    var hexText by remember { mutableStateOf("#%06X".format(selectedArgb and 0xFFFFFF)) }

    fun emitColor() {
        val rgb = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
        val argb = AndroidColor.argb((alpha.coerceIn(0f, 1f) * 255f).roundToInt(), AndroidColor.red(rgb), AndroidColor.green(rgb), AndroidColor.blue(rgb))
        hexText = "#%06X".format(argb and 0xFFFFFF)
        onColorSelected(argb)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(190.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Canvas(
                modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        fun update(pos: Offset) {
                            saturation = (pos.x / size.width).coerceIn(0f, 1f)
                            value = (1f - pos.y / size.height).coerceIn(0f, 1f)
                            emitColor()
                        }
                        detectDragGestures(onDragStart = { update(it) }, onDrag = { change, _ -> update(change.position) })
                    }
            ) {
                val pureHue = Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                drawRect(brush = Brush.horizontalGradient(listOf(Color.White, pureHue)))
                drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                val center = Offset(saturation * size.width, (1f - value) * size.height)
                drawCircle(Color.White, 9.dp.toPx(), center)
                drawCircle(Color.Black, 6.dp.toPx(), center)
                drawCircle(Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))), 4.dp.toPx(), center)
            }
            Canvas(
                modifier = Modifier.width(30.dp).fillMaxSize().clip(RoundedCornerShape(4.dp))
                    .pointerInput(Unit) {
                        fun update(y: Float) { hue = (y / size.height).coerceIn(0f, 1f) * 360f; emitColor() }
                        detectDragGestures(onDragStart = { update(it.y) }, onDrag = { change, _ -> update(change.position.y) })
                    }
            ) {
                drawRect(brush = Brush.verticalGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                val y = (hue / 360f) * size.height
                drawRect(Color.White, Offset(0f, (y - 3.dp.toPx()).coerceAtLeast(0f)), androidx.compose.ui.geometry.Size(size.width, 6.dp.toPx()))
                drawRect(Color.Black, Offset(0f, (y - 1.dp.toPx()).coerceAtLeast(0f)), androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx()))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = hexText,
                onValueChange = { input ->
                    hexText = input.uppercase().filter { it == '#' || it in '0'..'9' || it in 'A'..'F' }.take(7)
                    val hex = hexText.removePrefix("#")
                    if (hex.length == 6) hex.toLongOrNull(16)?.let { rgb ->
                        val parsed = (0xFF000000L or rgb).toInt()
                        val parsedHsv = FloatArray(3).also { AndroidColor.colorToHSV(parsed, it) }
                        hue = parsedHsv[0]; saturation = parsedHsv[1]; value = parsedHsv[2]; emitColor()
                    }
                },
                label = { Text("HEX") }, singleLine = true, modifier = Modifier.weight(1f)
            )
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color(selectedArgb)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)))
        }
        Text(text = "Opacity " + (alpha * 100).roundToInt() + "%", style = MaterialTheme.typography.labelMedium)
        Canvas(
            modifier = Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(5.dp))
                .pointerInput(Unit) {
                    fun update(x: Float) { alpha = (x / size.width).coerceIn(0f, 1f); emitColor() }
                    detectDragGestures(onDragStart = { update(it.x) }, onDrag = { change, _ -> update(change.position.x) })
                }
        ) {
            val tile = 10.dp.toPx(); var yy = 0f; var row = 0
            while (yy < size.height) { var xx = 0f; var col = 0
                while (xx < size.width) { drawRect(if ((row + col) % 2 == 0) Color.LightGray else Color.White, Offset(xx, yy), androidx.compose.ui.geometry.Size(tile, tile)); xx += tile; col++ }
                yy += tile; row++
            }
            val opaque = Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value)))
            drawRect(brush = Brush.horizontalGradient(listOf(Color.Transparent, opaque)))
            val x = alpha * size.width
            drawRect(Color.White, Offset((x - 2.dp.toPx()).coerceIn(0f, size.width - 4.dp.toPx()), 0f), androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height))
        }
    }
}
