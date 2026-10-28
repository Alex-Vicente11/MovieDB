package com.alexvicente.moviedb.features.tv_series.data.mapper

import com.alexvicente.moviedb.features.tv_series.data.remote.dto.TvSeriesDto
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeries

object TvSeriesMapper {

    fun TvSeriesDto.toDomain(): TvSeries = TvSeries(
        id = id,
        name = name.trim(),
        overview = overview?.trim().orEmpty(),
        posterPath = posterPath?.trim(),
        backdropPath = backdropPath?.trim(),
        voteAverage = voteAverage,
        voteCount = voteCount,
        firstAirDate = firstAirDate?.trim()?.takeIf { it.isNotEmpty() },
        popularity = popularity
    )
}