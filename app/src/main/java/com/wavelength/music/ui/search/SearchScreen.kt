package com.wavelength.music.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wavelength.music.R
import com.wavelength.music.data.model.Track
import com.wavelength.music.ui.components.EmptyView
import com.wavelength.music.ui.components.ErrorView
import com.wavelength.music.ui.components.LoadingView
import com.wavelength.music.ui.components.ScreenState
import com.wavelength.music.ui.components.TrackOptionsSheet
import com.wavelength.music.ui.components.TrackRow

@Composable
fun SearchScreen(
    onTrackClick: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val history by viewModel.searchHistory.collectAsStateWithLifecycle()
    var trackForMenu by remember { mutableStateOf<Track?>(null) }

    trackForMenu?.let { track ->
        TrackOptionsSheet(track = track, onDismiss = { trackForMenu = null })
    }

    Scaffold(
        topBar = {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = { viewModel.commitSearch() })
            )
        }
    ) { padding ->
        if (query.isBlank()) {
            SearchHistoryList(
                modifier = Modifier.padding(padding),
                history = history,
                onItemClick = { viewModel.onHistoryItemClick(it) },
                onRemoveItem = viewModel::removeHistoryEntry,
                onClearAll = viewModel::clearHistory
            )
            return@Scaffold
        }

        when (val state = results) {
            is ScreenState.Loading -> LoadingView(modifier = Modifier.padding(padding))
            is ScreenState.Error -> ErrorView(
                onRetry = viewModel::retry,
                modifier = Modifier.padding(padding),
                message = state.message
            )
            is ScreenState.Empty -> EmptyView(
                modifier = Modifier.padding(padding),
                message = stringResource(R.string.search_empty_hint)
            )
            is ScreenState.Success -> {
                val tracks = state.data
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    itemsIndexed(tracks) { index, track ->
                        TrackRow(
                            track = track,
                            onClick = {
                                viewModel.playTrack(tracks, index)
                                onTrackClick()
                            },
                            onMoreClick = { trackForMenu = track }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHistoryList(
    modifier: Modifier,
    history: List<String>,
    onItemClick: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearAll: () -> Unit
) {
    if (history.isEmpty()) {
        EmptyView(modifier = modifier.fillMaxSize(), message = stringResource(R.string.no_recent_searches))
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent searches", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onClearAll) { Text("Clear all") }
            }
        }
        items(history, key = { it }) { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(entry) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.History, contentDescription = null)
                Text(entry, modifier = Modifier.weight(1f).padding(start = 16.dp))
                IconButton(onClick = { onRemoveItem(entry) }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Remove")
                }
            }
        }
    }
}
