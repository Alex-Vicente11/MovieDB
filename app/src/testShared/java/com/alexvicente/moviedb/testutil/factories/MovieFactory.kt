package com.alexvicente.moviedb.testutil.factories

import com.alexvicente.moviedb.core.data.remote.dto.MovieDto
import com.alexvicente.moviedb.core.data.remote.dto.MovieResponseDto
import com.alexvicente.moviedb.core.domain.model.Movie

object MovieFactory {
    // Movie (modelo de dominio)
    fun createMovie(
        id: Int = 1,
        title: String = "Inception",
        overview: String = "A thief who steals corporate secrets",
        posterPath: String? = "/poster.jpg",
        backdropPath: String? = "/backdrop.jpg",
        voteAverage: Double = 8.8,
        voteCount: Int = 30000,
        releaseDate: String = "2010-07-16",
        popularity: Double = 100.0
    ) = Movie(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        voteCount = voteCount,
        releaseDate = releaseDate,
        popularity = popularity
    )

    fun createMovieList(size: Int): List<Movie> =
        (1..size).map { index ->
            createMovie(
                id = index,
                title = "Movie $index",
                popularity = (100 - index).toDouble() // popularidad decreciente, orden realista
            )
        }

    // MovieDto (capa de datos - respuesta cruda de la API)
    fun createMovieDto(
        id: Int = 1,
        title: String = "Inception",
        overview: String? = "A thief who steals corporate secrets",
        posterPath: String? = "/poster.jpg",
        backdropPath: String? = "/backdrop.jpg",
        voteAverage: Double = 8.8,
        voteCount: Int = 30000,
        releaseDate: String? = "2010-07-16",
        popularity: Double = 100.0
    ) = MovieDto(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        voteCount = voteCount,
        releaseDate = releaseDate,
        popularity = popularity,
        // campos obligatorios del DTO que no usa el dominio
        adult = false,
        originalLanguage = "en",
        originalTitle = title,
        video = false,
        genreIds = listOf(28, 12)
    )

    fun createMovieResponseDto(size: Int = 1) = MovieResponseDto(
        page = 1,
        totalPages = 1,
        totalResults = size,
        results = (1..size).map { index ->
            createMovieDto(id = index, title = "Movie $index")
        }
    )

    fun createMovieDtoList(size: Int): List<MovieDto> =
        (1..size).map { i ->
            createMovieDto(
                id = i,
                title = "Movie $i",
                popularity = (100 - i).toDouble() // popularidad decreciente, orden realista
            )
        }
}