package com.example.apptest.data.repository

import com.example.apptest.movies.data.remote.api.TMDBApiService
import com.example.apptest.movies.data.remote.dto.GenreDto
import com.example.apptest.movies.data.remote.dto.MovieDetailsDto
import com.example.apptest.movies.data.remote.dto.MovieDto
import com.example.apptest.movies.data.remote.dto.MovieResponseDto
import com.example.apptest.movies.data.repository.MovieRepositoryImpl
import com.example.apptest.core.data.util.Resource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * TESTS PARA MovieRepositoryImpl
 *
 * tests:
 *  Emisión correcta de estados (Loading → Success/Error)
 *  Llamadas a la API con parámetros correctos
 *  Mapeo de DTOs a modelos de dominio
 *  Manejo de diferentes tipos de excepciones
 *  Mensajes de error específicos por código HTTP
 *
 * UseCaseTest:
 *  Mockear respuestas de API (DTOs)
 *  Mockear excepciones HTTP con códigos específicos
 *  Test transformación DTO → Domain
 *  Test múltiples emisiones en Flow
 *
 * No testear:
 *  La red real (use mocks)
 *  Retrofit (test Retrofit)
 *  La lógica de los mappers (está en MovieMapperTest)
 */
class MovieRepositoryImplTest {

    // Mocks
    private lateinit var mockApi: TMDBApiService
    private lateinit var repository: MovieRepositoryImpl

    @Before
    fun setup() {
        mockApi = mockk()
        repository = MovieRepositoryImpl(mockApi)
    }

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
        popularity: Double = 50.0
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
        adult = false,
        originalLanguage = "en",
        originalTitle = title,
        video = false,
        genreIds = listOf(28)
    )

    private fun createTestMovieResponseDto(
        movies: List<MovieDto> = listOf(createTestMovieDto())
    ) = MovieResponseDto(
        page = 1,
        results = movies,
        totalPages = 1,
        totalResults = movies.size
    )

    private fun createTestMovieDetailsDto(
        id: Int = 1,
        title: String = "Test Movie",
        overview: String? = "Test overview",
        runtime: Int? = 120,
        genres: List<GenreDto>? = listOf(GenreDto(28, "Action"))
    ) = MovieDetailsDto(
        id = id,
        title = title,
        overview = overview,
        posterPath = "/poster.jpg",
        backdropPath = "/backdrop.jpg",
        voteAverage = 7.5,
        voteCount = 1000,
        releaseDate = "2024-01-01",
        popularity = 50.0,
        runtime = runtime,
        budget = 100_000_000,
        revenue = 500_000_000,
        genres = genres,
        tagline = "Test tagline",
        status = "Released",
        homepage = "https://test.com",
        imdbId = "tt1234567",
        originalLanguage = "en",
        originalTitle = title,
        adult = false
    )

    /**
     * Helper para crear HttpException con código específico
     */
    private fun createHttpException(code: Int): HttpException {
        val responseBody = "".toResponseBody("text/plain".toMediaTypeOrNull())
        return HttpException(Response.error<Any>(code, responseBody))
    }

    // ═══════════════════════════════════════════════════════
    // TESTS: searchMovies() - SUCCESS CASES
    // ═══════════════════════════════════════════════════════

    @Test
    fun `searchMovies emits Loading then Success when API call succeeds`() = runTest {
        // Given
        val query = "avengers"
        val mockDto = createTestMovieDto(id = 1, title = "Avengers")
        val mockResponse = createTestMovieResponseDto(listOf(mockDto))

        coEvery { mockApi.searchMovies(query = query) } returns mockResponse

        // When
        val results = repository.searchMovies(query).toList()

        // Then
        assertEquals(2, results.size)
        assertTrue("First emission should be Loading", results[0] is Resource.Loading)
        assertTrue("Second emission should be Success", results[1] is Resource.Success)

        val successData = (results[1] as Resource.Success).data
        assertNotNull(successData)
        assertEquals(1, successData.size)
        assertEquals(1, successData[0].id)
        assertEquals("Avengers", successData[0].title)
    }

    @Test
    fun `searchMovies calls API with correct query parameter`() = runTest {
        // Given
        val query = "batman"
        val mockResponse = createTestMovieResponseDto()

        coEvery { mockApi.searchMovies(query = query) } returns mockResponse

        // When
        repository.searchMovies(query).toList()

        // Then
        coVerify(exactly = 1) { mockApi.searchMovies(query = query) }
    }

    @Test
    fun `searchMovies maps multiple movies correctly`() = runTest {
        // Given
        val query = "star wars"
        val mockMovies = listOf(
            createTestMovieDto(id = 1, title = "Star Wars: A New Hope"),
            createTestMovieDto(id = 2, title = "Star Wars: The Empire Strikes Back"),
            createTestMovieDto(id = 3, title = "Star Wars: Return of the Jedi")
        )
        val mockResponse = createTestMovieResponseDto(mockMovies)

        coEvery { mockApi.searchMovies(query = query) } returns mockResponse

        // When
        val results = repository.searchMovies(query).toList()

        // Then
        val successData = (results[1] as Resource.Success).data
        assertEquals(3, successData.size)
        assertEquals("Star Wars: A New Hope", successData[0].title)
        assertEquals("Star Wars: The Empire Strikes Back", successData[1].title)
        assertEquals("Star Wars: Return of the Jedi", successData[2].title)
    }

    @Test
    fun `searchMovies returns empty list when API returns empty results`() = runTest {
        // Given
        val query = "xyznotfound123"
        val mockResponse = createTestMovieResponseDto(emptyList())

        coEvery { mockApi.searchMovies(query = query) } returns mockResponse

        // When
        val results = repository.searchMovies(query).toList()

        // Then
        val successData = (results[1] as Resource.Success).data
        assertTrue(successData.isEmpty())
    }

    // ═══════════════════════════════════════════════════════
    // TESTS: searchMovies() - ERROR CASES
    // ═══════════════════════════════════════════════════════

    @Test
    fun `searchMovies emits Loading then Error when HttpException 401`() = runTest {
        // Given
        val query = "test"
        coEvery { mockApi.searchMovies(query = query) } throws createHttpException(401)

        // When
        val results = repository.searchMovies(query).toList()

        // Then
        assertEquals(2, results.size)
        assertTrue("First emission should be Loading", results[0] is Resource.Loading)
        assertTrue("Second emission should be Error", results[1] is Resource.Error)
        assertEquals(
            "Error de autenticación. Verifica el token",
            (results[1] as Resource.Error).message
        )
    }

    @Test
    fun `searchMovies emits Loading then Error when HttpException 404`() = runTest {
        // Given
        val query = "test"
        coEvery { mockApi.searchMovies(query = query) } throws createHttpException(404)

        // When
        val results = repository.searchMovies(query).toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[1] is Resource.Error)
        assertEquals(
            "No se encontraron películas",
            (results[1] as Resource.Error).message
        )
    }

    @Test
    fun `searchMovies emits Loading then Error when HttpException 500`() = runTest {
        // Given
        val query = "test"
        coEvery { mockApi.searchMovies(query = query) } throws createHttpException(500)

        // When
        val results = repository.searchMovies(query).toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[1] is Resource.Error)
        assertEquals(
            "Error del servidor: 500",
            (results[1] as Resource.Error).message
        )
    }

    @Test
    fun `searchMovies emits Loading then Error when IOException occurs`() = runTest {
        // Given
        val query = "test"
        coEvery { mockApi.searchMovies(query = query) } throws IOException("Network error")

        // When
        val results = repository.searchMovies(query).toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[1] is Resource.Error)
        assertEquals(
            "Error de conexión. Verifica tu internet.",
            (results[1] as Resource.Error).message
        )
    }

    @Test
    fun `searchMovies emits Loading then Error when generic Exception occurs`() = runTest {
        // Given
        val query = "test"
        coEvery { mockApi.searchMovies(query = query) } throws Exception("Unknown error")

        // When
        val results = repository.searchMovies(query).toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[1] is Resource.Error)
        assertEquals(
            "Unknown error",
            (results[1] as Resource.Error).message
        )
    }

    // ═══════════════════════════════════════════════════════
    // TESTS: getPopularMovies() - SUCCESS CASES
    // ═══════════════════════════════════════════════════════

    @Test
    fun `getPopularMovies emits Loading then Success when API call succeeds`() = runTest {
        // Given
        val mockMovies = listOf(
            createTestMovieDto(id = 1, title = "Popular Movie 1"),
            createTestMovieDto(id = 2, title = "Popular Movie 2")
        )
        val mockResponse = createTestMovieResponseDto(mockMovies)

        coEvery { mockApi.getPopularMovies() } returns mockResponse

        // When
        val results = repository.getPopularMovies().toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[0] is Resource.Loading)
        assertTrue(results[1] is Resource.Success)

        val successData = (results[1] as Resource.Success).data
        assertEquals(2, successData.size)
        assertEquals("Popular Movie 1", successData[0].title)
        assertEquals("Popular Movie 2", successData[1].title)
    }

    @Test
    fun `getPopularMovies calls API without parameters`() = runTest {
        // Given
        val mockResponse = createTestMovieResponseDto()
        coEvery { mockApi.getPopularMovies() } returns mockResponse

        // When
        repository.getPopularMovies().toList()

        // Then
        coVerify(exactly = 1) { mockApi.getPopularMovies() }
    }

    // ═══════════════════════════════════════════════════════
    // TESTS: getPopularMovies() - ERROR CASES
    // ═══════════════════════════════════════════════════════

    @Test
    fun `getPopularMovies emits Loading then Error when HttpException occurs`() = runTest {
        // Given
        coEvery { mockApi.getPopularMovies() } throws createHttpException(500)

        // When
        val results = repository.getPopularMovies().toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[1] is Resource.Error)
        assertEquals(
            "Error del servidor: 500",
            (results[1] as Resource.Error).message
        )
    }

    @Test
    fun `getPopularMovies emits Loading then Error when IOException occurs`() = runTest {
        // Given
        coEvery { mockApi.getPopularMovies() } throws IOException("Network error")

        // When
        val results = repository.getPopularMovies().toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[1] is Resource.Error)
        assertEquals(
            "Error de conexión. Verifica tu internet.",
            (results[1] as Resource.Error).message
        )
    }

    // ═══════════════════════════════════════════════════════
    // TESTS: getMovieDetails() - SUCCESS CASES
    // ═══════════════════════════════════════════════════════

    @Test
    fun `getMovieDetails emits Loading then Success when API call succeeds`() = runTest {
        // Given
        val movieId = 1
        val mockDetailsDto = createTestMovieDetailsDto(
            id = movieId,
            title = "Inception",
            runtime = 148
        )

        coEvery { mockApi.getMovieDetails(movieId = movieId) } returns mockDetailsDto

        // When
        val results = repository.getMovieDetails(movieId).toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[0] is Resource.Loading)
        assertTrue(results[1] is Resource.Success)

        val successData = (results[1] as Resource.Success).data
        assertEquals(1, successData.id)
        assertEquals("Inception", successData.title)
        assertEquals(148, successData.runtime)
    }

    @Test
    fun `getMovieDetails calls API with correct movieId parameter`() = runTest {
        // Given
        val movieId = 550
        val mockDetailsDto = createTestMovieDetailsDto(id = movieId)

        coEvery { mockApi.getMovieDetails(movieId = movieId) } returns mockDetailsDto

        // When
        repository.getMovieDetails(movieId).toList()

        // Then
        coVerify(exactly = 1) { mockApi.getMovieDetails(movieId = movieId) }
    }

    @Test
    fun `getMovieDetails maps genres correctly`() = runTest {
        // Given
        val movieId = 1
        val genres = listOf(
            GenreDto(28, "Action"),
            GenreDto(878, "Science Fiction")
        )
        val mockDetailsDto = createTestMovieDetailsDto(id = movieId, genres = genres)

        coEvery { mockApi.getMovieDetails(movieId = movieId) } returns mockDetailsDto

        // When
        val results = repository.getMovieDetails(movieId).toList()

        // Then
        val successData = (results[1] as Resource.Success).data
        assertEquals(2, successData.genres.size)
        assertEquals("Action", successData.genres[0].name)
        assertEquals("Science Fiction", successData.genres[1].name)
    }

    // ═══════════════════════════════════════════════════════
    // TESTS: getMovieDetails() - ERROR CASES
    // ═══════════════════════════════════════════════════════

    @Test
    fun `getMovieDetails emits Loading then Error when HttpException 404`() = runTest {
        // Given
        val movieId = 999999
        coEvery { mockApi.getMovieDetails(movieId = movieId) } throws createHttpException(404)

        // When
        val results = repository.getMovieDetails(movieId).toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[1] is Resource.Error)
        assertEquals(
            "Película no encontrada (ID: $movieId)",
            (results[1] as Resource.Error).message
        )
    }

    @Test
    fun `getMovieDetails emits Loading then Error when HttpException 401`() = runTest {
        // Given
        val movieId = 1
        coEvery { mockApi.getMovieDetails(movieId = movieId) } throws createHttpException(401)

        // When
        val results = repository.getMovieDetails(movieId).toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[1] is Resource.Error)
        assertEquals(
            "Error de autenticación",
            (results[1] as Resource.Error).message
        )
    }

    @Test
    fun `getMovieDetails emits Loading then Error when IOException occurs`() = runTest {
        // Given
        val movieId = 1
        coEvery { mockApi.getMovieDetails(movieId = movieId) } throws IOException("Network error")

        // When
        val results = repository.getMovieDetails(movieId).toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[1] is Resource.Error)
        assertEquals(
            "Error de conexión. Verifica tu internet.",
            (results[1] as Resource.Error).message
        )
    }

    @Test
    fun `getMovieDetails emits Loading then Error when generic Exception occurs`() = runTest {
        // Given
        val movieId = 1
        coEvery { mockApi.getMovieDetails(movieId = movieId) } throws Exception("Unknown error")

        // When
        val results = repository.getMovieDetails(movieId).toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[1] is Resource.Error)
        assertEquals(
            "Unknown error",
            (results[1] as Resource.Error).message
        )
    }
}