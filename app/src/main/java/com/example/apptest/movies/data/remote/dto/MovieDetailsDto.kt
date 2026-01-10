package com.example.apptest.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para detalles completos de una película
 * Incluye campos adicionales que no vienen en búsquedas
 */
data class MovieDetailsDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("overview")
    val overview: String?,

    @SerializedName("poster_path")
    val posterPath: String?,

    @SerializedName("backdrop_path")
    val backdropPath: String?,

    @SerializedName("vote_average")
    val voteAverage: Double,

    @SerializedName("vote_count")
    val voteCount: Int,

    @SerializedName("release_date")
    val releaseDate: String?,

    @SerializedName("popularity")
    val popularity: Double,

    // Campos adicionales solo en detalles
    @SerializedName("runtime")
    val runtime: Int?,

    @SerializedName("budget")
    val budget: Long?,

    @SerializedName("revenue")
    val revenue: Long?,

    @SerializedName("genres")
    val genres: List<GenreDto>?,

    @SerializedName("tagline")
    val tagline: String?,

    @SerializedName("status")
    val status: String?,

    @SerializedName("homepage")
    val homepage: String?,

    @SerializedName("imdb_id")
    val imdbId: String?,

    @SerializedName("original_language")
    val originalLanguage: String,

    @SerializedName("original_title")
    val originalTitle: String,

    @SerializedName("adult")
    val adult: Boolean
)

/**
 * DTO para género
 */
data class GenreDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
)