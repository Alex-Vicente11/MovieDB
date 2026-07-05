package com.alexvicente.moviedb.features.genres.data.remote.api

import com.alexvicente.moviedb.core.data.remote.dto.MovieResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface MoviesByGenreApi {

    @GET("discover/movie")
    suspend fun getMoviesByGenre(
        @Query("with_genres") genreId: Int,
        @Query("page") page: Int,
        @Query("language") language: String,
        @Query("sort_by") sortBy: String = "popularity.desc"
    ): MovieResponseDto
}