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

private val latestUpdates = listOf(
    "Fixed Glass Up Next motion so songs continuously rotate around the circular fan while scrolling, with the separator realigned to the fan baseline",
    "Rebuilt Glass Now Playing to the supplied reference: yellow album-art Up Next cards in a circular swipe fan, one large curved frosted player panel, arched seek line, and compact connected transport",
    "Corrected Glass Now Playing geometry: clean single separator, unclipped circular Up Next fan, separated seek arc, and compact single-outline Saturn controls",
    "Now Playing refined with a thicker translucent timeline-shaped separator, softer lower playback frost, larger Play/Pause orbit, and Previous/Next moved closer to center",
    "Refined Glass Now Playing to match the supplied reference: true circular-fan Up Next geometry and corrected compact Saturn transport proportions",
    "Glass Now Playing now separates Up Next from playback with a curved divider, stronger lower-panel frost, and a horizontally scrollable circular fan queue",
    "Fixed Glass Up Next screenshot issue: removed numbering/encoded-looking prefixes, tightened spacing, and softened the horizontal arc",
    "Refined Glass Up Next: borderless vertical song names in a smoother horizontally scrollable curved layout",
    "Refined full-app Glassmorphism with a liquid gradient background, darker readable glass cards, vertical scrollable Up Next labels, deeper seek arc, and a smoother Saturn transport outline",
    "Glassmorphism is now a full-app theme with a continuous Saturn transport border, deeper curved seek arc, and scrollable arced Up Next cards",
    "Glass playback controls now use Saturn-style orbital rings and a deeper curved song timeline",
    "Glass Now Playing refined again for full-screen artwork, lower reference layout, and truly connected liquid transport controls",
    "Refined Glass Now Playing to closely match the reference: cleaner artwork, centered metadata, compact arc timeline, organic liquid transport, and minimal controls",
    "Fixed the curved glass seek timeline gesture build issue",
    "Glass Now Playing now matches the reference with an arched seek timeline and connected liquid playback controls",
    "Glassmorphism Now Playing now includes reference-style liquid-glass Previous, Play/Pause, Next, Shuffle and Repeat controls",
    "Optional Glassmorphism Now Playing mode with a blurred album-art backdrop and frosted translucent interface",
    "Fixed exact song-variant search matching and ranking for slowed, reverb, lofi, remix, and sped-up queries",
    "Search now prioritizes the exact song title before version words like slowed, reverb, lofi, remix and sped-up",
    "Updater now opens the Android installer automatically as soon as the APK download finishes",
    "Background opacity slider restored with live 0–100% control",
    "Simplified appearance selector with Solid and Liquid modes using one variable accent color",
    "New album-art styles: Depth Float, Bass Zoom, Spatial Float, Parallax, and Vinyl",
    "Album-art style now defaults to Off",
    "Expand Up Next when scrolled now defaults to On",
    "Smart Queue automatically adds related songs when Up Next is nearly empty",
    "Gapless Playback option added, enabled by default",
    "Adjustable crossfade retained for smooth transitions between songs",
    "Swipe down on Now Playing to collapse back to the mini-player",
    "Email Activation moved into Settings below Cloudflare Usage and above About",
    "Theme accent color picker is collapsible and applies to both Solid and Liquid modes"
)

private val featureGroups = listOf(
    "Playback" to listOf(
        "Stream millions of songs via JioSaavn, plus play files already on your device",
        "Gapless playback with adjustable crossfade between tracks",
        "Adjustable playback speed",
        "Sleep timer — countdown or end-of-track",
        "Synced lyrics, shown alongside the track",
        "Audio visualizer on Now Playing",
        "Smart Queue — keeps Up Next topped up automatically",
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
        "Single variable accent color shared by Solid and Liquid appearance modes",
        "Optional full-app Glassmorphism theme with frosted translucent surfaces and reference-style playback controls",
        "Solid or Liquid appearance selection",
        "Multiple app icon presets",
        "Custom background photo, plus a gallery of favorite wallpapers",
        "Depth Float, Bass Zoom, Spatial Float, Parallax, and Vinyl album-art styles",
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
        "Offline mode banner when there's no connection",
        "Email activation access management from Settings"
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
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
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
                        text = "Version ${BuildConfig.VERSION_NAME} • Build ${BuildConfig.VERSION_CODE}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Latest updates",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 18.dp, bottom = 4.dp)
            )
            latestUpdates.forEach { update ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("•  ", color = MaterialTheme.colorScheme.primary)
                    Text(update, style = MaterialTheme.typography.bodySmall)
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
        }
    }
}
