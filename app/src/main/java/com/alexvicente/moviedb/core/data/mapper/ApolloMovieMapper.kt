package com.alexvicente.moviedb.core.data.mapper

import com.alexvicente.moviedb.core.domain.model.Genre
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.graphql.PopularMoviesQuery

object ApolloMovieMapper {

    fun PopularMoviesQuery.Genre.toDomain(): Genre {
        return Genre(
            id = this.id,
            name = this.name.trim()
        )
    }

    fun PopularMoviesQuery.PopularMovie.toDomain(): Movie {
        return Movie(
            id = this.id,
            title = this.title.trim(),
            overview = this.overview.trim().ifBlank { "Sin descripción disponible" },
            posterPath = this.posterPath,
            backdropPath = this.backdropPath,
            voteAverage = this.voteAverage,
            voteCount = this.voteCount,
            releaseDate = this.releaseDate.trim(),
            popularity = this.popularity
        )
    }

    fun List<PopularMoviesQuery.PopularMovie>.toDomain(): List<Movie> {
        return this.map { it.toDomain() }
    }
}