package com.alexvicente.moviedb.testutil.factories

import com.alexvicente.moviedb.features.tv_series.data.remote.dto.TvSeriesDto
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeries

object TvSeriesFactory {

    fun createTvSeriesDto(
        id: Int = 1,
        name: String = "Series 1",
        overview: String? = "Overview",
        posterPath: String? = "/poster.jpg",
        backdropPath: String? = "/backdrop.jpg",
        voteAverage: Double = 8.5,
        voteCount: Int = 100,
        firstAirDate: String? = "2020-01-15",
        popularity: Double = 50.0
    ) = TvSeriesDto(
        id = id, name = name, overview = overview, posterPath = posterPath,
        backdropPath = backdropPath, voteAverage = voteAverage, voteCount = voteCount,
        firstAirDate = firstAirDate, popularity = popularity
    )

    fun createTvSeriesDtoList(size: Int): List<TvSeriesDto> =
        (1..size).map { createTvSeriesDto(id = it, name = "Series $it") }

    fun createTvSeries(id: Int = 1, name: String = "Series 1") = TvSeries(
        id = id, name = name, overview = "Overview", posterPath = "/poster.jpg",
        backdropPath = "/backdrop.jpg", voteAverage = 8.5, voteCount = 100,
        firstAirDate = "2020-01-15", popularity = 50.0
    )

    fun createTvSeriesList(size: Int): List<TvSeries> =
        (1..size).map { createTvSeries(id = it, name = "Series $it") }
}