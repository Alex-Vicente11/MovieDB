package com.alexvicente.moviedb.features.popular_movies.domain.usecase

import app.cash.turbine.test
import com.alexvicente.moviedb.features.popular_movies.domain.repository.PopularMoviesRepository
import com.alexvicente.moviedb.testutil.extensions.awaitError
import com.alexvicente.moviedb.testutil.extensions.awaitLoading
import com.alexvicente.moviedb.testutil.extensions.awaitSuccess
import com.alexvicente.moviedb.testutil.factories.MovieFactory
import com.alexvicente.moviedb.testutil.factories.ResourceFactory
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Tests unitarios para GetPopularMoviesUseCase
 *
 * Diferencia clave vs SearchMoviesUseCase:
 *   GetPopularMoviesUseCase NO tiene validaciones propias - solo delega.
 *   Por eso los tests se enfocan en verificar que la delegación es correcta
 *   y que todos los estados del repositorio se propagan sin modificación.
 *
 * Herramientas usadas:
 *   Mockk     -> simula PopularMoviesRepository
 *   Turbine   -> colecta y verifica emisiones del Flow
 *   FlowTestExtensions  -> awaitLoading/awaitSuccess/awaitError (helpers propios)
 *   MovieFactory  -> crea datos de prueba desde un solo lugar
 *   ResourceFactory -> estandariza los estados Resource<T>
 */

class GetPopularMoviesUseCaseTest {

    // Implementación falsa de la interfaz - sin red, sin BD
    private lateinit var mockRepository: PopularMoviesRepository
    private lateinit var useCase: GetPopularMoviesUseCase

    @Before
    fun setUp() {
        mockRepository = mockk()
        useCase = GetPopularMoviesUseCase(mockRepository)
    }

    // Delegación al repositorio
    @Test
    fun whenInvoked_delegatesToRepository() = runTest {
        // Given
        coEvery { mockRepository.getPopularMovies() } returns flowOf(
            ResourceFactory.successMovies(MovieFactory.createMovieList(3))
        )

        // When
        useCase().test { cancelAndIgnoreRemainingEvents() }

        //Then - el UseCase solo tiene una responsabilidad: llamar al repositorio
        coVerify(exactly = 1) { mockRepository.getPopularMovies() }
    }

    @Test
    fun whenInvokedMultipleTimes_callsRepositoryEachTime() = runTest {
        // Given
        coEvery { mockRepository.getPopularMovies() } returns flowOf(
            ResourceFactory.successMovies()
        )

        //When
        repeat(3) {
            useCase().test { cancelAndIgnoreRemainingEvents() }
        }
        //Then
        coVerify(exactly = 3) { mockRepository.getPopularMovies() }
    }

    // Propagación de estados

    @Test
    fun whenRepositoryEmitsSuccess_propagatesDataUnmodified() = runTest {
        // Given - lista con datos específicos para verificar que no se transforman
        val expectedMovies = MovieFactory.createMovieList(5)
        coEvery { mockRepository.getPopularMovies() } returns flowOf(
            ResourceFactory.successMovies(expectedMovies)
        )

        // When / Then
        useCase().test {
            // El UseCase no debe modificar, filtrar ni transformar los datos
            awaitSuccess { movies ->
                assertThat(movies).isEqualTo(expectedMovies)
                assertThat(movies).hasSize(5)
            }
            awaitComplete()
        }
    }

    @Test
    fun whenRepositoryEmitsError_propagatesMessageUnmodified() = runTest {
        // Given
        coEvery { mockRepository.getPopularMovies() } returns flowOf(
            ResourceFactory.networkError()
        )

        // When / Then
        useCase().test {
            // El UseCase no debe devolver ni modificar el mensaje de error
            awaitError { message ->
                assertThat(message).isEqualTo("Error de conexión. Verifica tu internet.")
            }
            awaitComplete()
        }
    }

    @Test
    fun whenRepositoryEmitsLoading_propagatesLoadingState() = runTest {
        // Given
        coEvery { mockRepository.getPopularMovies() } returns flowOf(
            ResourceFactory.loading()
        )

        // When / Then
        useCase().test {
            // Loading debe llegar a la UI sin modificación
            awaitLoading()
            awaitComplete()
        }
    }

    @Test
    fun whenRepositoryEmitsLoadingThenSuccess_propagatesBothStates() = runTest {
        // Given - secuencia real que emite el repositorio offline-first
        val movies = MovieFactory.createMovieList(3)
        coEvery { mockRepository.getPopularMovies() } returns flowOf(
            ResourceFactory.loading(),
            ResourceFactory.successMovies(movies)
        )

        // When / Then - los helpers consumen las emisiones en orden
        useCase().test {
            awaitLoading()
            awaitSuccess { assertThat(it).hasSize(3) }
            awaitComplete()
        }
    }

    @Test
    fun whenRepositoryEmitsLoadingThenError_propagatesBothStates() = runTest {
        // Given
        coEvery { mockRepository.getPopularMovies() } returns flowOf(
            ResourceFactory.loading(),
            ResourceFactory.networkError()
        )

        // When / Then
        useCase().test {
            awaitLoading()
            awaitError { message ->
                assertThat(message).contains("conexión")
            }
            awaitComplete()
        }
    }

    // Edge cases

    @Test
    fun whenRepositoryReturnsEmptyList_propagatesEmptySuccess() = runTest {
        // Given - lista vacía es un estado válido, no un error
        coEvery { mockRepository.getPopularMovies() } returns flowOf(
            ResourceFactory.emptyMovies()
        )

        // When / Then
        useCase().test {
            awaitSuccess { movies ->
                assertThat(movies).isEmpty()
            }
            awaitComplete()
        }
    }

    @Test
    fun whenRepositoryEmitsOfflineFirstSequence_propagatesAllThreeStates() = runTest {
        // Given - secuencia completa offline-first:
        // Loading -> caché inmediato -> datos frescos de red
        val cachedMovies = MovieFactory.createMovieList(2)
        val freshMovies = MovieFactory.createMovieList(5)

        coEvery { mockRepository.getPopularMovies() } returns flowOf(
            ResourceFactory.loading(),
            ResourceFactory.successMovies(cachedMovies), // primer Success: caché
            ResourceFactory.successMovies(freshMovies) // segundo Success: red
        )

        // When / Then
        useCase().test {
            awaitLoading()
            // Primera emisión de Success: datos del caché (menos películas)
            awaitSuccess { movies ->
                assertThat(movies).hasSize(2)
            }
            // Segunda emisión de Success: datos frescos (más películas)
            awaitSuccess { movies ->
                assertThat(movies).hasSize(5)
            }

            awaitComplete()
        }
    }
}