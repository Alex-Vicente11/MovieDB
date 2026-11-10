package com.alexvicente.moviedb.features.tv_series.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.core.presentation.theme.MovieDbTheme
import com.alexvicente.moviedb.features.tv_series.domain.model.TvEpisode
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeasonDetails
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
    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        // La barra no scrollea: el contenido nunca pasa bajo la barra de estado ni el botón
        SeasonTopBar(
            title = (uiState as? TvSeasonUiState.Success)?.season?.name.orEmpty(),
            onBackClick = onBackClick
        )
        Box(modifier = Modifier.weight(1f)) {
            when (uiState) {
                is TvSeasonUiState.Loading -> FullScreenLoading()
                is TvSeasonUiState.Error -> FullScreenError(onRetry = onRetry)
                is TvSeasonUiState.Success -> SeasonContent(uiState.season)
            }
        }
    }
}

@Composable
private fun SeasonTopBar(title: String, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BackButton(onClick = onBackClick)
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 16.dp)
        )
    }
}

@Composable
private fun SeasonContent(season: TvSeasonDetails) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = WindowInsets.navigationBars.asPaddingValues()
    ) {
        item(key = "header") { SeasonHeader(season) }

        if (season.episodes.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.tv_series_season_no_episodes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // id y no episodeNumber: es único en los cuatro fixtures reales
        items(season.episodes, key = { it.id }) { episode -> TvEpisodeItem(episode) }
    }
}

@Composable
private fun SeasonHeader(season: TvSeasonDetails) {
    val episodeCount = season.episodes.size
    val meta = listOfNotNull(
        season.airYear(),
        pluralStringResource(R.plurals.tv_series_episode_count, episodeCount, episodeCount)
    ).joinToString("  ·  ")

    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PosterImage(path = season.posterPath, width = 90.dp)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = meta,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            if (season.overview.isNotEmpty()) {
                Text(
                    text = season.overview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

private val previewSeason = TvSeasonDetails(
    id = 1, seasonNumber = 1, name = "Temporada 1",
    overview = "Los residentes de primer año comienzan su carrera en Seattle.",
    posterPath = null, airDate = "2005-03-27", voteAverage = 7.8,
    episodes = listOf(
        TvEpisode(
            id = 1, episodeNumber = 1, name = "Una noche de un duro día",
            overview = "Meredith Grey llega a su primer día como residente y descubre que " +
                    "sus compañeros son tan competitivos como ella, mientras una paciente " +
                    "pone a prueba a todo el equipo durante la noche.",
            airDate = "2005-03-27", runtimeMinutes = 44, stillPath = null,
            voteAverage = 7.8, voteCount = 120
        ),
        TvEpisode(
            id = 2, episodeNumber = 2, name = "Episodio 2", overview = "",
            airDate = "2026-11-12", runtimeMinutes = null, stillPath = null,
            voteAverage = 0.0, voteCount = 0
        )
    )
)

@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
private fun TvSeasonScreenPreview() {
    MovieDbTheme {
        TvSeasonScreen(
            uiState = TvSeasonUiState.Success(previewSeason),
            onRetry = {},
            onBackClick = {}
        )
    }
}