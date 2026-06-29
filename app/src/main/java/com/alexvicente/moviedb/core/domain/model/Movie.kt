package com.alexvicente.moviedb.core.domain.model

/**
 * MODELO DE DOMINIO - Movie
 *
 * ✅ SIN anotaciones de Gson
 * ✅ Solo lógica de negocio
 * ✅ Inmutable (val)
 * ✅ Datos esenciales para la UI
 */
data class Movie(
    val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double,
    val voteCount: Int,
    val releaseDate: String,
    val popularity: Double
) {
    companion object {
        private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"
        private const val POSTER_SIZE = "w500"
        private const val BACKDROP_SIZE = "w780"
    }

    /**
     * URL completa del póster
     */
    fun getPosterUrl(): String {
        return if (posterPath != null) {
            "$IMAGE_BASE_URL$POSTER_SIZE$posterPath"
        } else {
            ""
        }
    }

    /**
     * URL completa del backdrop
     */
    fun getBackdropUrl(): String {
        return if (backdropPath != null) {
            "$IMAGE_BASE_URL$BACKDROP_SIZE$backdropPath"
        } else {
            ""
        }
    }

    /**
     * Año de lanzamiento
     */
    fun getReleaseYear(): String {
        return releaseDate.split("-").firstOrNull() ?: "N/A"
    }

    /**
     * Rating formateado
     */
    fun getFormattedRating(): String {
        return String.format("⭐ %.1f/10", voteAverage)
    }
}