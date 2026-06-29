package com.alexvicente.moviedb.core.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para la respuesta de búsqueda y películas populares
 * Representa la estructura exacta del JSON de la API
 *
 * Es DTO es usado por múltiples features
 */
data class MovieResponseDto(
    @SerializedName("page")
    val page: Int,

    @SerializedName("results")
    val results: List<MovieDto>,

    @SerializedName("total_pages")
    val totalPages: Int,

    @SerializedName("total_results")
    val totalResults: Int
)

/**
 * DTO para película individual en búsquedas y populares
 * Solo contiene anotaciones de Gson
 */
data class MovieDto(
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

    @SerializedName("adult")
    val adult: Boolean,

    @SerializedName("original_language")
    val originalLanguage: String,

    @SerializedName("original_title")
    val originalTitle: String,

    @SerializedName("video")
    val video: Boolean,

    @SerializedName("genre_ids")
    val genreIds: List<Int>?
)