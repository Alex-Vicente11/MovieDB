package com.example.apptest.features.movie_details.data.remote.api

import com.example.apptest.features.movie_details.data.remote.dto.MovieDetailsDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API especializada para detalles de películas
 *
 * UBICACIÓN: features/movie_details/data/remote/api/
 *
 * RESPONSABILIDAD: Solo endpoints relacionados con detalles de películas
 *
 * Feature Module Pattern:
 * - Esta API pertenece al feature "movie_details"
 * - NO contiene endpoints de otros features
 * - Usa DTOs específicos del feature
 *
 * Principio SOLID aplicado: Single Responsibility Principle (SRP)
 * - Una API hace UNA cosa: obtener detalles de películas
 *
 * Comparación con API monolítico:
 *  ANTES: TMDBApiService con 3+ métodos mezclados
 *  AHORA: MovieDetailsApi con 1 método especializado
 */
interface MovieDetailsApi {

    /**
     * Obtener detalles completos de una película por ID
     *
     * Endpoint: GET /movie/{movie_id}
     * Documentación: https://developer.themoviedb.org/reference/movie-details
     *
     * @param movieId ID de la película (path parameter)
     * @param language Idioma de los resultados (default: español México)
     * @return MovieDetailsDto con información completa de la película
     *
     * Campos adicionales que retorna vs búsquedas:
     * - runtime: Duración en minutos
     * - budget: Presupuesto
     * - revenue: Ingresos
     * - genres: Lista de géneros (objetos completos, no solo IDs)
     * - tagline: Frase promocional
     * - status: Estado de la película (Released, Post Production, etc.)
     * - homepage: Sitio web oficial
     *
     * Excepciones:
     * - HttpException si el servidor retorna error (4xx, 5xx)
     * - IOException si hay problemas de red
     *
     * Códigos de error comunes:
     * - 404: Película no encontrada
     * - 401: Token de autenticación inválido
     */
    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "es-MX"
    ): MovieDetailsDto
}