package com.example.apptest.features.popular_movies.data.remote.api

import com.example.apptest.core.data.remote.dto.MovieResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API especializada para películas populares
 *
 * RESPONSABILIDAD: Solo endpoints relacionados con películas populares
 *
 * Feature Module Pattern:
 * - Esta API pertenece al feature "popular_movies"
 * - NO contiene endpoints de otros features
 * - Usa DTOs compartidos desde core (MovieResponseDto)
 *
 * Principio SOLID aplicado: Single Responsibility Principle (SRP)
 * - Una API hace UNA cosa: gestionar películas populares
 */
interface PopularMoviesApi {

    /**
     * Obtener películas populares de TMDB
     *
     * Endpoint: GET /movie/popular
     * Documentación: https://developer.themoviedb.org/reference/movie-popular-list
     *
     * @param language Idioma de los resultados (default: español México)
     * @param page Número de página para paginación (default: 1)
     * @return MovieResponseDto con lista de películas populares
     *
     * Excepciones:
     * - HttpException si el servidor retorna error (4xx, 5xx)
     * - IOException si hay problemas de red
     */
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("language") language: String = "es-MX",
        @Query("page") page: Int = 1
    ): MovieResponseDto
}