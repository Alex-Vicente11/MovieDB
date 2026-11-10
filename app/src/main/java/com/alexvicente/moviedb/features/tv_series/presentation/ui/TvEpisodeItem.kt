package com.alexvicente.moviedb.features.tv_series.presentation.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.core.presentation.theme.MovieDbTheme
import com.alexvicente.moviedb.core.presentation.util.TmdbImage
import com.alexvicente.moviedb.features.tv_series.domain.model.TvEpisode

private const val COLLAPSED_OVERVIEW_LINES = 3

@Composable
internal fun TvEpisodeItem(
    episode: TvEpisode,
    modifier: Modifier = Modifier
) {
    // rememberSaveable: sobrevive al scroll (el item sale de la composicion) y a la rotación
    var expanded by rememberSaveable { mutableStateOf(false) }
    // solo se puede expandir si, colapsada, la sinopsis quedó recortada
    var canExpand by remember { mutableStateOf(false) }
    val toggleable = canExpand || expanded

    val episodeLabel = stringResource(R.string.tv_series_episode_short, episode.episodeNumber)
    val runtime = episode.runtimeMinutes?.let {
        stringResource(R.string.tv_series_runtime_minutes, it)
    }
    val date = remember(episode.airDate) { formatAirDate(episode.airDate) }
    val rating = if (episode.hasRating()) "★ ${episode.voteAverage.toRatingText()}" else null
    val meta = listOfNotNull(episodeLabel, runtime, date, rating).joinToString("  ·  ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = toggleable) { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StillImage(path = episode.stillPath, width = 120.dp)

        Column(
            modifier = Modifier.weight(1f).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = episode.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            if (episode.overview.isNotEmpty()) {
                Text(
                    text = episode.overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_OVERVIEW_LINES,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { result ->
                        if (!expanded) canExpand = result.hasVisualOverflow
                    }
                )
                if (toggleable) {
                    Text(
                        text = stringResource(
                            if (expanded) R.string.tv_series_episode_see_less
                            else R.string.tv_series_episode_see_more
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun StillImage(path: String?, width: Dp, modifier: Modifier = Modifier) {
    AsyncImage(
        model = TmdbImage.still(path),
        contentDescription = null, // decorativa: el título va en el texto contiguo
        contentScale = ContentScale.Crop,
        modifier = modifier
            .width(width)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
private fun TvEpisodeItemPreview() {
    MovieDbTheme {
        Column {
            TvEpisodeItem(
                TvEpisode(
                    id = 1, episodeNumber = 1, name = "Una noche de un duro día",
                    overview = "Meredith Grey llega a su primer día como residente del Seattle Grace " +
                            "y descubre que sus compañeros son tan competitivos como ella, mientras " +
                            "una paciente pone a prueba a todo el equipo durante la noche.",
                    airDate = "2005-03-27", runtimeMinutes = 44, stillPath = null,
                    voteAverage = 7.8, voteCount = 120
                )
            )
            TvEpisodeItem(
                TvEpisode(
                    id = 2, episodeNumber = 5, name = "Episodio 5", overview = "",
                    airDate = "2026-11-12", runtimeMinutes = null, stillPath = null,
                    voteAverage = 0.0, voteCount = 0
                )
            )
        }
    }
}

