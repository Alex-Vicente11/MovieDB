package com.alexvicente.moviedb.core.data.mapper

import com.alexvicente.moviedb.core.data.mapper.GenreMapper.toDomain
import com.alexvicente.moviedb.core.data.mapper.MovieMapper.toDomain
import com.alexvicente.moviedb.core.data.remote.dto.GenreDto
import com.alexvicente.moviedb.core.data.remote.dto.MovieDto
import com.alexvicente.moviedb.features.movie_details.data.remote.dto.MovieDetailsDto
import org.junit.Assert
import org.junit.Test

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

        val movie = dto.toDomain()

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
        val movie = createTestMovieDto(overview = null).toDomain()
        Assert.assertEquals("Sin descripción disponible", movie.overview)
    }

    @Test
    fun `map MovieDto with null posterPath returns null`() {
        val movie = createTestMovieDto(posterPath = null).toDomain()
        Assert.assertNull(movie.posterPath)
    }

    @Test
    fun `map MovieDto with null backdropPath returns null`() {
        val movie = createTestMovieDto(backdropPath = null).toDomain()
        Assert.assertNull(movie.backdropPath)
    }

    @Test
    fun `map MovieDto with null releaseDate returns empty string`() {
        val movie = createTestMovieDto(releaseDate = null).toDomain()
        Assert.assertEquals("", movie.releaseDate)
    }

    @Test
    fun `map MovieDto trims title with leading and trailing spaces`() {
        val movie = createTestMovieDto(title = "  Batman  ").toDomain()
        Assert.assertEquals("Batman", movie.title)
    }

    @Test
    fun `map MovieDto trims overview with leading and trailing spaces`() {
        val movie = createTestMovieDto(overview = "  Great movie  ").toDomain()
        Assert.assertEquals("Great movie", movie.overview)
    }

    @Test
    fun `map MovieDto trims posterPath with spaces`() {
        val movie = createTestMovieDto(posterPath = "  /poster.jpg  ").toDomain()
        Assert.assertEquals("/poster.jpg", movie.posterPath)
    }

    @Test
    fun `map MovieDto trims backdropPath with spaces`() {
        val movie = createTestMovieDto(backdropPath = "  /backdrop.jpg  ").toDomain()
        Assert.assertEquals("/backdrop.jpg", movie.backdropPath)
    }

    @Test
    fun `map MovieDto trims releaseDate with spaces`() {
        val movie = createTestMovieDto(releaseDate = "  2024-01-01  ").toDomain()
        Assert.assertEquals("2024-01-01", movie.releaseDate)
    }

    @Test
    fun `map MovieDto with zero values maps correctly`() {
        val movie = createTestMovieDto(voteAverage = 0.0, voteCount = 0, popularity = 0.0).toDomain()
        Assert.assertEquals(0.0, movie.voteAverage, 0.001)
        Assert.assertEquals(0, movie.voteCount)
        Assert.assertEquals(0.0, movie.popularity, 0.001)
    }

    // ═══════════════════════════════════════════════════════
    // TESTS: List<MovieDto>.toDomain()
    // ═══════════════════════════════════════════════════════

    @Test
    fun `map list of MovieDto to list of Movie`() {
        val dtoList = listOf(
            createTestMovieDto(id = 1, overview = "Has overview", posterPath = "/p1.jpg"),
            createTestMovieDto(id = 2, overview = null, posterPath = null),
            createTestMovieDto(id = 3, overview = "Another overview", posterPath = "/p3.jpg")
        )

        val movies = dtoList.toDomain()

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
            genres = listOf(GenreDto(28, "Action"), GenreDto(878, "Science Fiction")),
            tagline = "Your mind is the scene of the crime"
        )

        val movieDetails = dto.toDomain()

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
        val movieDetails = createTestMovieDetailsDto(overview = null).toDomain()
        Assert.assertEquals("Sin descripción disponible", movieDetails.overview)
    }

    @Test
    fun `map MovieDetailsDto with null runtime returns null`() {
        Assert.assertNull(createTestMovieDetailsDto(runtime = null).toDomain().runtime)
    }

    @Test
    fun `map MovieDetailsDto with null budget returns null`() {
        Assert.assertNull(createTestMovieDetailsDto(budget = null).toDomain().budget)
    }

    @Test
    fun `map MovieDetailsDto with null revenue returns null`() {
        Assert.assertNull(createTestMovieDetailsDto(revenue = null).toDomain().revenue)
    }

    @Test
    fun `map MovieDetailsDto with null genres returns empty list`() {
        Assert.assertTrue(createTestMovieDetailsDto(genres = null).toDomain().genres.isEmpty())
    }

    @Test
    fun `map MovieDetailsDto with empty genres list returns empty list`() {
        Assert.assertTrue(createTestMovieDetailsDto(genres = emptyList()).toDomain().genres.isEmpty())
    }

    @Test
    fun `map MovieDetailsDto with null tagline returns null`() {
        Assert.assertNull(createTestMovieDetailsDto(tagline = null).toDomain().tagline)
    }

    @Test
    fun `map MovieDetailsDto trims title with spaces`() {
        val movieDetails = createTestMovieDetailsDto(title = "  Spider-Man  ").toDomain()
        Assert.assertEquals("Spider-Man", movieDetails.title)
    }

    @Test
    fun `map MovieDetailsDto trims overview with spaces`() {
        val movieDetails = createTestMovieDetailsDto(overview = "  Amazing story  ").toDomain()
        Assert.assertEquals("Amazing story", movieDetails.overview)
    }

    @Test
    fun `map MovieDetailsDto trims tagline with spaces`() {
        val movieDetails = createTestMovieDetailsDto(tagline = "  The best movie ever  ").toDomain()
        Assert.assertEquals("The best movie ever", movieDetails.tagline)
    }

    @Test
    fun `map MovieDetailsDto with multiple genres maps all correctly`() {
        val dto = createTestMovieDetailsDto(
            genres = listOf(
                GenreDto(28, "Action"),
                GenreDto(12, "Adventure"),
                GenreDto(878, "Science Fiction"),
                GenreDto(53, "Thriller")
            )
        )

        val movieDetails = dto.toDomain()

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
        val genre = createTestGenreDto(id = 28, name = "Action").toDomain()
        Assert.assertEquals(28, genre.id)
        Assert.assertEquals("Action", genre.name)
    }

    @Test
    fun `map GenreDto trims name with spaces`() {
        val genre = createTestGenreDto(name = "  Drama  ").toDomain()
        Assert.assertEquals("Drama", genre.name)
    }

    @Test
    fun `map GenreDto with special characters in name`() {
        val genre = createTestGenreDto(name = "Science Fiction & Fantasy").toDomain()
        Assert.assertEquals("Science Fiction & Fantasy", genre.name)
    }

    @Test
    fun `map GenreDto with different languages`() {
        val genre = createTestGenreDto(name = "Acción").toDomain()
        Assert.assertEquals("Acción", genre.name)
    }
}