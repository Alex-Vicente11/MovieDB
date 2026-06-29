package com.alexvicente.moviedb.features.movie_details.domain.model

import com.alexvicente.moviedb.core.domain.model.Genre

/**
 * MODELO DE DOMINIO - MovieDetails
 *
 * UBICACIÓN: features/movie_details/domain/model/
 *
 * Responsabilidad:
 * - Representar información completa de una película
 * - Incluye campos adicionales que NO están en búsquedas
 *
 * Decisión de diseño:
 * ¿Por qué este modelo va en movie_details/ y NO en core/?
 *
 * - Movie.kt está en core/ porque se usa en múltiples features:
 *   * Search (lista de búsqueda)
 *   * Popular Movies (lista de populares)
 *   * Trending, Upcoming, etc. (futuro)
 *
 * - MovieDetails.kt va en movie_details/ porque:
 *   * SOLO se usa en la pantalla de detalles
 *   * NO se usa en listas/búsquedas
 *   * Es específico de UN solo feature
 *
 * Principio YAGNI: "You Aren't Gonna Need It"
 * - No lo movemos a core hasta que otro feature lo necesite
 *
 * Cambios vs versión legacy:
 * ✅ Package actualizado: movies.domain.model → movie_details.domain.model
 * ✅ Documentación mejorada
 */
data class MovieDetails(
    val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double,
    val voteCount: Int,
    val releaseDate: String,
    val popularity: Double,

    // Campos adicionales solo en detalles
    val runtime: Int?,
    val budget: Long?,
    val revenue: Long?,
    val genres: List<Genre>,
    val tagline: String?
) {
    companion object {
        private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"
        private const val POSTER_SIZE = "w500"
        private const val BACKDROP_SIZE = "w780"
    }

    fun getPosterUrl(): String {
        return if (posterPath != null) {
            "$IMAGE_BASE_URL$POSTER_SIZE$posterPath"
        } else {
            ""
        }
    }

    fun getBackdropUrl(): String {
        return if (backdropPath != null) {
            "$IMAGE_BASE_URL$BACKDROP_SIZE$backdropPath"
        } else {
            ""
        }
    }

    fun getReleaseYear(): String {
        return releaseDate.split("-").firstOrNull() ?: "N/A"
    }

    fun getFormattedRating(): String {
        return String.format("⭐ %.1f/10", voteAverage)
    }

    /**
     * Duración formateada (ej: "2h 28min")
     */
    fun getFormattedRuntime(): String {
        return if (runtime != null && runtime > 0) {
            val hours = runtime / 60
            val minutes = runtime % 60
            if (hours > 0) {
                "${hours}h ${minutes}min"
            } else {
                "${minutes}min"
            }
        } else {
            "N/A"
        }
    }

    /**
     * Géneros como string (ej: "Action, Sci-Fi")
     */
    fun getGenresString(): String {
        return genres.joinToString(", ") { it.name }
    }

    /**
     * Presupuesto formateado (ej: "$63M")
     */
    fun getFormattedBudget(): String {
        return if (budget != null && budget > 0) {
            val millions = budget / 1_000_000.0
            "$${String.format("%.1f", millions)}M"
        } else {
            "N/A"
        }
    }

    /**
     * Ingresos formateados (ej: "$467M")
     */
    fun getFormattedRevenue(): String {
        return if (revenue != null && revenue > 0) {
            val millions = revenue / 1_000_000.0
            "$${String.format("%.1f", millions)}M"
        } else {
            "N/A"
        }
    }
}
