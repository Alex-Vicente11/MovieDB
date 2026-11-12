package com.alexvicente.moviedb.features.tv_series.domain.model

data class TvSeasonDetails(
    val id: Int,
    val seasonNumber:Int,
    val name: String,
    val overview: String,
    val posterPath: String?,
    val airDate: String?,
    val voteAverage: Double,
    val episodes: List<TvEpisode>
)

data class TvEpisode(
    val id: Int,
    val episodeNumber: Int,
    val name: String,
    val overview: String,
    val airDate: String?,
    val runtimeMinutes: Int?,
    val stillPath: String?,
    val voteAverage: Double,
    val voteCount: Int
)
