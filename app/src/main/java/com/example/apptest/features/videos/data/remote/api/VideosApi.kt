package com.example.apptest.features.videos.data.remote.api

import com.example.apptest.features.videos.data.remote.dto.VideoResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 *  Api especializada para videos de peliculas
 *
 *  Responsabilidad: Solo endpoints relacionados con videos/trailers
 */

interface VideosApi {

    /**
     * Obtener videos/trailers de una pelicula
     *
     * Endpoint: GET /movie/{movie_id}/videos
     * Documentacion: https://developer.themoviedb.org/reference/movie-videos
     *
     * @param movie ID de la pelicula (path parameter)
     * @param language Idioma de los resultados (default: españos México)
     * @return VideoResponseDto con lista de videos
     *
     * Tipos de videos que puede retornar
     * - Trailer: Oficial de la pelicla
     * - Teaser: Teaser corto
     * - Clip: Clip/escena de la pelicula
     * - Featurette: Video especial/making of
     * Behind the Scenes: Detrás de cámaras
     *
     * Plataformas soportadas:
     * Youtube o Vimeo
     *
     * HttpException si el servidor retorna error
     * IOException si hay problemas de red
     *
     * Códigos de error comunes:
     * 404: Pelicula no encontrada o sin videos
     * 401: Token de autenticación inválido
     */

    @GET("movie/{movie_id}/videos")
    suspend fun getMovieVideos(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "es-MX"
    ): VideoResponseDto
}