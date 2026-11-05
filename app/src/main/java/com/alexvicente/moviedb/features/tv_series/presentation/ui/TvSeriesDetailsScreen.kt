// presentation/ui/TvSeriesDetailsScreen.kt
package com.alexvicente.moviedb.features.tv_series.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.core.domain.model.Genre
import com.alexvicente.moviedb.core.presentation.theme.MovieDbTheme
import com.alexvicente.moviedb.core.presentation.util.TmdbImage
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeasonSummary
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesDetails
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesStatus
import com.alexvicente.moviedb.features.tv_series.presentation.CastUiState
import com.alexvicente.moviedb.features.tv_series.presentation.TvSeriesDetailsUiState
import com.alexvicente.moviedb.features.tv_series.presentation.TvSeriesDetailsViewModel

@Composable
fun TvSeriesDetailsRoute(
    onBackClick: () -> Unit,
    onSeasonClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvSeriesDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val castState by viewModel.castState.collectAsStateWithLifecycle()

    TvSeriesDetailsScreen(
        uiState = uiState,
        castState = castState,
        onRetry = viewModel::retry,
        onCastRetry = viewModel::retryCast,
        onBackClick = onBackClick,
        onSeasonClick = onSeasonClick,
        modifier = modifier.statusBarsPadding()
    )
}

@Composable
fun TvSeriesDetailsScreen(
    uiState: TvSeriesDetailsUiState,
    castState: CastUiState,
    onRetry: () -> Unit,
    onCastRetry: () -> Unit,
    onBackClick: () -> Unit,
    onSeasonClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is TvSeriesDetailsUiState.Loading -> FullScreenLoading()
            is TvSeriesDetailsUiState.Error -> FullScreenError(onRetry = onRetry)
            is TvSeriesDetailsUiState.Success -> TvSeriesDetailsContent(uiState.details, castState, onCastRetry, onSeasonClick)
        }

        BackButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
        )
    }
}

@Composable
private fun TvSeriesDetailsContent(
    details: TvSeriesDetails,
    castState: CastUiState,
    onCastRetry: () -> Unit,
    onSeasonClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val seasons = remember(details.seasons) { details.seasons.specialsLast() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = WindowInsets.navigationBars.asPaddingValues()
    ) {
        item(key = "header") { DetailsHeader(details) }
        item(key = "overview") { OverviewSection(details.overview) }
        item(key = "cast") { CastSection(castState = castState, onRetry = onCastRetry)}
        item(key = "seasons-title") {
            SectionTitle(stringResource(R.string.tv_series_details_seasons))
        }
        items(seasons, key = { it.id }) { season ->
            TvSeasonItem(season = season, onClick = { onSeasonClick(season.seasonNumber) })
        }
    }
}

@Composable
private fun DetailsHeader(details: TvSeriesDetails) {
    val statusLabel = details.status.labelRes()?.let { stringResource(it) }
    val meta = listOfNotNull(
        details.firstAirYear(),
        statusLabel,
        "★ ${details.voteAverage.toRatingText()}"
    ).joinToString("  ·  ")
    val genres = details.genres.joinToString(" · ") { it.name }

    val background = MaterialTheme.colorScheme.background
    val scrim = Brush.verticalGradient(
        colorStops = arrayOf(
            0f to Color.Black.copy(alpha = 0.6f),  // legibilidad de los íconos de la barra
            0.4f to background.copy(alpha = 0f),
            1f to background                       // el backdrop se funde con el fondo
        )
    )

    Column {
        Box {
            AsyncImage(
                model = TmdbImage.backdrop(details.backdropPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surface)
            )
            Box(modifier = Modifier.matchParentSize().background(scrim))
        }

        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PosterImage(path = details.posterPath, width = 110.dp)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = details.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (details.tagline.isNotEmpty()) {
                    Text(
                        text = details.tagline,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                if (genres.isNotEmpty()) {
                    Text(
                        text = genres,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewSection(overview: String) {
    Text(
        text = overview.ifEmpty { stringResource(R.string.tv_series_details_overview_empty) },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun TvSeasonItem(
    season: TvSeasonSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val episodes = pluralStringResource(
        R.plurals.tv_series_episode_count, season.episodeCount, season.episodeCount
    )
    val meta = listOfNotNull(episodes, season.airYear()).joinToString("  .  ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PosterImage(path = season.posterPath, width = 64.dp, cornerRadius = 6.dp)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = season.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            if (season.overview.isNotEmpty()) {
                Text(
                    text = season.overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .padding(8.dp)
            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.tv_series_details_back),
            tint = Color.White
        )
    }
}

private val previewDetails = TvSeriesDetails(
    id = 1,
    name = "Anatomía según Grey",
    tagline = "La vida que salves puede ser la tuya.",
    overview = "Un grupo de médicos residentes del Seattle Grace Hospital.",
    posterPath = null,
    backdropPath = null,
    voteAverage = 8.2,
    voteCount = 1000,
    firstAirDate = "2005-03-27",
    status = TvSeriesStatus.RETURNING_SERIES,
    numberOfSeasons = 2,
    numberOfEpisodes = 20,
    genres = listOf(Genre(id = 18, name = "Drama")),
    seasons = listOf(
        TvSeasonSummary(1, "Especiales", "", null, "2005-03-27", 68, 0, 0.0),
        TvSeasonSummary(2, "Temporada 1", "Los residentes comienzan.", null, "2005-03-27", 9, 1, 7.9),
        TvSeasonSummary(3, "Temporada 2", "", null, "2005-09-25", 1, 2, 8.1)
    )
)


@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
private fun TvSeriesDetailsScreenPreview() {
    MovieDbTheme {
        TvSeriesDetailsScreen(
            uiState = TvSeriesDetailsUiState.Success(previewDetails),
            castState = CastUiState.Loading,
            onRetry =  {},
            onCastRetry = {},
            onBackClick = {},
            onSeasonClick = {}
        )
    }
}