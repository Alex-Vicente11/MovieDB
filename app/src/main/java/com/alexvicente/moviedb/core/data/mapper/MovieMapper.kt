package com.alexvicente.moviedb.core.data.mapper

import com.alexvicente.moviedb.core.data.mapper.GenreMapper.toDomain
import com.alexvicente.moviedb.core.data.remote.dto.MovieDto
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.features.movie_details.data.remote.dto.MovieDetailsDto
import com.alexvicente.moviedb.features.movie_details.domain.model.MovieDetails
import com.alexvicente.moviedb.features.videos.data.remote.dto.VideoDto
import com.alexvicente.moviedb.features.videos.domain.model.Video

/**
 * MAPPER: Convierte DTOs a modelos de dominio
 * Este mapper es usado por todos los features que trabajan con Movie
 *
 * Responsabilidades:
 * - Transformar respuestas de API a modelos limpios
 * - Manejar valores nulos con defaults
 * - Normalizar datos (trim, defaults, etc.)
 */

object MovieMapper {

    /**
     * Convierte MovieDto a Movie (dominio)
     */
    fun MovieDto.toDomain(): Movie {
        return Movie(
            id = this.id,
            title = this.title.trim(),
            overview = this.overview?.trim() ?: "Sin descripción disponible",
            posterPath = this.posterPath?.trim(),
            backdropPath = this.backdropPath?.trim(),
            voteAverage = this.voteAverage,
            voteCount = this.voteCount,
            releaseDate = this.releaseDate?.trim() ?: "",
            popularity = this.popularity
        )
    }

    /**
     * Convierte lista de MovieDto a lista de Movie
     */
    fun List<MovieDto>.toDomain(): List<Movie> {
        return this.map { it.toDomain() }
    }

    /**
     * Convierte MovieDetailsDto a MovieDetails (dominio)
     */
    fun MovieDetailsDto.toDomain(): MovieDetails {
        return MovieDetails(
            id = this.id,
            title = this.title.trim(),
            overview = this.overview?.trim() ?: "Sin descripción disponible",
            posterPath = this.posterPath?.trim(),
            backdropPath = this.backdropPath?.trim(),
            voteAverage = this.voteAverage,
            voteCount = this.voteCount,
            releaseDate = this.releaseDate?.trim() ?: "",
            popularity = this.popularity,

            // Campos adicionales de detalles
            runtime = this.runtime,
            budget = this.budget,
            revenue = this.revenue,
            genres = this.genres?.map { it.toDomain() } ?: emptyList(),
            tagline = this.tagline?.trim()
        )
    }

    /**
     * Mapper para Video
     *
     * Convierte VideoDto a Video (domino)
     */
    fun VideoDto.toDomain(): Video {
        return Video(
            id = this.id,
            key = this.key,
            name = this.name.trim(),
            site = this.site,
            size = this.size,
            type = this.type,
            official = this.official,
            publishedAt = this.publishedAt,
            language = this.language,
            country = this.country
        )
    }

    /**
     * Convierte lista de VideoDto a lista de Video
     * Usado para mapear resultados de /movie/{id}/videos
     */
    fun List<VideoDto>.toDomainVideos(): List<Video> {
        return this.map { it.toDomain() }
    }
}