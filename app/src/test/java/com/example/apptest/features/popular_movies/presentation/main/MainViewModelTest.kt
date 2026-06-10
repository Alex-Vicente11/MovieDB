package com.example.apptest.features.popular_movies.presentation.main

import app.cash.turbine.test
import com.example.apptest.features.popular_movies.domain.usecase.GetPopularMoviesUseCase
import com.example.apptest.features.popular_movies.presentation.MainUiState
import com.example.apptest.features.search.domain.repository.SearchRepository
import com.example.apptest.features.search.domain.usecase.SearchMoviesUseCase
import com.example.apptest.testutil.factories.MovieFactory
import com.example.apptest.testutil.factories.ResourceFactory
import com.example.apptest.testutil.fakes.FakePopularMoviesRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Responsabilidad del ViewModel:
 *   - Traducir Resource<List<Movie>> -> MainUiState
 *   - Manejar lista vacía como MainUiState.Empty (solo en searchMovies)
 *   - Exponer el estado correcto en uiState: StateFlow
 *
 * Herramientas
 *  FakePopularMoviesRepository -> simula lógica offline-first para getPopularMovies
 *  MockK (SearchRepository)    -> simula respuestas simples para searchMovies
 *  StandardTestDispatcher      -> controla la ejecución de coroutines manualmente
 *  Turbine                     -> colecta y verifica emisiones del StateFlow
 *
 *  ¿Por qué StandardTestDispatcher en lugar de UnconfinedTestDispatcher?
 *  StandardTestDispatcher pausa las coroutines hasta que llamamos a advanceUntilIdle()
 *  Esto nos da control preciso sobre cuándo se ejecuta cada coroutine,
 *  UnconfinedTestDispatcher ejecuta todoo inmediatamente - más simple pero
 *  menos control sobre el orden de emisiones.
 */

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    // Dispatcher de test

    // StandardTestDispatcher: las coroutines no se ejecutan hasta advanceUntilIlde()
    // Reemplaza Dispatchers.Main durante los tests para que viewModelScope funcione
    private val testDispatcher = StandardTestDispatcher()

    // Dependencias

    // Fake para popular movies - modela offline-first con propiedades configurables
    private lateinit var fakePopularMoviesRepository: FakePopularMoviesRepository
    private lateinit var getPopularMoviesUseCase: GetPopularMoviesUseCase

    // Mock para search - comportamiento simple, configurado para test
    private lateinit var mockSearchRepository: SearchRepository
    private lateinit var searchMoviesUseCase: SearchMoviesUseCase

    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        // Reemplaza Dispatchers.Main por el dispatcher de test
        // Sin esto, viewModelScope.launch usaría el Main real y fallaría en JVM
        Dispatchers.setMain(testDispatcher)

        fakePopularMoviesRepository = FakePopularMoviesRepository()
        getPopularMoviesUseCase = GetPopularMoviesUseCase(fakePopularMoviesRepository)

        mockSearchRepository = mockk()
        searchMoviesUseCase = SearchMoviesUseCase(mockSearchRepository)

        viewModel = MainViewModel(searchMoviesUseCase, getPopularMoviesUseCase)
    }

    @After
    fun tearDown() {
        // Restaura Dispatchers.Main al estado original después de cada test
        Dispatchers.resetMain()
        fakePopularMoviesRepository.reset()
    }

    // ESTADO INICIAL

    @Test
    fun initialState_isIdle() = runTest {
        // El ViewModel recién creado debe estar en Idle antes de cualquier acción
        assertThat(viewModel.uiState.value).isInstanceOf(MainUiState.Idle::class.java)
    }

    // getPopularMovies - TRADUCCIÓN DE RESOURCE A UISTATE
    @Test
    fun whenGetPopularMoviesCalled_emitsLoadingThenSuccess() = runTest {
        // Given
        fakePopularMoviesRepository.movies = MovieFactory.createMovieList(5)

        // When / Then
        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(MainUiState.Idle::class.java)

            viewModel.getPopularMovies()

            // advanceUtilIdle() ejecuta todas las coroutines pendientes
            // hasta que no queda trabajo por hacer
            advanceUntilIdle()

            // Loading: traducción de Resource.Success -> MainState.Success
            assertThat(awaitItem()).isInstanceOf(MainUiState.Loading::class.java)

            // Success: traducción de Resource.Success -> MainState.Success
            val success = awaitItem()
            assertThat(success).isInstanceOf(MainUiState.Success::class.java)
            assertThat((success as MainUiState.Success).movies).hasSize(5)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun whenGetPopularMoviesReturnsError_emitsLoadingThenError() = runTest {
        // Given
        fakePopularMoviesRepository.shouldReturnError = true
        fakePopularMoviesRepository.errorMessage = "Sin conexión. Verifica tu internet."

        // When / Then
        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(MainUiState.Idle::class.java)

            viewModel.getPopularMovies()
            advanceUntilIdle()

            // Loading: traducción de Resource.Success -> MainState.Success
            assertThat(awaitItem()).isInstanceOf(MainUiState.Loading::class.java)

            val error = awaitItem()

            assertThat(error).isInstanceOf(MainUiState.Error::class.java)
            //Verifica que el mensaje llega intacto a la UI
            assertThat((error as MainUiState.Error).message)
                .isEqualTo("Sin conexión. Verifica tu internet.")

            cancelAndIgnoreRemainingEvents()
        }
    }

   @Test
   fun whenGetPopularMoviesReturnsOfflineFirstSequence_emitsThreeStates() = runTest {
       // Given - simula la secuencia completa: Loading -> cache -> red
       fakePopularMoviesRepository.shouldEmitCacheFirst = true
       fakePopularMoviesRepository.cachedMovies = MovieFactory.createMovieList(2)
       fakePopularMoviesRepository.movies = MovieFactory.createMovieList(5)

       // When / Then
       viewModel.uiState.test {
           assertThat(awaitItem()).isInstanceOf(MainUiState.Idle::class.java)

           viewModel.getPopularMovies()
           advanceUntilIdle()

           // 1. Loading
           assertThat(awaitItem()).isInstanceOf(MainUiState.Loading::class.java)

           // 2. Success con caché (2 movies)
           val cacheState = awaitItem()
           assertThat(cacheState).isInstanceOf(MainUiState.Success::class.java)
           assertThat((cacheState as MainUiState.Success).movies).hasSize(2)

           // 3. Success con datos frescos (5 movies)
           val freshState = awaitItem()
           assertThat(freshState).isInstanceOf(MainUiState.Success::class.java)
           assertThat((freshState as MainUiState.Success).movies).hasSize(5)

           cancelAndIgnoreRemainingEvents()
       }
   }

    // searchMovies - TRADUCCIÓN DE RESOURCE A UISTATE

    @Test
    fun whenSearchMoviesReturnsResults_emitsLoadingThenSuccess() = runTest {
        // Given
        val movies = MovieFactory.createMovieList(3)
        coEvery { mockSearchRepository.searchMovies(any()) } returns flowOf(
            ResourceFactory.loading(),
            ResourceFactory.successMovies(movies)
        )

        // When / Then
        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(MainUiState.Idle::class.java)

            viewModel.searchMovies("Spiderman")
            advanceUntilIdle()

            assertThat(awaitItem()).isInstanceOf(MainUiState.Loading::class.java)

            val success = awaitItem()
            assertThat(success).isInstanceOf(MainUiState.Success::class.java)
            assertThat((success as MainUiState.Success).movies).hasSize(3)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun whenSearchMoviesReturnsEmptyList_emitsEmpty() = runTest {
        // Given - lista vacía es el único caso donde ViewModel emite Empty
        // Esta lógica está en el ViewModel, no en el UseCase
        coEvery { mockSearchRepository.searchMovies(any()) } returns flowOf(
            ResourceFactory.emptyMovies()
        )

        // When / Then
        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(MainUiState.Idle::class.java)

            viewModel.searchMovies("xvsatnotfound")
            advanceUntilIdle()

            // El ViewModel convierte Success (emptyList) -> MainUiState.Empty
            assertThat(awaitItem()).isInstanceOf(MainUiState.Empty::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun whenSearchMoviesReturnsError_emitsError() = runTest {
        // Given
        coEvery { mockSearchRepository.searchMovies(any()) } returns flowOf(
            ResourceFactory.networkError()
        )

        // When / Then
        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(MainUiState.Idle::class.java)

            viewModel.searchMovies("batman")
            advanceUntilIdle()

            val error = awaitItem()
            assertThat(error).isInstanceOf(MainUiState.Error::class.java)
            assertThat((error as MainUiState.Error).message)
                .isEqualTo("Error de conexión. Verifica tu internet.")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun whenSearchQueryIsBlank_emitsErrorFromUseCase() = runTest {
        // Given - query vacío es interceptado por el UseCase antes de llegar al repositorio
        // No necesitamos configurar el mock porque el UseCase nunca lo llama

        // When / Then
        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(MainUiState.Idle::class.java)

            viewModel.searchMovies("")
            advanceUntilIdle()

            val error = awaitItem()
            assertThat(error).isInstanceOf(MainUiState.Error::class.java)
            assertThat((error as MainUiState.Error).message)
                .isEqualTo("El término de búsqueda no puede estar vacío")

            cancelAndIgnoreRemainingEvents()
        }
    }

    // TRANSICIONES ENTRE FUNCIONES

    @Test
    fun whenSearchCalledAfterPopularMovies_stateUpdatesCorrectly() = runTest {
        // Given
        fakePopularMoviesRepository.movies = MovieFactory.createMovieList(5)
        coEvery { mockSearchRepository.searchMovies("batman") } returns flowOf(
            ResourceFactory.successMovies(MovieFactory.createMovieList(2))
        )

        // Primera acción: popular movies
        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(MainUiState.Idle::class.java)

            viewModel.getPopularMovies()
            advanceUntilIdle()

            assertThat(awaitItem()).isInstanceOf(MainUiState.Loading::class.java)
            assertThat(awaitItem()).isInstanceOf(MainUiState.Success::class.java)

            cancelAndIgnoreRemainingEvents()
        }

        // Segunda acción: search
        // El mock de search solo emite Success — sin Loading intermedio
        // porque ResourceFactory.successMovies() no incluye Loading
        viewModel.uiState.test {
            // Estado actual del StateFlow: Success de popular movies
            assertThat(awaitItem()).isInstanceOf(MainUiState.Success::class.java)

            viewModel.searchMovies("batman")
            advanceUntilIdle()

            // Va directo a Success — el mock no emite Loading
            val searchResult = awaitItem()
            assertThat(searchResult).isInstanceOf(MainUiState.Success::class.java)
            assertThat((searchResult as MainUiState.Success).movies).hasSize(2)

            cancelAndIgnoreRemainingEvents()
        }
    }
}