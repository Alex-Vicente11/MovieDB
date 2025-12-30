package com.example.apptest.data.model

import androidx.compose.runtime.rememberUpdatedState
import com.google.gson.annotations.SerializedName


data class MovieResponse(
    val page: Int,
    val results: List<Movie>,
    @SerializedName("total_pages")
    val totalPages: Int,
    @SerializedName("total_results")
    val totalResults: Int
)

data class Movie(
    @SerializedName("adult")
    val adult: Boolean,

    @SerializedName("backdrop_path")
    val backdropPath: String?,

    @SerializedName("genre_ids")
    val genreIds: List<Int>? = null,    // solo en listas

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

    // endpoint GET("movie/movie_id") (en details)
    @SerializedName("genres")
    val genres: List<Genre>? = null,

    @SerializedName("budget")
    val budget: Long? = null,

    @SerializedName("revenue")
    val revenue: Long? = null,

    @SerializedName("runtime")
    val runtime: Int? = null,   // Duración en minutos

    @SerializedName("tagline")
    val tagline: String? = null,

    @SerializedName("status")
    val status: String? = null,     // "Released", "Post Production", etc.

    @SerializedName("homepage")
    val homepage: String? = null,

    @SerializedName("imdb_id")
    val imdbId: String? = null
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

    // Formatear duracion
    fun getFormattedRuntime(): String {
        return if (runtime != null && runtime > 0) {
            val hours = runtime / 60
            val minutes = runtime % 60
            "${hours}h ${minutes}min"
        } else {
            "N/A"
        }
    }

    // Formatear presupuesto/revenue
    fun getFormattedBudget(): String {
        return if (budget != null && budget > 0) {
            "$${budget / 1_000_000}M"
        } else {
            "N/A"
        }
    }

    fun getFormattedRevenue(): String {
        return if (revenue != null && revenue > 0) {
            "$${revenue / 1_000_000}M"
        } else {
            "N/A"
        }
    }

    // Obtener generos como string
    fun getGenresString(): String {
        return genres?.joinToString(", ") { it.name } ?: "N/A"
    }
}

data class Genre(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
)