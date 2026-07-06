package com.wavelength.music.ui.search

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wavelength.music.R
import com.wavelength.music.ui.components.EmptyView
import com.wavelength.music.ui.components.ErrorView
import com.wavelength.music.ui.components.LoadingView
import com.wavelength.music.ui.components.ScreenState
import com.wavelength.music.ui.components.TrackRow

@Composable
fun SearchScreen(
    onTrackClick: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()

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
                }
            )
        }
    ) { padding ->
        when (val state = results) {
            is ScreenState.Loading -> LoadingView(modifier = Modifier.padding(padding))
            is ScreenState.Error -> ErrorView(
                onRetry = viewModel::retry,
                modifier = Modifier.padding(padding),
                message = state.message
            )
            is ScreenState.Empty -> EmptyView(
                modifier = Modifier.padding(padding),
                message = if (query.isBlank()) {
                    stringResource(R.string.empty_results)
                } else {
                    stringResource(R.string.search_empty_hint)
                }
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
                            }
                        )
                    }
                }
            }
        }
    }
}
