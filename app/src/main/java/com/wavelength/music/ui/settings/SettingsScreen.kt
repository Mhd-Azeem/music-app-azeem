package com.wavelength.music.ui.settings

import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wavelength.music.BuildConfig
import com.wavelength.music.R
import com.wavelength.music.playback.EqualizerMode
import com.wavelength.music.playback.EqualizerPreset
import com.wavelength.music.ui.components.CircularKnob
import com.wavelength.music.ui.components.ImageCropDialog
import com.wavelength.music.ui.theme.AppTheme
import com.wavelength.music.ui.theme.swatchColor
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: AppSettingsViewModel = hiltViewModel(),
    equalizerViewModel: EqualizerViewModel = hiltViewModel()
) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val downloadsSummary by viewModel.downloadsSummary.collectAsStateWithLifecycle()
    val usageState by viewModel.usageState.collectAsStateWithLifecycle()
    var showClearDownloadsConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
                    HomeScreenShortcut.pinPhotoAsShortcut(context, bitmap, "Azeem's Music")
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

    val pickBackgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { pendingBackgroundCropUri = it } }

    val pickIconPhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { pendingIconCropUri = it } }

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
                SettingsSection(title = "App icon") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(IconPreset.entries) { preset ->
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
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = "Opacity: ${(settings.backgroundOpacity * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = settings.backgroundOpacity,
                            onValueChange = { viewModel.setBackgroundOpacity(it) },
                            valueRange = 0f..1f
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Default Themes") {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(AppTheme.entries.filter { !it.isGlass }) { theme ->
                            ThemeOption(
                                theme = theme,
                                selected = settings.theme == theme,
                                onClick = { viewModel.selectTheme(theme) }
                            )
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "Liquid Themes") {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(AppTheme.entries.filter { it.isGlass }) { theme ->
                            ThemeOption(
                                theme = theme,
                                selected = settings.theme == theme,
                                onClick = { viewModel.selectTheme(theme) }
                            )
                        }
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
                }
            }

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
                                items(EqualizerPreset.entries) { preset ->
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
                SettingsSection(title = "About") {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.ic_launcher_classic),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                            )
                            Column(modifier = Modifier.padding(start = 12.dp)) {
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
                        }
                        Text(
                            text = "Features",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                        val features = listOf(
                            "Stream and search millions of songs",
                            "Download tracks for offline listening",
                            "Custom playlists with reordering and bulk actions",
                            "Equalizer with bass boost and genre presets",
                            "Sleep timer with countdown or end-of-track modes",
                            "Custom app icon, background, and themes — including Liquid Glass",
                            "Recently played, favorites, and search history"
                        )
                        features.forEach { feature ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text("•  ", color = MaterialTheme.colorScheme.primary)
                                Text(feature, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text(
                            text = "Created and Developed by Mohammed Azeem.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 20.dp, bottom = 16.dp)
                        )
                    }
                }
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
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
private fun IconPresetOption(preset: IconPreset, selected: Boolean, onClick: () -> Unit) {
    val previewRes = when (preset) {
        IconPreset.CLASSIC -> R.drawable.ic_launcher_classic
        IconPreset.HEADPHONES -> R.drawable.ic_launcher_headphones
        IconPreset.VINYL -> R.drawable.ic_launcher_vinyl
        IconPreset.EQUALIZER -> R.drawable.ic_launcher_equalizer
        IconPreset.LETTER_A_PURPLE -> R.drawable.ic_launcher_letter_a_purple
        IconPreset.LETTER_A_DARK -> R.drawable.ic_launcher_letter_a_dark
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
