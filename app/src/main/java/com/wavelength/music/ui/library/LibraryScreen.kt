package com.wavelength.music.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wavelength.music.R
import com.wavelength.music.ui.components.EmptyView
import com.wavelength.music.ui.components.TrackRow

@Composable
fun LibraryScreen(
    onTrackClick: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.favorites), stringResource(R.string.recently_played))

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_library)) }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            val list = if (selectedTab == 0) favorites else recentlyPlayed
            if (list.isEmpty()) {
                EmptyView(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(list) { index, track ->
                        TrackRow(
                            track = track,
                            onClick = {
                                viewModel.playFrom(list, index)
                                onTrackClick()
                            },
                            isFavorite = selectedTab == 0,
                            onFavoriteClick = if (selectedTab == 0) {
                                { viewModel.removeFavorite(track) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}
