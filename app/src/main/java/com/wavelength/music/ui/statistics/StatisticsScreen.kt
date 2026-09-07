package com.wavelength.music.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wavelength.music.ui.components.EmptyView
import com.wavelength.music.ui.components.SectionHeader
import com.wavelength.music.ui.components.TrackRow
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wavelength.music.activation.AdminActivationViewModel

@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    onOpenAdmin: () -> Unit = {},
    viewModel: StatisticsViewModel = hiltViewModel(),
    adminViewModel: AdminActivationViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val listeningTime by viewModel.listeningTime.collectAsStateWithLifecycle()
    val topArtists by viewModel.topArtists.collectAsStateWithLifecycle()
    val playsLast7Days by viewModel.playsLast7Days.collectAsStateWithLifecycle()
    val playsLast30Days by viewModel.playsLast30Days.collectAsStateWithLifecycle()
    val topTracks by viewModel.topTracks.collectAsStateWithLifecycle()
    val adminAuthenticated by adminViewModel.isAuthenticated.collectAsStateWithLifecycle()
    val userStats by adminViewModel.userStats.collectAsStateWithLifecycle()
    val adminLoading by adminViewModel.isLoading.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedTab, adminAuthenticated) {
        if (selectedTab == 1 && adminAuthenticated) adminViewModel.loadUserStats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listening Statistics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = { selectedTab = 0 }, modifier = Modifier.weight(1f)) {
                    Text(if (selectedTab == 0) "✓ My Stats" else "My Stats")
                }
                TextButton(onClick = { selectedTab = 1 }, modifier = Modifier.weight(1f)) {
                    Text(if (selectedTab == 1) "✓ User Stats" else "User Stats")
                }
            }

            if (selectedTab == 1) {
                if (!adminAuthenticated) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("User Stats requires the Activation Admin session.")
                        Button(onClick = onOpenAdmin) { Text("Open Activation Admin") }
                    }
                } else if (adminLoading && userStats.isEmpty()) {
                    EmptyView(modifier = Modifier.fillMaxSize(), message = "Loading user statistics…")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item { SectionHeader("Per-user listening") }
                        if (userStats.isEmpty()) {
                            item { Text("No user listening data yet.", modifier = Modifier.padding(16.dp)) }
                        }
                        items(userStats, key = { it.email }) { user ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(user.email, style = MaterialTheme.typography.titleMedium)
                                    Text("${user.totalPlays} songs played")
                                    Text("Listening time: ${formatDuration(user.totalListenedMs)}")
                                }
                            }
                        }
                    }
                }
                return@Column
            }

            if (stats.totalPlays == 0 && listeningTime.allTimeMs == 0L) {
                EmptyView(
                    modifier = Modifier.fillMaxSize(),
                    message = "Play some songs and your stats will show up here"
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(label = "Plays", value = stats.totalPlays.toString(), modifier = Modifier.weight(1f))
                            StatCard(label = "Tracks", value = stats.uniqueTracks.toString(), modifier = Modifier.weight(1f))
                            StatCard(label = "Artists", value = stats.uniqueArtists.toString(), modifier = Modifier.weight(1f))
                        }
                    }

                    item {
                        val average = if (stats.uniqueTracks > 0) stats.totalPlays.toDouble() / stats.uniqueTracks else 0.0
                        val topArtist = topArtists.firstOrNull()
                        val topArtistShare = if (topArtist != null && stats.totalPlays > 0) {
                            (topArtist.playCount * 100 / stats.totalPlays)
                        } else 0
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard("Avg plays / track", String.format("%.1f", average), Modifier.weight(1f))
                            StatCard("Top artist share", "$topArtistShare%", Modifier.weight(1f))
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard("Last 7 days", playsLast7Days.toString(), Modifier.weight(1f))
                            StatCard("Last 30 days", playsLast30Days.toString(), Modifier.weight(1f))
                        }
                    }

                    item { SectionHeader("Listening time") }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard("This week", formatDuration(listeningTime.last7DaysMs), Modifier.weight(1f))
                            StatCard("Last 30 days", formatDuration(listeningTime.last30DaysMs), Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard("Today", formatDuration(listeningTime.todayMs), Modifier.weight(1f))
                            StatCard("All time", formatDuration(listeningTime.allTimeMs), Modifier.weight(1f))
                        }
                    }

                    if (topArtists.isNotEmpty()) {
                        item { SectionHeader("Top artists") }
                        items(topArtists, key = { it.artistName }) { artist ->
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(artist.artistName, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${artist.playCount} plays", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    if (topTracks.isNotEmpty()) {
                        item { SectionHeader("Most played") }
                        itemsIndexed(topTracks, key = { _, track -> track.id }) { index, track ->
                            TrackRow(track = track, onClick = { viewModel.playTrack(topTracks, index) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


private fun formatDuration(ms: Long): String {
    val totalMinutes = (ms / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0L) "${hours}h ${minutes}m" else "${minutes}m"
}
