package com.example.apptest.data.remote.mapper

import com.example.apptest.data.remote.dto.GenreDto
import com.example.apptest.data.remote.dto.MovieDetailsDto
import com.example.apptest.data.remote.dto.MovieDto
import com.example.apptest.data.remote.mapper.MovieMapper.toDomain
import org.junit.Assert
import org.junit.Test

/**
 * TESTS PARA MovieMapper
 *
 * Tests:
 *  Transformación correcta de DTOs a modelos de dominio
 *  Manejo de valores nulos con defaults
 *  Normalización de datos (trim)
 *  Mapeo de listas
 *  Mapeo de objetos anidados (géneros)
 *
 * No testear:
 *  La serialización/deserialización de Gson (eso lo hace Gson)
 *  Las llamadas a la API (eso es responsabilidad del Repository)
 */
class MovieMapperTest {

    // ═══════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═══════════════════════════════════════════════════════

    private fun createTestMovieDto(
        id: Int = 1,
        title: String = "Test Movie",
        overview: String? = "Test overview",
        posterPath: String? = "/poster.jpg",
        backdropPath: String? = "/backdrop.jpg",
        voteAverage: Double = 7.5,
        voteCount: Int = 1000,
        releaseDate: String? = "2024-01-01",
        popularity: Double = 50.0,
        adult: Boolean = false,
        originalLanguage: String = "en",
        originalTitle: String = "Test Movie",
        video: Boolean = false,
        genreIds: List<Int>? = listOf(28, 12)
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
        adult = adult,
        originalLanguage = originalLanguage,
        originalTitle = originalTitle,
        video = video,
        genreIds = genreIds
    )

    private fun createTestMovieDetailsDto(
        id: Int = 1,
        title: String = "Test Movie",
        overview: String? = "Test overview",
        posterPath: String? = "/poster.jpg",
        backdropPath: String? = "/backdrop.jpg",
        voteAverage: Double = 7.5,
        voteCount: Int = 1000,
        releaseDate: String? = "2024-01-01",
        popularity: Double = 50.0,
        runtime: Int? = 120,
        budget: Long? = 100_000_000,
        revenue: Long? = 500_000_000,
        genres: List<GenreDto>? = listOf(GenreDto(28, "Action")),
        tagline: String? = "Test tagline",
        status: String? = "Released",
        homepage: String? = "https://test.com",
        imdbId: String? = "tt1234567",
        originalLanguage: String = "en",
        originalTitle: String = "Test Movie",
        adult: Boolean = false
    ) = MovieDetailsDto(
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
        tagline = tagline,
        status = status,
        homepage = homepage,
        imdbId = imdbId,
        originalLanguage = originalLanguage,
        originalTitle = originalTitle,
        adult = adult
    )

    private fun createTestGenreDto(
        id: Int = 28,
        name: String = "Action"
    ) = GenreDto(id = id, name = name)

    // ═══════════════════════════════════════════════════════
    // TESTS: MovieDto.toDomain()
    // ═══════════════════════════════════════════════════════

    @Test
    fun `map MovieDto to Movie with all fields correctly`() {
        // Given
        val dto = createTestMovieDto(
            id = 1,
            title = "Avengers",
            overview = "Earth's mightiest heroes",
            posterPath = "/avengers.jpg",
            backdropPath = "/avengers_backdrop.jpg",
            voteAverage = 8.0,
            voteCount = 10000,
            releaseDate = "2012-05-04",
            popularity = 100.0
        )

        // When
        val movie = dto.toDomain()

        // Then
        Assert.assertEquals(1, movie.id)
        Assert.assertEquals("Avengers", movie.title)
        Assert.assertEquals("Earth's mightiest heroes", movie.overview)
        Assert.assertEquals("/avengers.jpg", movie.posterPath)
        Assert.assertEquals("/avengers_backdrop.jpg", movie.backdropPath)
        Assert.assertEquals(8.0, movie.voteAverage, 0.001)
        Assert.assertEquals(10000, movie.voteCount)
        Assert.assertEquals("2012-05-04", movie.releaseDate)
        Assert.assertEquals(100.0, movie.popularity, 0.001)
    }

    @Test
    fun `map MovieDto with null overview returns default message`() {
        // Given
        val dto = createTestMovieDto(overview = null)

        // When
        val movie = dto.toDomain()

        // Then
        Assert.assertEquals("Sin descripción disponible", movie.overview)
    }

    @Test
    fun `map MovieDto with null posterPath returns null`() {
        // Given
        val dto = createTestMovieDto(posterPath = null)

        // When
        val movie = dto.toDomain()

        // Then
        Assert.assertNull(movie.posterPath)
    }

    @Test
    fun `map MovieDto with null backdropPath returns null`() {
        // Given
        val dto = createTestMovieDto(backdropPath = null)

        // When
        val movie = dto.toDomain()

        // Then
        Assert.assertNull(movie.backdropPath)
    }

    @Test
    fun `map MovieDto with null releaseDate returns empty string`() {
        // Given
        val dto = createTestMovieDto(releaseDate = null)

        // When
        val movie = dto.toDomain()

        // Then
        Assert.assertEquals("", movie.releaseDate)
    }

    @Test
    fun `map MovieDto trims title with leading and trailing spaces`() {
        // Given
        val dto = createTestMovieDto(title = "  Batman  ")

        // When
        val movie = dto.toDomain()

        // Then
        Assert.assertEquals("Batman", movie.title)
    }

    @Test
    fun `map MovieDto trims overview with leading and trailing spaces`() {
        // Given
        val dto = createTestMovieDto(overview = "  Great movie  ")

        // When
        val movie = dto.toDomain()

        // Then
        Assert.assertEquals("Great movie", movie.overview)
    }

    @Test
    fun `map MovieDto trims posterPath with spaces`() {
        // Given
        val dto = createTestMovieDto(posterPath = "  /poster.jpg  ")

        // When
        val movie = dto.toDomain()

        // Then
        Assert.assertEquals("/poster.jpg", movie.posterPath)
    }

    @Test
    fun `map MovieDto trims backdropPath with spaces`() {
        // Given
        val dto = createTestMovieDto(backdropPath = "  /backdrop.jpg  ")

        // When
        val movie = dto.toDomain()

        // Then
        Assert.assertEquals("/backdrop.jpg", movie.backdropPath)
    }

    @Test
    fun `map MovieDto trims releaseDate with spaces`() {
        // Given
        val dto = createTestMovieDto(releaseDate = "  2024-01-01  ")

        // When
        val movie = dto.toDomain()

        // Then
        Assert.assertEquals("2024-01-01", movie.releaseDate)
    }

    @Test
    fun `map MovieDto with zero values maps correctly`() {
        // Given
        val dto = createTestMovieDto(
            voteAverage = 0.0,
            voteCount = 0,
            popularity = 0.0
        )

        // When
        val movie = dto.toDomain()

        // Then
        Assert.assertEquals(0.0, movie.voteAverage, 0.001)
        Assert.assertEquals(0, movie.voteCount)
        Assert.assertEquals(0.0, movie.popularity, 0.001)
    }

    // ═══════════════════════════════════════════════════════
    // TESTS: List<MovieDto>.toDomain()
    // ═══════════════════════════════════════════════════════

    @Test
    fun `map empty list of MovieDto returns empty list`() {
        // Given
        val emptyList = emptyList<MovieDto>()

        // When
        val movies = emptyList.toDomain()

        // Then
        Assert.assertTrue(movies.isEmpty())
    }

    @Test
    fun `map list of MovieDto returns correct number of Movies`() {
        // Given
        val dtoList = listOf(
            createTestMovieDto(id = 1, title = "Movie 1"),
            createTestMovieDto(id = 2, title = "Movie 2"),
            createTestMovieDto(id = 3, title = "Movie 3")
        )

        // When
        val movies = dtoList.toDomain()

        // Then
        Assert.assertEquals(3, movies.size)
        Assert.assertEquals(1, movies[0].id)
        Assert.assertEquals("Movie 1", movies[0].title)
        Assert.assertEquals(2, movies[1].id)
        Assert.assertEquals("Movie 2", movies[1].title)
        Assert.assertEquals(3, movies[2].id)
        Assert.assertEquals("Movie 3", movies[2].title)
    }

    @Test
    fun `map list with mixed null values handles all correctly`() {
        // Given
        val dtoList = listOf(
            createTestMovieDto(id = 1, overview = "Has overview", posterPath = "/poster1.jpg"),
            createTestMovieDto(id = 2, overview = null, posterPath = null),
            createTestMovieDto(id = 3, overview = "Another overview", posterPath = "/poster3.jpg")
        )

        // When
        val movies = dtoList.toDomain()

        // Then
        Assert.assertEquals(3, movies.size)
        Assert.assertEquals("Has overview", movies[0].overview)
        Assert.assertNotNull(movies[0].posterPath)
        Assert.assertEquals("Sin descripción disponible", movies[1].overview)
        Assert.assertNull(movies[1].posterPath)
        Assert.assertEquals("Another overview", movies[2].overview)
        Assert.assertNotNull(movies[2].posterPath)
    }

    // ═══════════════════════════════════════════════════════
    // TESTS: MovieDetailsDto.toDomain()
    // ═══════════════════════════════════════════════════════

    @Test
    fun `map MovieDetailsDto to MovieDetails with all fields correctly`() {
        // Given
        val dto = createTestMovieDetailsDto(
            id = 1,
            title = "Inception",
            overview = "A mind-bending thriller",
            posterPath = "/inception.jpg",
            backdropPath = "/inception_backdrop.jpg",
            voteAverage = 8.8,
            voteCount = 20000,
            releaseDate = "2010-07-16",
            popularity = 150.0,
            runtime = 148,
            budget = 160_000_000,
            revenue = 829_895_144,
            genres = listOf(
                GenreDto(28, "Action"),
                GenreDto(878, "Science Fiction")
            ),
            tagline = "Your mind is the scene of the crime"
        )

        // When
        val movieDetails = dto.toDomain()

        // Then
        Assert.assertEquals(1, movieDetails.id)
        Assert.assertEquals("Inception", movieDetails.title)
        Assert.assertEquals("A mind-bending thriller", movieDetails.overview)
        Assert.assertEquals("/inception.jpg", movieDetails.posterPath)
        Assert.assertEquals("/inception_backdrop.jpg", movieDetails.backdropPath)
        Assert.assertEquals(8.8, movieDetails.voteAverage, 0.001)
        Assert.assertEquals(20000, movieDetails.voteCount)
        Assert.assertEquals("2010-07-16", movieDetails.releaseDate)
        Assert.assertEquals(150.0, movieDetails.popularity, 0.001)
        Assert.assertEquals(148, movieDetails.runtime)
        Assert.assertEquals(160_000_000L, movieDetails.budget)
        Assert.assertEquals(829_895_144L, movieDetails.revenue)
        Assert.assertEquals(2, movieDetails.genres.size)
        Assert.assertEquals("Your mind is the scene of the crime", movieDetails.tagline)
    }

    @Test
    fun `map MovieDetailsDto with null overview returns default message`() {
        // Given
        val dto = createTestMovieDetailsDto(overview = null)

        // When
        val movieDetails = dto.toDomain()

        // Then
        Assert.assertEquals("Sin descripción disponible", movieDetails.overview)
    }

    @Test
    fun `map MovieDetailsDto with null runtime returns null`() {
        // Given
        val dto = createTestMovieDetailsDto(runtime = null)

        // When
        val movieDetails = dto.toDomain()

        // Then
        Assert.assertNull(movieDetails.runtime)
    }

    @Test
    fun `map MovieDetailsDto with null budget returns null`() {
        // Given
        val dto = createTestMovieDetailsDto(budget = null)

        // When
        val movieDetails = dto.toDomain()

        // Then
        Assert.assertNull(movieDetails.budget)
    }

    @Test
    fun `map MovieDetailsDto with null revenue returns null`() {
        // Given
        val dto = createTestMovieDetailsDto(revenue = null)

        // When
        val movieDetails = dto.toDomain()

        // Then
        Assert.assertNull(movieDetails.revenue)
    }

    @Test
    fun `map MovieDetailsDto with null genres returns empty list`() {
        // Given
        val dto = createTestMovieDetailsDto(genres = null)

        // When
        val movieDetails = dto.toDomain()

        // Then
        Assert.assertTrue(movieDetails.genres.isEmpty())
    }

    @Test
    fun `map MovieDetailsDto with empty genres list returns empty list`() {
        // Given
        val dto = createTestMovieDetailsDto(genres = emptyList())

        // When
        val movieDetails = dto.toDomain()

        // Then
        Assert.assertTrue(movieDetails.genres.isEmpty())
    }

    @Test
    fun `map MovieDetailsDto with null tagline returns null`() {
        // Given
        val dto = createTestMovieDetailsDto(tagline = null)

        // When
        val movieDetails = dto.toDomain()

        // Then
        Assert.assertNull(movieDetails.tagline)
    }

    @Test
    fun `map MovieDetailsDto trims title with spaces`() {
        // Given
        val dto = createTestMovieDetailsDto(title = "  Spider-Man  ")

        // When
        val movieDetails = dto.toDomain()

        // Then
        Assert.assertEquals("Spider-Man", movieDetails.title)
    }

    @Test
    fun `map MovieDetailsDto trims overview with spaces`() {
        // Given
        val dto = createTestMovieDetailsDto(overview = "  Amazing story  ")

        // When
        val movieDetails = dto.toDomain()

        // Then
        Assert.assertEquals("Amazing story", movieDetails.overview)
    }

    @Test
    fun `map MovieDetailsDto trims tagline with spaces`() {
        // Given
        val dto = createTestMovieDetailsDto(tagline = "  The best movie ever  ")

        // When
        val movieDetails = dto.toDomain()

        // Then
        Assert.assertEquals("The best movie ever", movieDetails.tagline)
    }

    @Test
    fun `map MovieDetailsDto with multiple genres maps all correctly`() {
        // Given
        val dto = createTestMovieDetailsDto(
            genres = listOf(
                GenreDto(28, "Action"),
                GenreDto(12, "Adventure"),
                GenreDto(878, "Science Fiction"),
                GenreDto(53, "Thriller")
            )
        )

        // When
        val movieDetails = dto.toDomain()

        // Then
        Assert.assertEquals(4, movieDetails.genres.size)
        Assert.assertEquals("Action", movieDetails.genres[0].name)
        Assert.assertEquals("Adventure", movieDetails.genres[1].name)
        Assert.assertEquals("Science Fiction", movieDetails.genres[2].name)
        Assert.assertEquals("Thriller", movieDetails.genres[3].name)
    }

    // ═══════════════════════════════════════════════════════
    // TESTS: GenreDto.toDomain()
    // ═══════════════════════════════════════════════════════

    @Test
    fun `map GenreDto to Genre correctly`() {
        // Given
        val dto = createTestGenreDto(id = 28, name = "Action")

        // When
        val genre = dto.toDomain()

        // Then
        Assert.assertEquals(28, genre.id)
        Assert.assertEquals("Action", genre.name)
    }

    @Test
    fun `map GenreDto trims name with spaces`() {
        // Given
        val dto = createTestGenreDto(name = "  Drama  ")

        // When
        val genre = dto.toDomain()

        // Then
        Assert.assertEquals("Drama", genre.name)
    }

    @Test
    fun `map GenreDto with special characters in name`() {
        // Given
        val dto = createTestGenreDto(name = "Science Fiction & Fantasy")

        // When
        val genre = dto.toDomain()

        // Then
        Assert.assertEquals("Science Fiction & Fantasy", genre.name)
    }

    @Test
    fun `map GenreDto with different languages`() {
        // Given
        val dto = createTestGenreDto(name = "Acción")

        // When
        val genre = dto.toDomain()

        // Then
        Assert.assertEquals("Acción", genre.name)
    }
}