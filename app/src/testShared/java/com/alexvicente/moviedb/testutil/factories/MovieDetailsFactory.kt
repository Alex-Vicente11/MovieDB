package com.alexvicente.moviedb.testutil.factories

import com.alexvicente.moviedb.core.domain.model.Genre
import com.alexvicente.moviedb.features.movie_details.domain.model.MovieDetails

object MovieDetailsFactory {

    fun createGenre(
        id: Int = 28,
        name: String = "Action"
    ) = Genre(id = id, name = name)

    fun createMovieDetails(
        id: Int = 1,
        title: String = "Inception",
        overview: String = "A thief who steals corporate secrets",
        // null: evita que Glide intente descargar imágenes reales en los tests instrumentados
        posterPath: String? = null,
        backdropPath: String? = null,
        voteAverage: Double = 8.8,
        voteCount: Int = 30000,
        releaseDate: String = "2010-07-16",
        popularity: Double = 100.0,
        runtime: Int? = 148,
        budget: Long? = 160_000_000L,
        revenue: Long? = 829_895_144L,
        genres: List<Genre> = listOf(
            createGenre(id = 28, name = "Action"),
            createGenre(id = 878, name = "Science Fiction")
        ),
        tagline: String? = "Your mind is the scene of the crime"
    ) = MovieDetails(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        voteCount = voteCount,
        releaseDate = releaseDate,
        popularity = popularity,
        runtime = runtime,
        budget = budget,
        revenue = revenue,
        genres = genres,
        tagline = tagline
    )
}