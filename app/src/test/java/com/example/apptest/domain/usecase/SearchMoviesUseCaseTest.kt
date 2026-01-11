package com.example.apptest.domain.usecase

import com.example.apptest.core.domain.model.Movie
import com.example.apptest.movies.domain.repository.MovieRepository
import com.example.apptest.movies.domain.usecase.SearchMoviesUseCase
import com.example.apptest.core.data.util.Resource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * TESTS PARA SearchMoviesUseCase
 *
 * Se testea:
 *  Validaciones de negocio (query vacío, muy corto)
 *  Delegación correcta al repositorio
 *  Transformación del query (trim)
 *  Propagación de estados del repositorio
 *
 * No tests:
 *  Lógica del repositorio (eso va en MovieRepositoryImplTest)
 *  Llamadas a la API (eso es integración, no unitario)
 */
class SearchMoviesUseCaseTest {

    // Mock del repositorio (simulacion)
    private lateinit var mockRepository: MovieRepository

    // El use case que vamos a testear (usa el mock)
    private lateinit var useCase: SearchMoviesUseCase

    /**
     * Setup: Se ejecuta ANTES de cada test
     * Crea instancias frescas para cada test (aislamiento)
     */
    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = SearchMoviesUseCase(mockRepository)
    }

    // ═══════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═══════════════════════════════════════════════════════

    /**
     * Función helper para crear Movies de prueba
     * Simplifica la creación de objetos en los tests
     */
    private fun createTestMovie(
        id: Int = 1,
        title: String = "Test Movie",
        overview: String = "Test overview",
        posterPath: String? = null,
        backdropPath: String? = null,
        voteAverage: Double = 7.5,
        voteCount: Int = 1000,
        releaseDate: String = "2024-01-01",
        popularity: Double = 50.0
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

    // ═══════════════════════════════════════════════════════
    // TESTS DE VALIDACIÓN
    // ═══════════════════════════════════════════════════════

    @Test
    fun `when query is empty, returns error without calling repository`() = runTest {
        // Given (Dado que...)
        val emptyQuery = ""

        // When (Cuando...)
        val result = useCase(emptyQuery).first()

        // Then (Entonces...)
        assertTrue("El resultado debe ser Error", result is Resource.Error)
        assertEquals(
            "El término de búsqueda no puede estar vacío",
            (result as Resource.Error).message
        )

        // Verificar que NO se llamó al repositorio
        coVerify(exactly = 0) { mockRepository.searchMovies(any()) }
    }

    @Test
    fun `when query is blank (spaces only), returns error without calling repository`() = runTest {
        // Given
        val blankQuery = "   "

        // When
        val result = useCase(blankQuery).first()

        // Then
        assertTrue("El resultado debe ser Error", result is Resource.Error)
        assertEquals(
            "El término de búsqueda no puede estar vacío",
            (result as Resource.Error).message
        )

        coVerify(exactly = 0) { mockRepository.searchMovies(any()) }
    }

    @Test
    fun `when query has only 1 character, returns error without calling repository`() = runTest {
        // Given
        val shortQuery = "a"

        // When
        val result = useCase(shortQuery).first()

        // Then
        assertTrue("El resultado debe ser Error", result is Resource.Error)
        assertEquals(
            "Ingresa al menos 2 caracteres",
            (result as Resource.Error).message
        )

        coVerify(exactly = 0) { mockRepository.searchMovies(any()) }
    }

    @Test
    fun `when query has exactly 2 characters, calls repository`() = runTest {
        // Given
        val validQuery = "ab"
        val expectedMovies = listOf(createTestMovie())

        // Configurar el mock: cuando se llame searchMovies, retornar esto
        coEvery { mockRepository.searchMovies(validQuery) } returns flowOf(
            Resource.Success(expectedMovies)
        )

        // When
        val result = useCase(validQuery).first()

        // Then
        assertTrue("El resultado debe ser Success", result is Resource.Success)

        // Verificar que SÍ se llamó al repositorio con el query correcto
        coVerify(exactly = 1) { mockRepository.searchMovies(validQuery) }
    }

    // ═══════════════════════════════════════════════════════
    // TESTS DE TRANSFORMACIÓN
    // ═══════════════════════════════════════════════════════

    @Test
    fun `when query has leading and trailing spaces, trims them before calling repository`() = runTest {
        // Given
        val queryWithSpaces = "  batman  "
        val trimmedQuery = "batman"
        val expectedMovies = listOf(
            createTestMovie(title = "Batman", voteAverage = 8.5, voteCount = 5000)
        )

        coEvery { mockRepository.searchMovies(trimmedQuery) } returns flowOf(
            Resource.Success(expectedMovies)
        )

        // When
        useCase(queryWithSpaces).first()

        // Then
        // Verificar que se llamó con el query TRIMMEADO
        coVerify(exactly = 1) { mockRepository.searchMovies(trimmedQuery) }

        // Verificar que NO se llamó con el query original
        coVerify(exactly = 0) { mockRepository.searchMovies(queryWithSpaces) }
    }

    // ═══════════════════════════════════════════════════════
    // TESTS DE DELEGACIÓN AL REPOSITORIO
    // ═══════════════════════════════════════════════════════

    @Test
    fun `when repository returns Success, use case propagates it`() = runTest {
        // Given
        val query = "avengers"
        val expectedMovies = listOf(
            Movie(
                id = 1,
                title = "Avengers",
                overview = "Earth's mightiest heroes",
                posterPath = "/path.jpg",
                backdropPath = "/backdrop.jpg",
                voteAverage = 8.0,
                voteCount = 10000,
                releaseDate = "2012-05-04",
                popularity = 100.0
            ),
            Movie(
                id = 2,
                title = "Avengers: Age of Ultron",
                overview = "When Tony Stark tries to jumpstart",
                posterPath = "/path2.jpg",
                backdropPath = "/backdrop2.jpg",
                voteAverage = 7.3,
                voteCount = 8000,
                releaseDate = "2015-05-01",
                popularity = 80.0
            )
        )

        coEvery { mockRepository.searchMovies(query) } returns flowOf(
            Resource.Success(expectedMovies)
        )

        // When
        val result = useCase(query).first()

        // Then
        assertTrue("El resultado debe ser Success", result is Resource.Success)
        assertEquals(
            expectedMovies,
            (result as Resource.Success).data
        )
        assertNotNull("Los datos no deben ser null", result.data)
        assertEquals("Debe retornar 2 películas", 2, result.data?.size)
    }

    @Test
    fun `when repository returns Error, use case propagates it`() = runTest {
        // Given
        val query = "unknown movie"
        val errorMessage = "No se encontraron películas"

        coEvery { mockRepository.searchMovies(query) } returns flowOf(
            Resource.Error(errorMessage)
        )

        // When
        val result = useCase(query).first()

        // Then
        assertTrue("El resultado debe ser Error", result is Resource.Error)
        assertEquals(
            errorMessage,
            (result as Resource.Error).message
        )
    }

    @Test
    fun `when repository returns Loading then Success, use case propagates both states`() = runTest {
        // Given
        val query = "spider-man"
        val expectedMovies = listOf(
            createTestMovie(
                title = "Spider-Man",
                overview = "With great power comes great responsibility",
                releaseDate = "2002-05-03",
                voteAverage = 7.3,
                voteCount = 3000
            )
        )

        // Simular que el repositorio emite Loading y luego Success
        coEvery { mockRepository.searchMovies(query) } returns flowOf(
            Resource.Loading(),
            Resource.Success(expectedMovies)
        )

        // When - Colectar TODOS los estados emitidos
        val results = useCase(query).toList()

        // Then
        assertEquals("Debe emitir 2 estados", 2, results.size)
        assertTrue("Primer estado debe ser Loading", results[0] is Resource.Loading)
        assertTrue("Segundo estado debe ser Success", results[1] is Resource.Success)
        assertEquals(
            expectedMovies,
            (results[1] as Resource.Success).data
        )
    }

    @Test
    fun `when repository returns empty list, use case propagates it as Success`() = runTest {
        // Given
        val query = "xyzabc123notfound"
        val emptyList = emptyList<Movie>()

        coEvery { mockRepository.searchMovies(query) } returns flowOf(
            Resource.Success(emptyList)
        )

        // When
        val result = useCase(query).first()

        // Then
        assertTrue("El resultado debe ser Success", result is Resource.Success)
        assertTrue("La lista debe estar vacía", (result as Resource.Success).data?.isEmpty() == true)
    }

    // ═══════════════════════════════════════════════════════
    // TESTS DE EDGE CASES
    // ═══════════════════════════════════════════════════════

    @Test
    fun `when query has special characters, calls repository normally`() = runTest {
        // Given
        val specialQuery = "avengers: endgame"
        val expectedMovies = listOf(
            createTestMovie(
                title = "Avengers: Endgame",
                overview = "After the devastating events",
                releaseDate = "2019-04-26",
                voteAverage = 8.4,
                voteCount = 15000,
                popularity = 150.0
            )
        )

        coEvery { mockRepository.searchMovies(specialQuery) } returns flowOf(
            Resource.Success(expectedMovies)
        )

        // When
        val result = useCase(specialQuery).first()

        // Then
        assertTrue("El resultado debe ser Success", result is Resource.Success)
        coVerify(exactly = 1) { mockRepository.searchMovies(specialQuery) }
    }

    @Test
    fun `when query has uppercase letters, calls repository with exact case`() = runTest {
        // Given
        val uppercaseQuery = "BATMAN"
        val expectedMovies = listOf(
            createTestMovie(
                title = "Batman",
                overview = "The Dark Knight rises",
                releaseDate = "1989-06-23",
                voteAverage = 7.6,
                voteCount = 4000
            )
        )

        coEvery { mockRepository.searchMovies(uppercaseQuery) } returns flowOf(
            Resource.Success(expectedMovies)
        )

        // When
        val result = useCase(uppercaseQuery).first()

        // Then
        assertTrue("El resultado debe ser Success", result is Resource.Success)

        // Verificar que NO se convirtió a lowercase
        coVerify(exactly = 1) { mockRepository.searchMovies(uppercaseQuery) }
        coVerify(exactly = 0) { mockRepository.searchMovies("batman") }
    }

    @Test
    fun `when query has numbers, calls repository normally`() = runTest {
        // Given
        val queryWithNumbers = "blade runner 2049"
        val expectedMovies = listOf(
            createTestMovie(
                title = "Blade Runner 2049",
                overview = "Thirty years after the events",
                releaseDate = "2017-10-06",
                voteAverage = 8.0,
                voteCount = 7000,
                popularity = 90.0
            )
        )

        coEvery { mockRepository.searchMovies(queryWithNumbers) } returns flowOf(
            Resource.Success(expectedMovies)
        )

        // When
        val result = useCase(queryWithNumbers).first()

        // Then
        assertTrue("El resultado debe ser Success", result is Resource.Success)
        coVerify(exactly = 1) { mockRepository.searchMovies(queryWithNumbers) }
    }
}