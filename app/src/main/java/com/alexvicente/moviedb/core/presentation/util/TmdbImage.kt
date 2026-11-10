package com.alexvicente.moviedb.core.presentation.util

object TmdbImage {
    private const val BASE_URL = "https://image.tmdb.org/t/p/"
    private const val POSTER_SIZE = "w342"
    private const val BACKDROP_SIZE = "w780"
    private const val PROFILE_SIZE = "w185"
    private const val STILL_SIZE = "w300"

    fun poster(path: String?): String? = path?.let { "$BASE_URL$POSTER_SIZE$it" }
    fun backdrop(path: String?): String? = path?.let { "$BASE_URL$BACKDROP_SIZE$it" }
    fun profile(path: String?): String? = path?.let { "$BASE_URL$PROFILE_SIZE$it"}
    fun still(path: String?): String? = path?.let { "$BASE_URL$STILL_SIZE$it"}
}