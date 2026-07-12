package com.wavelength.music.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wavelength.music.BuildConfig
import com.wavelength.music.R

private val featureGroups = listOf(
    "Playback" to listOf(
        "Stream millions of songs via JioSaavn, plus play files already on your device",
        "Gapless playback with adjustable crossfade between tracks",
        "Adjustable playback speed",
        "Sleep timer — countdown or end-of-track",
        "Synced lyrics, shown alongside the track",
        "Audio visualizer on Now Playing",
        "AI DJ — keeps the queue topped up automatically",
        "Smart shuffle weighted by your listening history",
        "Drag-to-reorder Up Next queue",
        "Volume slider, optionally synced with your device's system volume"
    ),
    "Library & organization" to listOf(
        "Custom playlists with folders and drag-to-reorder",
        "Smart playlists — Most Played, Recently Added",
        "Daily Mix, built from your top artists",
        "Favorites, recently played, and listening statistics",
        "Offline downloads with a storage/usage view",
        "Share playlists with a QR code",
        "Multi-select for bulk actions in your library"
    ),
    "Search & discovery" to listOf(
        "Search history and voice search",
        "Browse by artist and by genre/language",
        "Paginated search results with infinite scroll"
    ),
    "Personalization" to listOf(
        "Multiple app icon presets",
        "Custom background photo, plus a gallery of favorite wallpapers",
        "Liquid Glass, dynamic (album-art-based), and vinyl-style themes",
        "Adjustable background opacity",
        "Configurable track-change animation, on/off with adjustable speed"
    ),
    "Sound" to listOf(
        "Full equalizer with genre presets (Rock, Pop, Classical, Jazz, and more)",
        "Bass boost and volume booster",
        "Simple mode with circular Bass/Treble/Vocals knobs"
    ),
    "Convenience" to listOf(
        "Home screen widget",
        "Swipe gestures — collapse Now Playing, skip tracks, switch between Home/Search/Library",
        "Pull-to-refresh on Home",
        "Local backup and restore — playlists, favorites, settings, and wallpapers",
        "Offline mode banner when there's no connection"
    )
)

/** Pops out over Settings rather than living inline in the list — the full feature rundown is
 * long enough that always showing it would push everything else in Settings further down. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
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
            featureGroups.forEach { (group, items) ->
                Text(
                    text = group,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
                items.forEach { feature ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("•  ", color = MaterialTheme.colorScheme.primary)
                        Text(feature, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text(
                text = "Created and Developed by Mohammed Azeem.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 20.dp, bottom = 24.dp)
            )
        }
    }
}
