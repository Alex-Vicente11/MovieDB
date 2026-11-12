package com.alexvicente.moviedb.features.tv_series.presentation.ui

import androidx.annotation.StringRes
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.features.tv_series.domain.model.TvEpisode
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeasonDetails
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeasonSummary
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeries
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesCategory
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesDetails
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesStatus
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

private const val SPECIALS_SEASON_NUMBER = 0
private val ContentLocale: Locale = Locale.forLanguageTag("es-MX")

internal fun Double.toRatingText(): String = String.format(Locale.getDefault(), "%.1f", this)
internal fun TvSeries.firstAirYear(): String? = firstAirDate?.take(4)
internal fun TvSeries.formattedRating(): String = voteAverage.toRatingText()
internal fun TvSeriesDetails.firstAirYear(): String? = firstAirDate?.take(4)
internal fun TvSeasonSummary.airYear(): String? = airDate?.take(4)
internal fun TvSeasonDetails.airYear(): String? = airDate?.take(4)

// vote_average llega como 0.0 cuando no hay votos: la existencia de calificación la dice vote_count
internal fun TvEpisode.hasRating(): Boolean = voteCount > 0

@StringRes
internal fun TvSeriesCategory.labelRes(): Int = when (this) {
    TvSeriesCategory.POPULAR -> R.string.tv_series_category_popular
    TvSeriesCategory.TOP_RATED -> R.string.tv_series_category_top_rated
}

// null = sin etiqueta: UNKNOWN no se muestra en pantalla
@StringRes
internal fun TvSeriesStatus.labelRes(): Int? = when (this) {
    TvSeriesStatus.RETURNING_SERIES -> R.string.tv_series_status_returning
    TvSeriesStatus.IN_PRODUCTION -> R.string.tv_series_status_in_production
    TvSeriesStatus.PLANNED -> R.string.tv_series_status_planned
    TvSeriesStatus.PILOT -> R.string.tv_series_status_pilot
    TvSeriesStatus.ENDED -> R.string.tv_series_status_ended
    TvSeriesStatus.CANCELED -> R.string.tv_series_status_canceled
    TvSeriesStatus.UNKNOWN -> null
}

// TMDB manda «Especiales» (temporada 0) primero; se muestra al final. sortedBy es estable.
internal fun List<TvSeasonSummary>.specialsLast(): List<TvSeasonSummary> =
    sortedBy { if (it.seasonNumber == SPECIALS_SEASON_NUMBER) Int.MAX_VALUE else it.seasonNumber }

internal fun formatAirDate(isoDate: String?, locale: Locale = ContentLocale): String? {
    if (isoDate.isNullOrBlank()) return null
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val date = parser.parse(isoDate) ?: return null
        DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(date)
    } catch (e: ParseException) {
        null
    }
}