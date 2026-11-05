package com.alexvicente.moviedb.features.tv_series.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.features.tv_series.presentation.TvSeasonUiState
import com.alexvicente.moviedb.features.tv_series.presentation.TvSeasonViewModel

@Composable
fun TvSeasonRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvSeasonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TvSeasonScreen(
        uiState = uiState,
        onRetry = viewModel::retry,
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@Composable
fun TvSeasonScreen(
    uiState: TvSeasonUiState,
    onRetry: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is TvSeasonUiState.Loading -> FullScreenLoading()
            is TvSeasonUiState.Error -> FullScreenError(onRetry = onRetry)
            is TvSeasonUiState.Success -> {
                val episodeCount = uiState.season.episodes.size
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    Text(
                        text = uiState.season.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.tv_series_episode_count, episodeCount, episodeCount
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }

        BackButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
        )
    }
}