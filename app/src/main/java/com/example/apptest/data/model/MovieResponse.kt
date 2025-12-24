package com.example.apptest.data.model

import com.google.gson.annotations.SerializedName


data class MovieResponse(
    val page: Int,
    val results: List<Movie>,
    val totalPages: Int,
    val totalResults: Int
)

data class Movie(
    @SerializedName("adult")
    val adult: Boolean,

    @SerializedName("backdrop_path")
    val backdropPath: String?,

    @SerializedName("genre_ids")
    val genreIds: List<Int>,

    @SerializedName("id")
    val id: Int,

    @SerializedName("original_language")
    val originalLanguage: String,

    @SerializedName("original_title")
    val originalTitle: String,

    @SerializedName("overview")
    val overview: String?,

    @SerializedName("popularity")
    val popularity: Double,

    @SerializedName("poster_path")
    val posterPath: String?,

    @SerializedName("release_date")
    val releaseDate: String?,

    @SerializedName("title")
    val title: String,

    @SerializedName("video")
    val video: Boolean,

    @SerializedName("vote_average")
    val voteAverage: Double,

    @SerializedName("vote_count")
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