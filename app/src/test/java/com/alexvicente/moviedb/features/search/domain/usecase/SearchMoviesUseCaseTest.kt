package com.alexvicente.moviedb.features.search.domain.usecase

import app.cash.turbine.test
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.features.search.domain.repository.SearchRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
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
    private lateinit var mockRepository: SearchRepository

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
    fun whenQueryIsEmpty_emitsErrorAndSkipsRepository() = runTest {
        // Given
        val emptyQuery = ""

        // When/Then - Turbine colecta el Flow y expone cada emisión individualmente
        useCase(emptyQuery).test {
            val item = awaitItem()
            assertThat(item).isInstanceOf(Resource.Error::class.java)
            assertThat((item as Resource.Error).message)
                .isEqualTo("El término de búsqueda no puede estar vacío")
            awaitComplete() // verifica que el Flow cerró limpiamente
        }
        // Confirma que la validación no se llamó al repositorio
        coVerify ( exactly = 0) { mockRepository.searchMovies(any()) }
    }

    @Test
    fun whenQueryIsBlankSpaces_emitsErrorAndSkipsRepository() = runTest {
        // Given - isBlank() también atrapa strings de solo espacios
        val blankQuery = "   "

        // When/Then
        useCase(blankQuery).test {
            val item = awaitItem()
            assertThat(item).isInstanceOf(Resource.Error::class.java)
            assertThat((item as Resource.Error).message)
                .isEqualTo("El término de búsqueda no puede estar vacío")
            awaitComplete()
        }

        coVerify (exactly = 0) { mockRepository.searchMovies(any())}
    }

    @Test
    fun whenQueryIsSingleChar_emitsErrorAndSkipsRepository() = runTest {
        // given
        val shortQuery = "a"

        // when/then
        useCase(shortQuery).test {
            val item = awaitItem()
            assertThat(item).isInstanceOf(Resource.Error::class.java)
            assertThat((item as Resource.Error).message)
                .isEqualTo("Ingresa al menos 2 caracteres")
            awaitComplete()
        }

        coVerify(exactly = 0) {mockRepository.searchMovies(any()) }
    }

    @Test
    fun whenQueryHasExactlyTwoChars_callsRepository() = runTest {
        // Given - 2 caracteres es el minimo válido según la regla de negocio
        val validQuery = "ab"
        coEvery { mockRepository.searchMovies(validQuery) } returns
                flowOf(Resource.Success(listOf(createTestMovie())))

        // When/Then
        useCase(validQuery).test {
            assertThat(awaitItem()).isInstanceOf(Resource.Success::class.java)
            awaitComplete()
        }

        // Confirma que si delegó al repositorio con el query exacto
        coVerify (exactly = 1) { mockRepository.searchMovies(validQuery) }

    }

    // TESTS DE TRANSFORMACIÓN DEL QUERY


    @Test
    fun whenQueryHasWhitespace_trimsBeforeDelegating() = runTest {
        // Given
        val queryWithSpaces = "  batman  "
        val trimmedQuery = "batman"
        coEvery { mockRepository.searchMovies(trimmedQuery) } returns
                flowOf(Resource.Success(listOf(createTestMovie(title = "Batman"))))

        // When/Then
        useCase(queryWithSpaces).test {
            awaitItem()
            awaitComplete()
        }

        // El repositorio debe recibir el query sin espacios
        coVerify (exactly = 1) { mockRepository.searchMovies(trimmedQuery) }
        // Y nunca el query original con espacios
        coVerify (exactly = 0) { mockRepository.searchMovies(queryWithSpaces) }
    }

    @Test
    fun whenQueryHasUppercase_doesNotNormalizeCasing() = runTest {
        // Given - el UseCase no debe alterar mayúsculas, eso es responsabilidad de la API
        val uppercaseQuery = "BATMAN"
        coEvery { mockRepository.searchMovies(uppercaseQuery) } returns
                flowOf(Resource.Success(listOf(createTestMovie(title = "Batman"))))

        // When/Then
        useCase(uppercaseQuery).test {
            assertThat(awaitItem()).isInstanceOf(Resource.Success::class.java)
            awaitComplete()
        }

        coVerify (exactly = 1) { mockRepository.searchMovies(uppercaseQuery) }
        coVerify (exactly = 0) { mockRepository.searchMovies("batman") }
    }

    // Delegación y propagación de estados

    @Test
    fun whenRepositoryEmitsSuccess_useCasePropagatesData() = runTest {
        // Given
        val query = "avengers"
        val expectedMovies = listOf(
            createTestMovie(id = 1, title = "Avengers", voteAverage = 8.0),
            createTestMovie(id = 2, title = "Avengers: Age of Ultron", voteAverage = 7.3)
        )
        coEvery { mockRepository.searchMovies(query) } returns
                flowOf(Resource.Success(expectedMovies))

        // When/Then
        useCase(query).test {
            val item = awaitItem()
            assertThat(item).isInstanceOf(Resource.Success::class.java)
            // Verifica que los datos llegan intactos sin transformación
            assertThat((item as Resource.Success).data).isEqualTo(expectedMovies)
            assertThat(item.data).hasSize(2)
            awaitComplete()
        }
    }

    @Test
    fun whenRepositoryEmitsError_useCasePropagatesMessage() = runTest {
        // Given
        val query = "unknown movie"
        val errorMessage = "No se encontraron películas"
        coEvery { mockRepository.searchMovies(query) } returns
                flowOf(Resource.Error(errorMessage))

        // When/Then
        useCase(query).test {
            val item = awaitItem()
            assertThat(item).isInstanceOf(Resource.Error::class.java)
            // El UseCase no debe modificar el mensaje de error del repositorio
            assertThat((item as Resource.Error).message).isEqualTo(errorMessage)
            awaitComplete()
        }
    }

    @Test
    fun whenRepositoryEmitsLoadingThenSuccess_useCasePropagatesBothStates() = runTest {
        // Given
        val query = "spider-man"
        val expectedMovies = listOf(createTestMovie(title = "Spider-Man"))
        coEvery { mockRepository.searchMovies(query) } returns
                flowOf(
                    Resource.Loading(),
                    Resource.Success(expectedMovies)
                )
        // toList() es preferible aquí sobre Turbine porque necesitamos
        // verificar el orden y cantidad exacta de múltiples emisiones
        val results = useCase(query).toList()

        // Then
        assertThat(results).hasSize(2)
        assertThat(results[0]).isInstanceOf(Resource.Loading::class.java)
        assertThat(results[1]).isInstanceOf(Resource.Success::class.java)
        assertThat((results[1] as Resource.Success).data).isEqualTo(expectedMovies)
    }

    @Test
    fun whenRepositoryReturnsEmptyList_emitsSuccessWithEmptyData() = runTest {
        // Given - lista vacía es un resultado válido, no un error
        val query = "vecsr432notfound"
        coEvery { mockRepository.searchMovies(query) } returns
                flowOf(Resource.Success(emptyList()))

        // When/Then
        useCase(query).test {
            val item = awaitItem()
            assertThat(item).isInstanceOf(Resource.Success::class.java)
            assertThat((item as Resource.Success).data).isEmpty()
            awaitComplete()
        }
    }

    // TESTS DE EDGE CASES

    @Test
    fun whenQueryHasSpecialCharacters_callsRepositoryNormally() = runTest {
        // Given caracteres especiales como ":" son válidos en títulos de películas
        val specialQuery = "avengers: endgame"
        coEvery { mockRepository.searchMovies(specialQuery) } returns
                flowOf(Resource.Success(listOf(createTestMovie(title = "Avengers: Endgame"))))
        // when/then
        useCase(specialQuery).test {
            assertThat(awaitItem()).isInstanceOf(Resource.Success::class.java)
            awaitComplete()
        }

        coVerify (exactly = 1) { mockRepository.searchMovies(specialQuery) }
    }

    @Test
    fun whenQueryHasNumbers_callsRepositoryNormally() = runTest {
        // Given
        val queryWithNumbers = "blade runner 2049"
        coEvery { mockRepository.searchMovies(queryWithNumbers) } returns
                flowOf(Resource.Success(listOf(createTestMovie(title = "Blade Runner 2049"))))

        // When / Then
        useCase(queryWithNumbers).test {
            assertThat(awaitItem()).isInstanceOf(Resource.Success::class.java)
            awaitComplete()
        }

        coVerify(exactly = 1) { mockRepository.searchMovies(queryWithNumbers) }
    }
}