package com.alexvicente.moviedb.features.tv_series.presentation

import androidx.lifecycle.SavedStateHandle
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.tv_series.domain.usecase.GetTvSeriesCastUseCase
import com.alexvicente.moviedb.features.tv_series.domain.usecase.GetTvSeriesDetailsUseCase
import com.alexvicente.moviedb.testutil.factories.TvSeriesFactory
import com.alexvicente.moviedb.testutil.fakes.FakeTvSeriesCastRepository
import com.alexvicente.moviedb.testutil.fakes.FakeTvSeriesDetailsRepository
import com.alexvicente.moviedb.testutil.rules.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TvSeriesDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeTvSeriesDetailsRepository
    private lateinit var fakeCastRepository: FakeTvSeriesCastRepository

    @Before
    fun setUp() {
        fakeRepository = FakeTvSeriesDetailsRepository()
        fakeCastRepository = FakeTvSeriesCastRepository()
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(
            mapOf(TvSeriesDetailsViewModel.ARG_SERIES_ID to 1416)
        )
    ) = TvSeriesDetailsViewModel(
        savedStateHandle = savedStateHandle,
        getTvSeriesDetailsUseCase = GetTvSeriesDetailsUseCase(fakeRepository),
        getTvSeriesCastUseCase = GetTvSeriesCastUseCase(fakeCastRepository)
    )

    private fun handleWithId(seriesId: Int) = SavedStateHandle(
        mapOf(TvSeriesDetailsViewModel.ARG_SERIES_ID to seriesId)
    )

    // ── Detalle ───────────────────────────────────────────────────────────────

    @Test
    fun initialState_isLoading() = runTest {
        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value).isInstanceOf(TvSeriesDetailsUiState.Loading::class.java)
        assertThat(viewModel.castState.value).isInstanceOf(CastUiState.Loading::class.java)
    }

    @Test
    fun whenRepositorySucceeds_emitsSuccessWithDetails() = runTest {
        fakeRepository.result = Resource.Success(
            TvSeriesFactory.createTvSeriesDetails(id = 1416, name = "Anatomía según Grey")
        )
        val viewModel = createViewModel()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TvSeriesDetailsUiState.Success::class.java)
        assertThat((state as TvSeriesDetailsUiState.Success).details.name)
            .isEqualTo("Anatomía según Grey")
    }

    @Test
    fun usesTheSeriesIdFromSavedStateHandle() = runTest {
        createViewModel(handleWithId(87108))

        advanceUntilIdle()

        assertThat(fakeRepository.requestedIds).containsExactly(87108)
        assertThat(fakeCastRepository.requestedIds).containsExactly(87108)
    }

    @Test
    fun whenArgumentIsMissing_usesInvalidIdSentinel() = runTest {
        // -1 coincide con el android:defaultValue del <argument> en nav_graph.xml
        createViewModel(SavedStateHandle())

        advanceUntilIdle()

        assertThat(fakeRepository.requestedIds).containsExactly(-1)
    }

    @Test
    fun whenRepositoryFails_emitsErrorWithMessage() = runTest {
        fakeRepository.result = Resource.Error("boom")
        val viewModel = createViewModel()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TvSeriesDetailsUiState.Error::class.java)
        assertThat((state as TvSeriesDetailsUiState.Error).message).isEqualTo("boom")
    }

    @Test
    fun whenRetryAfterError_loadsAgainAndEmitsSuccess() = runTest {
        fakeRepository.result = Resource.Error("boom")
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(TvSeriesDetailsUiState.Error::class.java)

        fakeRepository.result = Resource.Success(TvSeriesFactory.createTvSeriesDetails())
        viewModel.retry()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(TvSeriesDetailsUiState.Success::class.java)
        assertThat(fakeRepository.requestedIds).hasSize(2)
    }

    // ── Reparto (estado independiente del detalle) ────────────────────────────

    @Test
    fun whenCastLoads_isLimitedToMaxMembers() = runTest {
        fakeCastRepository.result = Resource.Success(TvSeriesFactory.createCastMemberList(20))
        val viewModel = createViewModel()

        advanceUntilIdle()

        val state = viewModel.castState.value
        assertThat(state).isInstanceOf(CastUiState.Success::class.java)
        assertThat((state as CastUiState.Success).members)
            .hasSize(TvSeriesDetailsViewModel.MAX_CAST_MEMBERS)
    }

    @Test
    fun whenCastFails_detailsAreStillShown() = runTest {
        fakeCastRepository.result = Resource.Error("cast boom")
        val viewModel = createViewModel()

        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(TvSeriesDetailsUiState.Success::class.java)
        assertThat(viewModel.castState.value).isInstanceOf(CastUiState.Error::class.java)
    }

    @Test
    fun whenDetailsFail_castStillLoads() = runTest {
        fakeRepository.result = Resource.Error("details boom")
        val viewModel = createViewModel()

        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(TvSeriesDetailsUiState.Error::class.java)
        assertThat(viewModel.castState.value).isInstanceOf(CastUiState.Success::class.java)
    }

    @Test
    fun whenRetryCast_onlyCastIsRequestedAgain() = runTest {
        fakeCastRepository.result = Resource.Error("boom")
        val viewModel = createViewModel()
        advanceUntilIdle()

        fakeCastRepository.result = Resource.Success(TvSeriesFactory.createCastMemberList(3))
        viewModel.retryCast()
        advanceUntilIdle()

        assertThat(viewModel.castState.value).isInstanceOf(CastUiState.Success::class.java)
        assertThat(fakeCastRepository.requestedIds).hasSize(2)
        assertThat(fakeRepository.requestedIds).hasSize(1)
    }
}
