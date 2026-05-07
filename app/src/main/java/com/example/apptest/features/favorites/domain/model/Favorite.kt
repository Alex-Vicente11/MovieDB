package com.example.apptest.features.favorites.domain.model

data class Favorite(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val voteAverage: Double,
    val releaseDate: String,
    val overview: String,
    val addedAt: Long
) {
    companion object {
        private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"
    }

    fun getPosterUrl(): String =
        if (posterPath != null) "$IMAGE_BASE_URL$posterPath" else "N/A"

    fun getReleaseYear(): String =
        releaseDate.split("-").firstOrNull() ?: "N/A"

    fun getFormattedRating(): String =
        String.format("⭐ %.1f/10", voteAverage)
}