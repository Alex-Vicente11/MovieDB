package com.alexvicente.moviedb.features.movie_details.data.remote.api

import com.alexvicente.moviedb.features.movie_details.data.remote.dto.MovieDetailsDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MovieDetailsApi {

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "es-MX"
    ): MovieDetailsDto
}