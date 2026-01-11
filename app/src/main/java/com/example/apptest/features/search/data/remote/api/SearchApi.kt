package com.example.apptest.features.search.data.remote.api

import com.example.apptest.movies.data.remote.dto.MovieResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API para búsqueda de películas
 *
 * Feature independiente - solo endpoint de search
 */
interface SearchApi {

    /**
     * Buscar películas por query
     *
     * Endpoint: GET /search/movie
     */
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("language") language: String = "es-MX",
        @Query("page") page: Int = 1
    ): MovieResponseDto
}