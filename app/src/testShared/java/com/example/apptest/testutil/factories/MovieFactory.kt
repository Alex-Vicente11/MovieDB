package com.example.apptest.testutil.factories

import com.example.apptest.core.data.remote.dto.MovieDto
import com.example.apptest.core.data.remote.dto.MovieResponseDto
import com.example.apptest.core.domain.model.Movie

/**
 * Responsabilidad: Centralizar la creación de objetos Movie y MovieDto para tests.
 *
 * Sin esta factory, cada archivo de test define su propio createTestMovie()
 * Cuando Movie agrega o cambia un campo, hay que actualizar N archivos.
 * Con MovieFactory, el cambio se hace en un solo lugar.
 *
 * Patrón aplicado: Object Mother
 * Provee objetos preconfigurados con valores realistas y semánticos.
 * Los tests sobreescriben solo los campos relevantes para su caso
 */

object MovieFactory {
    // Movie (modelo de dominio)
    /**
     * val movie = MovieFactory.createMovie()
     * val movie = MovieFactory.createMovie(id = 42, title = "batman")
     */
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

    /**
     * Crea una lista de Movies con IDs y títulos únicos.
     * Útil para tests que necesitan verificar tamaño o contenido de listas.
     *
     * Uso:
     *      val movies = MovieFactory.createMovies(3)
     *      -> [Movie(id=1, title="Movie 1"), Movie(id=2, title="Movie 2"), Movie(id=3, title="Movie 3")]
     */
    fun createMovieList(size: Int): List<Movie> =
        (1..size).map { index ->
            createMovie(
                id = index,
                title = "Movie $index",
                popularity = (100 - index).toDouble() // popularidad decreciente, orden realista
            )
        }

    // MovieDto (capa de datos - respuesta cruda de la API)
    /**
     * Crea un MovieDto que representa la respuesta JSON de TMDB
     * Útil para tests del repositorio y del mapper.
     *
     * La distinción con createMovie() es importante:
     *   MovieDto -> llega a la API, puede tener nulls, campos extra (adult, video...)
     *   Movie -> modelo limpio de dominio, ya procesado por el mapper
     */
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

    /**
     * Crea un MovieResponseDto que envuelve una lista de MovieDto.
     * Representa la respuesta completa del endpoint /search/movie o /movie/popular
     *
     * MockWebServer necesita JSON, pero este objeto es útil para tests
     * que mockean el API directamente con Mockk en lugar de MockWebServer
     *
     * Uso:
     *    coEvery { api.getPopularMovies() } returns MovieFactory.createMovieResponseDto(size = 5)
     */
    fun createMovieResponseDto(size: Int = 1) = MovieResponseDto(
        page = 1,
        totalPages = 1,
        totalResults = size,
        results = (1..size).map { index ->
            createMovieDto(id = index, title = "Movie $index")
        }
    )
}