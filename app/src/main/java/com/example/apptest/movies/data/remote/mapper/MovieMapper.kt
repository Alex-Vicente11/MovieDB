package com.example.apptest.movies.data.remote.mapper

import com.example.apptest.movies.data.remote.dto.GenreDto
import com.example.apptest.movies.data.remote.dto.MovieDetailsDto
import com.example.apptest.movies.data.remote.dto.MovieDto
import com.example.apptest.movies.domain.model.Genre
import com.example.apptest.movies.domain.model.Movie
import com.example.apptest.movies.domain.model.MovieDetails

/**
 * MAPPER: Convierte DTOs a modelos de dominio
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
     * Convierte GenreDto a Genre (dominio)
     */
    fun GenreDto.toDomain(): Genre {
        return Genre(
            id = this.id,
            name = this.name.trim()
        )
    }
}