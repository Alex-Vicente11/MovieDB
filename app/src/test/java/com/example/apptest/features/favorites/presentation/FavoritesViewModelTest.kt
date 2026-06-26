package com.example.apptest.features.favorites.presentation

import app.cash.turbine.test
import com.example.apptest.features.favorites.domain.model.Favorite
import com.example.apptest.features.favorites.domain.repository.FavoritesRepository
import com.example.apptest.features.favorites.domain.usecase.AddFavoriteUseCase
import com.example.apptest.features.favorites.domain.usecase.GetFavoritesUseCase
import com.example.apptest.features.favorites.domain.usecase.IsFavoriteUseCase
import com.example.apptest.features.favorites.domain.usecase.RemoveFavoriteUseCase
import com.example.apptest.testutil.factories.MovieFactory
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
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
 * Tests unitarios para FavoritesViewModel.
 *
 * FavoritesViewModel tiene DOS responsabilidades:
 *   1. Lista de favoritos (FavoritesFragment):
 *      uiState → Loading / Empty / Success(list)
 *   2. Estado de favorito individual (MovieDetailsFragment):
 *      isFavoriteState → Flow<Boolean>
 *      toggleFavorite() → add o remove según estado actual
 *
 * Testeamos ambas responsabilidades en este archivo porque
 * comparten el mismo ViewModel — dividirlos causaría confusión.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // ── Dependencias mockeadas ────────────────────────────────────────────────
    private lateinit var mockRepository: FavoritesRepository
    private lateinit var getFavoritesUseCase: GetFavoritesUseCase
    private lateinit var addFavoriteUseCase: AddFavoriteUseCase
    private lateinit var removeFavoriteUseCase: RemoveFavoriteUseCase
    private lateinit var isFavoriteUseCase: IsFavoriteUseCase
    private lateinit var viewModel: FavoritesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mockk(relaxed = true)
        getFavoritesUseCase = GetFavoritesUseCase(mockRepository)
        addFavoriteUseCase = AddFavoriteUseCase(mockRepository)
        removeFavoriteUseCase = RemoveFavoriteUseCase(mockRepository)
        isFavoriteUseCase = IsFavoriteUseCase(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Crea un Favorite de prueba con valores predecibles.
     * Separado de MovieFactory porque Favorite tiene campos propios
     * (addedAt, overview directo) distintos a Movie.
     */
    private fun createFavorite(
        id: Int = 1,
        title: String = "Inception",
        voteAverage: Double = 8.8
    ) = Favorite(
        id = id,
        title = title,
        posterPath = "/poster.jpg",
        voteAverage = voteAverage,
        releaseDate = "2010-07-16",
        overview = "A thief who steals corporate secrets",
        addedAt = System.currentTimeMillis()
    )

    /**
     * Crea el ViewModel con el estado de repositorio ya configurado.
     * Se llama al final de setUp de cada test — no en el @Before global
     * porque cada test necesita configurar el mock ANTES de init{}.
     */
    private fun createViewModel() {
        viewModel = FavoritesViewModel(
            getFavoritesUseCase,
            addFavoriteUseCase,
            removeFavoriteUseCase,
            isFavoriteUseCase
        )
    }

    // ── Tests de uiState — lista de favoritos ─────────────────────────────────

    @Test
    fun whenCreated_initialStateIsLoading() = runTest {
        // Given — Flow que nunca emite ni completa, simula Room "en vuelo"
        every { mockRepository.getAllFavoritesRepo() } returns flow {awaitCancellation()}

        // When - creamos el ViewModel pero NO llamamos advanceUntilIdle()
        // porque queremos capturar el estado ANTES de que cualquier emisión llegue
        createViewModel()

        // Then — estado inicial es Loading antes de que Room emita
        assertThat(viewModel.uiState.value)
            .isInstanceOf(FavoritesUiState.Loading::class.java)
    }

    @Test
    fun whenFavoritesListIsEmpty_emitsEmptyState() = runTest {
        // Given — Room emite lista vacía
        every { mockRepository.getAllFavoritesRepo() } returns flowOf(emptyList())

        // When
        createViewModel()
        advanceUntilIdle()

        // Then — Empty es el estado correcto cuando no hay favoritos
        assertThat(viewModel.uiState.value)
            .isInstanceOf(FavoritesUiState.Empty::class.java)
    }

    @Test
    fun whenFavoritesExist_emitsSuccessWithData() = runTest {
        // Given
        val favorites = listOf(
            createFavorite(id = 1, title = "Inception", voteAverage = 8.8),
            createFavorite(id = 2, title = "Interstellar")
        )
        every { mockRepository.getAllFavoritesRepo() } returns flowOf(favorites)

        // When
        createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(FavoritesUiState.Success::class.java)
        assertThat((state as FavoritesUiState.Success).favorites).hasSize(2)
        assertThat(state.favorites.map { it.title })
            .containsExactly("Inception", "Interstellar")
            .inOrder()

        assertThat(state.favorites.first().voteAverage).isEqualTo(8.8)
        assertThat(state.favorites.first().id).isEqualTo(1)
    }

    @Test
    fun whenRoomEmitsUpdate_uiStateUpdatesReactively() = runTest {
        // Given — Room emite primero lista vacía, luego lista con datos
        // Simula el flujo real: el usuario agrega un favorito desde otra pantalla
        val favorite = createFavorite(id = 1)
        every { mockRepository.getAllFavoritesRepo() } returns flow {
            emit(emptyList())         // primera emisión: sin favoritos
            emit(listOf(favorite))     // segunda emisión: favorito agregado
        }

        // When
        createViewModel()
        advanceUntilIdle()

        // Then — Turbine captura el estado final después de todas las emisiones
        viewModel.uiState.test {
            // El StateFlow replay=1 entrega el último valor emitido al suscribirse
            val finalState = awaitItem()
            assertThat(finalState).isInstanceOf(FavoritesUiState.Success::class.java)
            assertThat((finalState as FavoritesUiState.Success).favorites).hasSize(1)
            assertThat(finalState.favorites.first().title).isEqualTo("Inception")

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Tests de removeFavorite ────────────────────────────────────────────────

    @Test
    fun whenRemoveFavoriteCalled_delegatesToRepository() = runTest {
        // Given
        every { mockRepository.getAllFavoritesRepo() } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // When
        viewModel.removeFavorite(movieId = 42)
        advanceUntilIdle()

        // Then — removeFavoriteRepo fue llamado con el ID correcto
        coVerify(exactly = 1) { mockRepository.removeFavoriteRepo(42) }
    }

    @Test
    fun whenRemoveFavoriteCalledMultipleTimes_eachCallDelegates() = runTest {
        // Given
        every { mockRepository.getAllFavoritesRepo() } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // When
        viewModel.removeFavorite(1)
        viewModel.removeFavorite(2)
        viewModel.removeFavorite(3)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { mockRepository.removeFavoriteRepo(1) }
        coVerify(exactly = 1) { mockRepository.removeFavoriteRepo(2) }
        coVerify(exactly = 1) { mockRepository.removeFavoriteRepo(3) }
    }

    // ── Tests de isFavoriteState ──────────────────────────────────────────────

    @Test
    fun whenObserveIsFavoriteCalled_updatesIsFavoriteState() = runTest {
        // Given
        val movie = MovieFactory.createMovie(id = 99)
        every { mockRepository.getAllFavoritesRepo() } returns flowOf(emptyList())
        every { mockRepository.isFavoriteRepo(99) } returns flowOf(true)
        createViewModel()

        // When
        viewModel.observeIsFavorite(movie)
        advanceUntilIdle()

        // Then — isFavoriteState refleja el valor de Room
        assertThat(viewModel.isFavoriteState.value).isTrue()
    }

    @Test
    fun whenMovieIsNotFavorite_isFavoriteStateIsFalse() = runTest {
        // Given
        val movie = MovieFactory.createMovie(id = 99)
        every { mockRepository.getAllFavoritesRepo() } returns flowOf(emptyList())
        every { mockRepository.isFavoriteRepo(99) } returns flowOf(false)
        createViewModel()

        // When
        viewModel.observeIsFavorite(movie)
        advanceUntilIdle()

        // Then
        assertThat(viewModel.isFavoriteState.value).isFalse()
    }

    // ── Tests de toggleFavorite ───────────────────────────────────────────────

    @Test
    fun whenToggleFavoriteAndIsNotFavorite_addsFavorite() = runTest {
        // Given — película NO es favorita actualmente
        val movie = MovieFactory.createMovie(id = 99)
        every { mockRepository.getAllFavoritesRepo() } returns flowOf(emptyList())
        every { mockRepository.isFavoriteRepo(99) } returns flowOf(false)
        createViewModel()

        viewModel.observeIsFavorite(movie)
        advanceUntilIdle()

        // When — toggle con estado false → debe AGREGAR
        viewModel.toggleFavorite()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { mockRepository.addFavoriteRepo(movie) }
        coVerify(exactly = 0) { mockRepository.removeFavoriteRepo(any()) }
    }

    @Test
    fun whenToggleFavoriteAndIsFavorite_removesFavorite() = runTest {
        // Given — película SÍ es favorita actualmente
        val movie = MovieFactory.createMovie(id = 99)
        every { mockRepository.getAllFavoritesRepo() } returns flowOf(emptyList())
        every { mockRepository.isFavoriteRepo(99) } returns flowOf(true)
        createViewModel()

        viewModel.observeIsFavorite(movie)
        advanceUntilIdle()

        // When — toggle con estado true → debe REMOVER
        viewModel.toggleFavorite()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { mockRepository.removeFavoriteRepo(99) }
        coVerify(exactly = 0) { mockRepository.addFavoriteRepo(any()) }
    }

    @Test
    fun whenToggleFavoriteWithoutObserving_doesNothing() = runTest {
        // Given — currentMovie es null (observeIsFavorite nunca fue llamado)
        every { mockRepository.getAllFavoritesRepo() } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        // When — toggle sin haber observado ninguna película
        viewModel.toggleFavorite()
        advanceUntilIdle()

        // Then — no debe llamar ni add ni remove
        coVerify(exactly = 0) { mockRepository.addFavoriteRepo(any()) }
        coVerify(exactly = 0) { mockRepository.removeFavoriteRepo(any()) }
    }

    @Test
    fun whenToggleFavoriteTwice_returnsToOriginalState() = runTest {
        val movie = MovieFactory.createMovie(id = 99)

        // SharedFlow que podemos controlar manualmente — simula Room emitiendo
        // un nuevo valor cada vez que la base de datos cambia
        val isFavoriteFlow = MutableSharedFlow<Boolean>(replay = 1)

        every { mockRepository.getAllFavoritesRepo() } returns flowOf(emptyList())
        every { mockRepository.isFavoriteRepo(99) } returns isFavoriteFlow

        // Simular que add/remove actualizan el Flow reactivamente
        coEvery { mockRepository.addFavoriteRepo(any()) } coAnswers {
            isFavoriteFlow.emit(true)
        }
        coEvery { mockRepository.removeFavoriteRepo(any()) } coAnswers {
            isFavoriteFlow.emit(false)
        }

        createViewModel()

        // Estado inicial: NO es favorita
        isFavoriteFlow.emit(false)
        viewModel.observeIsFavorite(movie)
        advanceUntilIdle()
        assertThat(viewModel.isFavoriteState.value).isFalse()

        // Primer toggle — agrega
        viewModel.toggleFavorite()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.addFavoriteRepo(movie) }
        assertThat(viewModel.isFavoriteState.value).isTrue()

        // Segundo toggle — quita
        viewModel.toggleFavorite()
        advanceUntilIdle()

        // Then — remove fue llamado, add sigue siendo exactamente 1
        coVerify(exactly = 1) { mockRepository.removeFavoriteRepo(99) }
        coVerify(exactly = 1) { mockRepository.addFavoriteRepo(movie) }
        assertThat(viewModel.isFavoriteState.value).isFalse()
    }

    @Test
    fun whenObserveIsFavoriteCalledTwice_isFavoriteStateReflectsLastMovie() = runTest {
        val movie1 = MovieFactory.createMovie(id = 1)
        val movie2 = MovieFactory.createMovie(id = 2)

        every { mockRepository.getAllFavoritesRepo() } returns flowOf(emptyList())
        every { mockRepository.isFavoriteRepo(1) } returns flowOf(true)
        every { mockRepository.isFavoriteRepo(2) } returns flowOf(false)

        createViewModel()

        viewModel.observeIsFavorite(movie1)
        advanceUntilIdle()
        assertThat(viewModel.isFavoriteState.value).isTrue()

        viewModel.observeIsFavorite(movie2)
        advanceUntilIdle()

        // Then — debe reflejar movie2, no movie1
        assertThat(viewModel.isFavoriteState.value).isFalse()
        // currentMovie debe ser movie2
        // (si toggleFavorite() se llama ahora, debe actuar sobre movie2)
    }

    @Test
    fun whenRemoveFavoriteCalled_uiStateUpdatesToEmpty() = runTest {
        val favorite = createFavorite(id = 1)

        // Flow controlado manualmente — emitimos cada valor en el momento exacto
        val favoritesFlow = MutableSharedFlow<List<Favorite>>(replay = 1)
        every { mockRepository.getAllFavoritesRepo() } returns favoritesFlow

        // Estado inicial: lista con 1 favorito
        favoritesFlow.emit(listOf(favorite))
        createViewModel()
        advanceUntilIdle()

        // Verificar estado inicial — Success con 1 favorito
        assertThat(viewModel.uiState.value)
            .isInstanceOf(FavoritesUiState.Success::class.java)

        // Simular que Room re-emite lista vacía tras el delete
        coEvery { mockRepository.removeFavoriteRepo(1) } coAnswers {
            favoritesFlow.emit(emptyList())
        }

        // When
        viewModel.removeFavorite(1)
        advanceUntilIdle()

        // Then — Room re-emitió lista vacía → Empty
        assertThat(viewModel.uiState.value)
            .isInstanceOf(FavoritesUiState.Empty::class.java)
    }
}