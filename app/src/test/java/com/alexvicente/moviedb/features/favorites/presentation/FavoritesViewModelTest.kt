package com.alexvicente.moviedb.features.favorites.presentation

import app.cash.turbine.test
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.favorites.domain.model.Favorite
import com.alexvicente.moviedb.features.favorites.domain.repository.FavoritesRepository
import com.alexvicente.moviedb.features.favorites.domain.usecase.AddFavoriteUseCase
import com.alexvicente.moviedb.features.favorites.domain.usecase.GetFavoritesUseCase
import com.alexvicente.moviedb.features.favorites.domain.usecase.IsFavoriteUseCase
import com.alexvicente.moviedb.features.favorites.domain.usecase.RemoveFavoriteUseCase
import com.alexvicente.moviedb.testutil.factories.MovieFactory
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

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

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

    private fun createViewModel() {
        viewModel = FavoritesViewModel(
            getFavoritesUseCase,
            addFavoriteUseCase,
            removeFavoriteUseCase,
            isFavoriteUseCase
        )
    }

    // ── Tests de uiState ─────────────────────────────────────────────────────

    @Test
    fun whenCreated_initialStateIsLoading() = runTest {
        every { mockRepository.getAllFavoritesRepo() } returns flow { awaitCancellation() }
        createViewModel()
        assertThat(viewModel.uiState.value).isInstanceOf(FavoritesUiState.Loading::class.java)
    }

    @Test
    fun whenFavoritesListIsEmpty_emitsEmptyState() = runTest {
        every { mockRepository.getAllFavoritesRepo() } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(FavoritesUiState.Empty::class.java)
    }

    @Test
    fun whenFavoritesExist_emitsSuccessWithData() = runTest {
        val favorites = listOf(
            createFavorite(id = 1, title = "Inception", voteAverage = 8.8),
            createFavorite(id = 2, title = "Interstellar")
        )
        every { mockRepository.getAllFavoritesRepo() } returns flowOf(favorites)
        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(FavoritesUiState.Success::class.java)
        assertThat((state as FavoritesUiState.Success).favorites).hasSize(2)
        assertThat(state.favorites.map { it.title })
            .containsExactly("Inception", "Interstellar").inOrder()
        assertThat(state.favorites.first().voteAverage).isEqualTo(8.8)
        assertThat(state.favorites.first().id).isEqualTo(1)
    }

    @Test
    fun whenRoomEmitsUpdate_uiStateUpdatesReactively() = runTest {
        val favorite = createFavorite(id = 1)
        every { mockRepository.getAllFavoritesRepo() } returns flow {
            emit(emptyList())
            emit(listOf(favorite))
        }
        createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val finalState = awaitItem()
            assertThat(finalState).isInstanceOf(FavoritesUiState.Success::class.java)
            assertThat((finalState as FavoritesUiState.Success).favorites).hasSize(1)
            assertThat(finalState.favorites.first().title).isEqualTo("Inception")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Tests de removeFavorite ───────────────────────────────────────────────

    @Test
    fun whenRemoveFavoriteCalled_delegatesToRepository() = runTest {
        every { mockRepository.getAllFavoritesRepo() } returns flowOf(emptyList())
        coEvery { mockRepository.removeFavoriteRepo(any()) } returns Resource.Success(Unit)
        createViewModel()
        advanceUntilIdle()

        viewModel.removeFavorite(movieId = 42)
        advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.removeFavoriteRepo(42) }
    }

    @Test
    fun whenRemoveFavoriteCalledMultipleTimes_eachCallDelegates() = runTest {
        every { mockRepository.getAllFavoritesRepo() } returns flowOf(emptyList())
        coEvery { mockRepository.removeFavoriteRepo(any()) } returns Resource.Success(Unit)
        createViewModel()
        advanceUntilIdle()

        viewModel.removeFavorite(1)
        viewModel.removeFavorite(2)
        viewModel.removeFavorite(3)
        advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.removeFavoriteRepo(1) }
        coVerify(exactly = 1) { mockRepository.removeFavoriteRepo(2) }
        coVerify(exactly = 1) { mockRepository.removeFavoriteRepo(3) }
    }

    // ── Tests de isFavoriteState ──────────────────────────────────────────────

    @Test
    fun whenObserveIsFavoriteCalled_updatesIsFavoriteState() = runTest {
        val movie = MovieFactory.createMovie(id = 99)
        every { mockRepository.getAllFavoritesRepo() } returns flowOf(emptyList())
        every { mockRepository.isFavoriteRepo(99) } returns flowOf(true)
        createViewModel()

        viewModel.observeIsFavorite(movie)
        advanceUntilIdle()

        assertThat(viewModel.isFavoriteState.value).isTrue()
    }

    @Test
    fun whenMovieIsNotFavorite_isFavoriteStateIsFalse() = runTest {
        val movie = MovieFactory.createMovie(id = 99)
        every { mockRepository.getAllFavoritesRepo() } returns flowOf(emptyList())
        every { mockRepository.isFavoriteRepo(99) } returns flowOf(false)
        createViewModel()

        viewModel.observeIsFavorite(movie)
        advanceUntilIdle()

        assertThat(viewModel.isFavoriteState.value).isFalse()
    }

    // ── Tests de toggleFavorite ───────────────────────────────────────────────

    @Test
    fun whenToggleFavoriteAndIsNotFavorite_addsFavorite() = runTest {
        val movie = MovieFactory.createMovie(id = 99)
        every { mockRepository.getAllFavoritesRepo() } returns flowOf(emptyList())
        every { mockRepository.isFavoriteRepo(99) } returns flowOf(false)
        coEvery { mockRepository.addFavoriteRepo(any()) } returns Resource.Success(Unit)
        createViewModel()

        viewModel.observeIsFavorite(movie)
        advanceUntilIdle()

        viewModel.toggleFavorite()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.addFavoriteRepo(movie) }
        coVerify(exactly = 0) { mockRepository.removeFavoriteRepo(any()) }
    }

    @Test
    fun whenToggleFavoriteAndIsFavorite_removesFavorite() = runTest {
        val movie = MovieFactory.createMovie(id = 99)
        every { mockRepository.getAllFavoritesRepo() } returns flowOf(emptyList())
        every { mockRepository.isFavoriteRepo(99) } returns flowOf(true)
        coEvery { mockRepository.removeFavoriteRepo(any()) } returns Resource.Success(Unit)
        createViewModel()

        viewModel.observeIsFavorite(movie)
        advanceUntilIdle()

        viewModel.toggleFavorite()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.removeFavoriteRepo(99) }
        coVerify(exactly = 0) { mockRepository.addFavoriteRepo(any()) }
    }

    @Test
    fun whenToggleFavoriteWithoutObserving_doesNothing() = runTest {
        every { mockRepository.getAllFavoritesRepo() } returns flowOf(emptyList())
        createViewModel()
        advanceUntilIdle()

        viewModel.toggleFavorite()
        advanceUntilIdle()

        coVerify(exactly = 0) { mockRepository.addFavoriteRepo(any()) }
        coVerify(exactly = 0) { mockRepository.removeFavoriteRepo(any()) }
    }

    @Test
    fun whenToggleFavoriteTwice_returnsToOriginalState() = runTest {
        val movie = MovieFactory.createMovie(id = 99)
        val isFavoriteFlow = MutableSharedFlow<Boolean>(replay = 1)

        every { mockRepository.getAllFavoritesRepo() } returns flowOf(emptyList())
        every { mockRepository.isFavoriteRepo(99) } returns isFavoriteFlow
        coEvery { mockRepository.addFavoriteRepo(any()) } coAnswers {
            isFavoriteFlow.emit(true)
            Resource.Success(Unit)      // ← retorno requerido
        }
        coEvery { mockRepository.removeFavoriteRepo(any()) } coAnswers {
            isFavoriteFlow.emit(false)
            Resource.Success(Unit)      // ← retorno requerido
        }

        createViewModel()
        isFavoriteFlow.emit(false)
        viewModel.observeIsFavorite(movie)
        advanceUntilIdle()
        assertThat(viewModel.isFavoriteState.value).isFalse()

        viewModel.toggleFavorite()
        advanceUntilIdle()
        coVerify(exactly = 1) { mockRepository.addFavoriteRepo(movie) }
        assertThat(viewModel.isFavoriteState.value).isTrue()

        viewModel.toggleFavorite()
        advanceUntilIdle()
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
        assertThat(viewModel.isFavoriteState.value).isFalse()
    }

    @Test
    fun whenRemoveFavoriteCalled_uiStateUpdatesToEmpty() = runTest {
        val favorite = createFavorite(id = 1)
        val favoritesFlow = MutableSharedFlow<List<Favorite>>(replay = 1)
        every { mockRepository.getAllFavoritesRepo() } returns favoritesFlow

        favoritesFlow.emit(listOf(favorite))
        createViewModel()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(FavoritesUiState.Success::class.java)

        coEvery { mockRepository.removeFavoriteRepo(1) } coAnswers {
            favoritesFlow.emit(emptyList())
            Resource.Success(Unit)      // ← retorno requerido
        }

        viewModel.removeFavorite(1)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(FavoritesUiState.Empty::class.java)
    }

    // ── Tests de errorEvent ───────────────────────────────────────────────────

    @Test
    fun whenRemoveFavoriteFails_emitsErrorEvent() = runTest {
        every { mockRepository.getAllFavoritesRepo() } returns flowOf(emptyList())
        coEvery { mockRepository.removeFavoriteRepo(any()) } returns
                Resource.Error("Error interno. Intenta de nuevo.")
        createViewModel()
        advanceUntilIdle()

        viewModel.errorEvent.test {
            viewModel.removeFavorite(1)
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo("Error interno. Intenta de nuevo.")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun whenAddFavoriteFails_emitsErrorEvent() = runTest {
        val movie = MovieFactory.createMovie(id = 99)
        every { mockRepository.getAllFavoritesRepo() } returns flowOf(emptyList())
        every { mockRepository.isFavoriteRepo(99) } returns flowOf(false)
        coEvery { mockRepository.addFavoriteRepo(any()) } returns
                Resource.Error("No se pudo guardar el favorito.")
        createViewModel()

        viewModel.observeIsFavorite(movie)
        advanceUntilIdle()

        viewModel.errorEvent.test {
            viewModel.toggleFavorite()
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo("No se pudo guardar el favorito.")
            cancelAndIgnoreRemainingEvents()
        }
    }
}