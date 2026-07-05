package com.alexvicente.moviedb.core.data.mapper

import com.alexvicente.moviedb.core.data.mapper.GenreMapper.toDomain
import com.alexvicente.moviedb.core.data.remote.dto.MovieDto
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.features.movie_details.data.remote.dto.MovieDetailsDto
import com.alexvicente.moviedb.features.movie_details.domain.model.MovieDetails
import com.alexvicente.moviedb.features.videos.data.remote.dto.VideoDto
import com.alexvicente.moviedb.features.videos.domain.model.Video

object MovieMapper {

    fun MovieDto. toDomain(): Movie {
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

    fun List<MovieDto>.toDomain(): List<Movie> {
        return this.map { it.toDomain() }
    }

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

    fun List<VideoDto>.toDomainVideos(): List<Video> {
        return this.map { it.toDomain() }
    }
}