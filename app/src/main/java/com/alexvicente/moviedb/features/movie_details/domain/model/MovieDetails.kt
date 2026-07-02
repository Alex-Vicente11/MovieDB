package com.alexvicente.moviedb.features.movie_details.domain.model

import com.alexvicente.moviedb.core.domain.model.Genre

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

    fun getGenresString(): String {
        return genres.joinToString(", ") { it.name }
    }

    fun getFormattedBudget(): String {
        return if (budget != null && budget > 0) {
            val millions = budget / 1_000_000.0
            "$${String.format("%.1f", millions)}M"
        } else {
            "N/A"
        }
    }

    fun getFormattedRevenue(): String {
        return if (revenue != null && revenue > 0) {
            val millions = revenue / 1_000_000.0
            "$${String.format("%.1f", millions)}M"
        } else {
            "N/A"
        }
    }
}
