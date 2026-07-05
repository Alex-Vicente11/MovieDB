package com.alexvicente.moviedb.features.search.data.remote.api

import com.alexvicente.moviedb.core.data.remote.dto.MovieResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchApi {

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("language") language: String = "es-MX",
        @Query("page") page: Int = 1
    ): MovieResponseDto
}