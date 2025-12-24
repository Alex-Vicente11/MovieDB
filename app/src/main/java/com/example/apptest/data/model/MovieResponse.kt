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
        // Si posterPath es null o vacio, retorna string vacio
        // Glide mostrará el placeholder automaticamente
        return if (!posterPath.isNullOrEmpty()) {
            "https://image.tmdb.org/t/p/w500$posterPath"
        } else {
            ""
        }
    }

    fun getBackdropUrl(): String {
        return if (!backdropPath.isNullOrEmpty()){
            "https://image.tmdb.org/t/p/w780$backdropPath"
        } else
            ""
    }

    fun getFormatedRating(): String {
        return "%.1f/10".format(voteAverage)
    }
}