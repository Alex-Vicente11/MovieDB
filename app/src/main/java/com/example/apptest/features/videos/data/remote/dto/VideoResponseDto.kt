package com.example.apptest.features.videos.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para la respuesta de videos de TMDB
 *
 * UBICACIÓN: features/videos/data/remote/dto/
 *
 * Endpoint: GET /movie/{movie_id}/videos
 * Documentación: https://developer.themoviedb.org/reference/movie-videos
 *
 * Responsabilidad:
 * - Representar la estructura JSON de la respuesta de la API
 * - Contiene el ID de la película y la lista de videos
 *
 * Ejemplo de respuesta:
 * ```json
 * {
 *   "id": 550,
 *   "results": [
 *     {
 *       "key": "V0Fqdb-smqo",
 *       "name": "Official Trailer",
 *       "site": "YouTube",
 *       "type": "Trailer",
 *       ...
 *     }
 *   ]
 * }
 * ```
 */

data class VideoResponseDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("results")
    val results: List<VideoDto>
)
