package com.alexvicente.moviedb.features.tv_series.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TvSeasonDetailsDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("air_date") val airDate: String?,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("episodes") val episodes: List<TvEpisodeDto>?
)

data class TvEpisodeDto(
    @SerializedName("id") val id: Int,
    @SerializedName("episode_number") val episodeNumber: Int,
    @SerializedName("name") val name: String,
    @SerializedName("overview") val overview: String?,
    @SerializedName("air_date") val airDate: String?,
    @SerializedName("runtime") val runtime: Int?,
    @SerializedName("still_path") val stillPath: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("vote_count") val voteCount: Int
    // crew y guest_stars no se declaran: son casi tod0 el peso y la pantalla no los usa
)
