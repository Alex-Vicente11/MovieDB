package com.example.apptest.data.model



data class MovieResponse(
    val page: Int,
    val results: List<Movie>,
    val totalPages: Int,
    val totalResults: Int
)

data class Movie(
    val adult: Boolean,
    val backdropPath: String?,
    val genreIds: List<Int>,
    val id: Int,
    val originalLanguage: String,
    val originalTitle: String,
    val overview: String?,
    val popularity: Double,
    val posterPath: String?,
    val releaseDate: String?,
    val title: String,
    val vide: Boolean,
    val voteAverage: Double,
    val voteCount: Int,
) {
    fun getPosterURL(): String {
        return "https://image.tmdb.org/t/p/w500${posterPath ?: ""}"
    }

    fun getBackdropUrl(): String {
        return "https://image.tmdb.org/t/p/w780${backdropPath ?: ""}"
    }

    fun getFormatedRating(): String {
        return "%.1f/10".format(voteAverage)
    }
}