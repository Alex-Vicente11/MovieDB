package com.alexvicente.moviedb.features.tv_series.domain.model

import com.alexvicente.moviedb.core.domain.model.Genre

data class TvSeriesDetails(
    val id: Int,
    val name: String,
    val tagline: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double,
    val voteCount: Int,
    val firstAirDate: String?,
    val status: TvSeriesStatus,
    val numberOfSeasons: Int,
    val numberOfEpisodes: Int,
    val genres: List<Genre>,
    val seasons: List<TvSeasonSummary>
)

data class TvSeasonSummary(
    val id: Int,
    val name: String,
    val overview: String,
    val posterPath: String?,
    val airDate: String?,
    val episodeCount: Int,
    val seasonNumber: Int,
    val voteAverage: Double
)

enum class TvSeriesStatus {
    RETURNING_SERIES, IN_PRODUCTION, PLANNED, PILOT, ENDED, CANCELED, UNKNOWN
}