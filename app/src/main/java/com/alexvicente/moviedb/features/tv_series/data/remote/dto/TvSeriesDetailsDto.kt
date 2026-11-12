package com.alexvicente.moviedb.features.tv_series.data.remote.dto

import com.alexvicente.moviedb.core.data.remote.dto.GenreDto
import com.google.gson.annotations.SerializedName

data class TvSeriesDetailsDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("tagline") val tagline: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("vote_count") val voteCount: Int,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int,
    @SerializedName("genres") val genres: List<GenreDto>?,
    @SerializedName("seasons") val seasons: List<TvSeasonSummaryDto>?
)

data class TvSeasonSummaryDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("air_date") val airDate: String?,
    @SerializedName("episode_count") val episodeCount: Int,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("vote_average") val voteAverage: Double
)
