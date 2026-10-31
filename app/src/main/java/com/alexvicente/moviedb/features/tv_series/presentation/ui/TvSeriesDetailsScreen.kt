// presentation/ui/TvSeriesDetailsScreen.kt
package com.alexvicente.moviedb.features.tv_series.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alexvicente.moviedb.features.tv_series.presentation.TvSeriesDetailsUiState
import com.alexvicente.moviedb.features.tv_series.presentation.TvSeriesDetailsViewModel

@Composable
fun TvSeriesDetailsRoute(
    modifier: Modifier = Modifier,
    viewModel: TvSeriesDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TvSeriesDetailsScreen(
        uiState = uiState,
        onRetry = viewModel::retry,
        modifier = modifier.statusBarsPadding()
    )
}

@Composable
fun TvSeriesDetailsScreen(
    uiState: TvSeriesDetailsUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is TvSeriesDetailsUiState.Loading -> FullScreenLoading(modifier)
        is TvSeriesDetailsUiState.Error -> FullScreenError(onRetry = onRetry, modifier = modifier)
        is TvSeriesDetailsUiState.Success -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = uiState.details.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}