package com.wavelength.music.ui.settings

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import coil.compose.AsyncImage
import com.wavelength.music.R
import com.wavelength.music.ui.theme.AppTheme
import com.wavelength.music.ui.theme.swatchColor

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: AppSettingsViewModel = hiltViewModel(),
    equalizerViewModel: EqualizerViewModel = hiltViewModel()
) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val downloadsSummary by viewModel.downloadsSummary.collectAsStateWithLifecycle()
    var showClearDownloadsConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val eqSupported by equalizerViewModel.isSupported.collectAsStateWithLifecycle()
    val eqEnabled by equalizerViewModel.enabled.collectAsStateWithLifecycle()
    val eqBands by equalizerViewModel.bands.collectAsStateWithLifecycle()
    val bassSupported by equalizerViewModel.bassBoostSupported.collectAsStateWithLifecycle()
    val bassStrength by equalizerViewModel.bassBoostStrength.collectAsStateWithLifecycle()

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

    val pickBackgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.pickBackground(it) } }

    val pickIconPhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (HomeScreenShortcut.isSupported(context)) {
            HomeScreenShortcut.pinPhotoAsShortcut(context, uri, "Azeem's Music")
        } else {
            Toast.makeText(
                context,
                "Your home screen doesn't support pinned shortcuts.",
                Toast.LENGTH_LONG
            ).show()
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
                SettingsSection(title = "App icon") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconPreset.entries.forEach { preset ->
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
                            AsyncImage(
                                model = viewModel.customBackgroundFile,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.ic_launcher_photo),
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
                }
            }

            item {
                SettingsSection(title = "Theme") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AppTheme.entries.forEach { theme ->
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
        IconPreset.PHOTO_1 -> R.drawable.ic_launcher_photo
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
