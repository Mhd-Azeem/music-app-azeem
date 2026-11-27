package com.wavelength.music.ui.radio

import android.content.ActivityNotFoundException
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.TextButton
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Button
import androidx.compose.ui.text.style.TextAlign
import com.wavelength.music.data.repository.VisualThemeMode
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun ExperienceSwitcher(
    onSwitch: () -> Unit,
    radioSelected: Boolean = false,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    var expanded by remember { mutableStateOf(false) }
    val current = if (radioSelected) "AzRadioFM" else "AzMusic"
    Box {
        Row(
            modifier = Modifier.clickable(onClickLabel = "Switch between AzMusic and AzRadioFM") { expanded = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(current, color = tint, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = tint)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("AzMusic", "AzRadioFM").forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        if (option != current) onSwitch()
                    }
                )
            }
        }
    }
}

private data class Station(val id: String, val name: String, val details: String, val livePage: String, val stream: String)

private val stations = listOf(
    Station("shakthi", "Shakthi FM", "Tamil · 103.9 / 104.1 FM", "https://shakthifm.com/home.aspx", "https://mbc.thestreamtech.com:8086/stream"),
    Station("sooriyan", "Sooriyan FM", "Tamil · 103.4 / 103.6 FM", "https://www.sooriyanfm.lk/listenlive.php", "https://radio.lotustechnologieslk.net:8006/"),
    Station("vasantham", "Vasantham FM", "Tamil · 102.6 / 102.8 FM", "https://www.vasanthamfm.lk/", "https://cp12.serverse.com/proxy/vasanthamfm?mp=/stream"),
    Station("slbc-tamil", "SLBC Tamil National", "Tamil · 102.1 / 102.3 FM", "https://slbc.lk/", "https://stream-32.zeno.fm/xekhrn4zetzuv"),
    Station("hiru", "Hiru FM", "Sinhala · 96.1 / 96.3 FM", "https://www.hirufm.lk/listenlive.php", "https://radio.lotustechnologieslk.net:2020/stream/hirufmgarden/stream/1/"),
    Station("shaa", "Shaa FM", "Sinhala · 90.9 / 91.1 FM", "https://www.shaafm.lk/listenlive.php", "https://radio.lotustechnologieslk.net:2020/stream/shaafmgarden"),
    Station("neth", "Neth FM", "Sinhala · 94.8 / 95.0 FM", "https://nethfm.com/", "https://cp11.serverse.com/proxy/nethfm/stream"),
    Station("gold", "Gold FM", "English · 93.0 / 93.2 FM", "https://www.goldfm.lk/", "https://radio.lotustechnologieslk.net:2020/stream/goldfmgarden"),
    Station("sun", "Sun FM", "English · 98.7 / 98.9 FM", "https://www.sunfm.lk/", "https://radio.lotustechnologieslk.net:2020/stream/sunfmgarden")
)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
internal fun RadioScreen(
    onOpenMusic: () -> Unit,
    onStartRadio: () -> Unit,
    visualThemeMode: VisualThemeMode,
    minimal: Boolean
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("azradiofm", android.content.Context.MODE_PRIVATE) }
    var favorites by remember(prefs) { mutableStateOf(prefs.getStringSet("favorites", emptySet()).orEmpty().toSet()) }
    var recents by remember(prefs) { mutableStateOf(prefs.getString("recents", "").orEmpty().split(',').filter { it.isNotEmpty() }) }
    var query by rememberSaveable { mutableStateOf("") }
    var expandedPlayer by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    BackHandler(enabled = expandedPlayer) { expandedPlayer = false }
    val search = query.trim()
    val matching = remember(search) {
        stations.filter { station ->
            station.name.contains(search, ignoreCase = true) || station.details.contains(search, ignoreCase = true)
        }
    }

    val onFavorite: (Station) -> Unit = { station ->
        favorites = if (station.id in favorites) favorites - station.id else favorites + station.id
        prefs.edit().putStringSet("favorites", favorites).apply()
    }
    var current by remember { mutableStateOf<Station?>(null) }
    var status by remember { mutableStateOf("Stopped") }
    var stopAvailable by remember { mutableStateOf(false) }
    val player = remember(context) {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            setAudioAttributes(AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(), true)
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                stopAvailable = player.playWhenReady && player.playerError == null &&
                    player.playbackState != Player.STATE_IDLE && player.playbackState != Player.STATE_ENDED
                status = when {
                    player.playerError != null -> "Stream unavailable"
                    player.isPlaying -> "LIVE"
                    stopAvailable -> "Connecting…"
                    else -> "Stopped"
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    val startStation: (Station) -> Unit = { station ->
        onStartRadio()
        current = station
        status = "Connecting…"
        stopAvailable = true
        player.setMediaItem(MediaItem.fromUri(station.stream))
        player.prepare()
        player.play()
        recents = (listOf(station.id) + recents.filter { it != station.id }).take(10)
        prefs.edit().putString("recents", recents.joinToString(",")).apply()
    }
    val onListen: (Station) -> Unit = { station ->
        if (current != station || !stopAvailable) startStation(station)
    }
    val togglePlayback: (Station) -> Unit = { station ->
        if (stopAvailable) {
            player.pause()
            player.stop()
        } else {
            startStation(station)
        }
    }
    val openOfficial: (Station) -> Unit = { station ->
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(station.livePage)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Install a browser to open the live player.", Toast.LENGTH_SHORT).show()
        }
    }

    val selected = current
    if (expandedPlayer && selected != null) {
        RadioNowPlaying(
            station = selected,
            status = status,
            stopAvailable = stopAvailable,
            saved = selected.id in favorites,
            onCollapse = { expandedPlayer = false },
            onTogglePlayback = { togglePlayback(selected) },
            onFavorite = { onFavorite(selected) },
            onOpenOfficial = { openOfficial(selected) }
        )
        return
    }

    Scaffold(topBar = {
        if (minimal) {
            TopAppBar(title = { ExperienceSwitcher(onSwitch = onOpenMusic, radioSelected = true) })
        } else {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                StyledExperienceSwitcher(visualThemeMode, onSwitch = onOpenMusic, radioSelected = true)
            }
        }
    }, bottomBar = {
        current?.let { station ->
            ListItem(
                modifier = Modifier.clickable(onClickLabel = "Open radio Now Playing") {
                    expandedPlayer = true
                },
                headlineContent = { Text(station.name) },
                supportingContent = { Text(status) },
                trailingContent = {
                    TextButton(onClick = { togglePlayback(station) }) {
                        Text(if (stopAvailable) "Stop" else "Play")
                    }
                }
            )
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                label = { Text("Search stations") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
            )
            LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 16.dp)) {
                if (search.isEmpty()) {
                    stationSection("Favorites", stations.filter { it.id in favorites }, favorites, onFavorite, onListen,
                        emptyMessage = "Tap the heart to save a station.")
                    stationSection("Recents", recents.mapNotNull { id -> stations.firstOrNull { it.id == id } },
                        favorites, onFavorite, onListen, emptyMessage = "Stations you open will appear here.")
                }
                stationSection(if (search.isEmpty()) "All stations" else "Search results",
                    matching, favorites, onFavorite, onListen, emptyMessage = "No matching stations.")
            }
        }
    }
}

@Composable
private fun RadioNowPlaying(
    station: Station,
    status: String,
    stopAvailable: Boolean,
    saved: Boolean,
    onCollapse: () -> Unit,
    onTogglePlayback: () -> Unit,
    onFavorite: () -> Unit,
    onOpenOfficial: () -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Now Playing") },
            navigationIcon = {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to stations")
                }
            },
            actions = {
                IconButton(onClick = onFavorite) {
                    Icon(
                        if (saved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (saved) "Remove from Favorites" else "Add to Favorites"
                    )
                }
            }
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(152.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Radio, contentDescription = null, modifier = Modifier.size(70.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(station.name, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text(station.details, textAlign = TextAlign.Center)
            Text(status, color = if (status == "LIVE") MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onTogglePlayback) { Text(if (stopAvailable) "Stop" else "Play") }
            if (status == "Stream unavailable") {
                TextButton(onClick = onOpenOfficial) { Text("Open official player") }
            }
        }
    }
}

private fun LazyListScope.stationSection(
    title: String,
    stations: List<Station>,
    favorites: Set<String>,
    onFavorite: (Station) -> Unit,
    onListen: (Station) -> Unit,
    emptyMessage: String
) {
    item(key = title) {
        Text(title, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp),
            style = MaterialTheme.typography.titleMedium)
    }
    if (stations.isEmpty()) item(key = "$title-empty") {
        Text(emptyMessage, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    items(stations, key = { "$title-${it.id}" }) { station ->
        val saved = station.id in favorites
        ListItem(
            modifier = Modifier.clickable(onClickLabel = "Play live station") { onListen(station) },
            headlineContent = { Text(station.name) },
            supportingContent = { Text(station.details) },
            leadingContent = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
            trailingContent = {
                IconButton(onClick = { onFavorite(station) }) {
                    Icon(
                        if (saved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (saved) "Remove ${station.name} from Favorites" else "Add ${station.name} to Favorites",
                        tint = if (saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}
