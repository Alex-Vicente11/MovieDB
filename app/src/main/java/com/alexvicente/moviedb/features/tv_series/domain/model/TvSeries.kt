package com.alexvicente.moviedb.features.tv_series.domain.model

data class TvSeries(
    val id: Int,
    val name: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double,
    val voteCount: Int,
    val firstAirDate: String?,
    val popularity: Double
)
