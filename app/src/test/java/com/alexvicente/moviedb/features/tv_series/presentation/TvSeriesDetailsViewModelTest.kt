package com.alexvicente.moviedb.features.tv_series.presentation

import androidx.lifecycle.SavedStateHandle
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.tv_series.domain.usecase.GetTvSeriesDetailsUseCase
import com.alexvicente.moviedb.testutil.factories.TvSeriesFactory
import com.alexvicente.moviedb.testutil.fakes.FakeTvSeriesDetailsRepository
import com.alexvicente.moviedb.testutil.rules.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TvSeriesDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeTvSeriesDetailsRepository

    @Before
    fun setUp() {
        fakeRepository = FakeTvSeriesDetailsRepository()
    }

    private fun createViewModel(seriesId: Int = 1416) = TvSeriesDetailsViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(TvSeriesDetailsViewModel.ARG_SERIES_ID to seriesId)
        ),
        getTvSeriesDetailsUseCase = GetTvSeriesDetailsUseCase(fakeRepository)
    )

    @Test
    fun initialState_isLoading() = runTest {
        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value).isInstanceOf(TvSeriesDetailsUiState.Loading::class.java)
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
        createViewModel(seriesId = 87108)

        advanceUntilIdle()

        assertThat(fakeRepository.requestedIds).containsExactly(87108)
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

    @Test
    fun whenArgumentIsMissing_usesInvalidIdSentinel() = runTest {
        // -1 coincide con el android:defaultValue del <argument> en nav_graph
        TvSeriesDetailsViewModel(
            savedStateHandle = SavedStateHandle(),
            getTvSeriesDetailsUseCase = GetTvSeriesDetailsUseCase(fakeRepository)
        )

        advanceUntilIdle()

        assertThat(fakeRepository.requestedIds).containsExactly(-1)
    }
}