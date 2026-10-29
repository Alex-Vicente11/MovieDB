package com.alexvicente.moviedb.features.tv_series.presentation.ui

import androidx.annotation.StringRes
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeries
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesCategory
import java.util.Locale

internal fun TvSeries.firstAirYear(): String? = firstAirDate?.take(4)

internal fun TvSeries.formattedRating(): String =
    String.format(Locale.getDefault(), "%.1f", voteAverage)

@StringRes
internal fun TvSeriesCategory.labelRes(): Int = when (this) {
    TvSeriesCategory.POPULAR -> R.string.tv_series_category_popular
    TvSeriesCategory.TOP_RATED -> R.string.tv_series_category_top_rated
}