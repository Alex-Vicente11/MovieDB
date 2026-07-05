package com.alexvicente.moviedb.features.popular_movies.data.remote.api

import com.alexvicente.moviedb.core.data.remote.dto.MovieResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface PopularMoviesApi {

    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("language") language: String = "es-MX",
        @Query("page") page: Int = 1
    ): MovieResponseDto
}