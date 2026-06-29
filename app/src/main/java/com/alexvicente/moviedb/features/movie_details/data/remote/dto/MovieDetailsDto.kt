package com.alexvicente.moviedb.features.movie_details.data.remote.dto

import com.alexvicente.moviedb.core.data.remote.dto.GenreDto
import com.google.gson.annotations.SerializedName

/**
 * DTO para detalles completos de una película
 *
 * UBICACIÓN: features/movie_details/data/remote/dto/
 *
 * Responsabilidad:
 * - Representar la estructura JSON de la API para el endpoint /movie/{id}
 * - Incluye campos adicionales que NO vienen en búsquedas
 *
 * Decisión de diseño:
 * ¿Por qué este DTO va en movie_details/ y NO en core/?
 *
 * - MovieDto (core) se usa en múltiples endpoints:
 *   * /search/movie
 *   * /movie/popular
 *   * /movie/trending
 *   * etc.
 *
 * - MovieDetailsDto solo se usa en:
 *   * /movie/{id} (endpoint de detalles)
 *
 * Por lo tanto, va en el feature específico
 *
 * Cambios vs versión legacy:
 *  Package actualizado: movies.data.remote.dto → movie_details.data.remote.dto
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
