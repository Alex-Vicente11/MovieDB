package com.example.apptest.movies.data.remote.api

import com.example.apptest.features.movie_details.data.remote.dto.MovieDetailsDto
import com.example.apptest.core.data.remote.dto.MovieResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Interface de Retrofit para la API de TMDB
 *
 * Cambios vs versión anterior:
 * ✅ Usa DTOs en lugar de modelos de dominio
 * ✅ Retorna DTOs directamente (sin Response<T>)
 * ✅ Excepciones manejadas en el Repository
 */
interface TMDBApiService {

    /**
     * Buscar películas por query
     *
     * Lanza HttpException si falla (código 4xx o 5xx)
     * Lanza IOException si hay error de red
     */
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("language") language: String = "es-MX",
        @Query("page") page: Int = 1
    ): MovieResponseDto

    /**
     * Obtener películas populares
     */
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("language") language: String = "es-MX",
        @Query("page") page: Int = 1
    ): MovieResponseDto

    /**
     * Obtener detalles de una película
     */
    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "es-MX"
    ): MovieDetailsDto
}