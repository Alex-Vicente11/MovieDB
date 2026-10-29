package com.alexvicente.moviedb.features.tv_series.presentation.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.alexvicente.moviedb.core.presentation.theme.MovieDbTheme
import com.alexvicente.moviedb.core.presentation.util.TmdbImage
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeries

@Composable
fun TvSeriesItem(
    series: TvSeries,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = TmdbImage.poster(series.posterPath),
            contentDescription = null, // decorativa: el nombre va en el texto contiguo
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(90.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = series.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOfNotNull(series.firstAirYear(), "★ ${series.formattedRating()}")
                    .joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            if (series.overview.isNotEmpty()) {
                Text(
                    text = series.overview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
private fun TvSeriesItemPreview() {
    MovieDbTheme {
        TvSeriesItem(
            series = TvSeries(
                id = 1,
                name = "Una serie con un título bastante largo para probar el recorte",
                overview = "Una sinopsis larga que debería recortarse a tres líneas para que la tarjeta no crezca sin control en la lista.",
                posterPath = null, backdropPath = null, voteAverage = 8.4, voteCount = 100,
                firstAirDate = "2021-03-14",
                popularity = 10.0
            )
        )
    }
}