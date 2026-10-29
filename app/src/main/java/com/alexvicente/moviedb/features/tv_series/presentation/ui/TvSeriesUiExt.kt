package com.alexvicente.moviedb.features.tv_series.presentation.ui

import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeries
import java.util.Locale

internal fun TvSeries.firstAirYear(): String? = firstAirDate?.take(4)

internal fun TvSeries.formattedRating(): String =
    String.format(Locale.getDefault(), "%.1f", voteAverage)