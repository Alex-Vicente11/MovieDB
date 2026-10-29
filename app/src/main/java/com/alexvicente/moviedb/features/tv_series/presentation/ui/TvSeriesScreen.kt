package com.alexvicente.moviedb.features.tv_series.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.core.presentation.theme.MovieDbTheme
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeries
import com.alexvicente.moviedb.features.tv_series.presentation.TvSeriesViewModel
import kotlinx.coroutines.flow.flowOf

// iguala al espacio que reservan los XML bajo el BottomNavigationView
private val BottomNavigationSpace = 80.dp

@Composable
fun TvSeriesRoute(
    modifier: Modifier = Modifier,
    viewModel: TvSeriesViewModel = hiltViewModel()
) {
    val tvSeries = viewModel.tvSeries.collectAsLazyPagingItems()

    TvSeriesScreen(
        tvSeries = tvSeries,
        contentPadding = PaddingValues(bottom = BottomNavigationSpace),
        modifier = modifier.statusBarsPadding()
    )
}
@Composable
fun TvSeriesScreen(
    tvSeries: LazyPagingItems<TvSeries>,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val refresh = tvSeries.loadState.refresh

    when {
        tvSeries.itemCount == 0 && refresh is LoadState.Loading ->
            FullScreenLoading(modifier)

        tvSeries.itemCount == 0 && refresh is LoadState.Error ->
            FullScreenError(onRetry = { tvSeries.retry() }, modifier = modifier)

        else -> TvSeriesList(tvSeries, contentPadding, modifier)
    }
}

@Composable
private fun TvSeriesList(
    tvSeries: LazyPagingItems<TvSeries>,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        items(
            count = tvSeries.itemCount,
            key = tvSeries.itemKey { it.id }
        ) { index ->
            tvSeries[index]?.let { series ->
                Text(
                    text = series.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }

        when (tvSeries.loadState.append) {
            is LoadState.Loading -> item { AppendLoading() }
            is LoadState.Error -> item { AppendError(onRetry = { tvSeries.retry() }) }
            else -> Unit
        }
    }
}

@Composable
private fun FullScreenLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun FullScreenError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.tv_series_retry)
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.tv_series_retry))
        }
    }
}

@Composable
private fun AppendLoading() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AppendError(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = onRetry) { Text(stringResource(R.string.tv_series_retry))}
    }
}

private val previewSeries = List(5) { i ->
    TvSeries(
        id = i,
        name = "Series $i",
        overview = "",
        posterPath = null,
        backdropPath = null,
        voteAverage = 8.0,
        voteCount = 10,
        firstAirDate = null,
        popularity = 1.0

    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
private fun TvSeriesScreenPreview() {
    val items = flowOf(PagingData.from(previewSeries)).collectAsLazyPagingItems()
    MovieDbTheme { TvSeriesScreen(tvSeries = items, contentPadding = PaddingValues()) }
}