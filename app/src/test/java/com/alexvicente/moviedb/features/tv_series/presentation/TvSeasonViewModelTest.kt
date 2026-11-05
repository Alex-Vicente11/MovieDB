package com.alexvicente.moviedb.features.tv_series.presentation

import androidx.lifecycle.SavedStateHandle
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.tv_series.domain.usecase.GetTvSeasonDetailsUseCase
import com.alexvicente.moviedb.testutil.factories.TvSeasonFactory
import com.alexvicente.moviedb.testutil.fakes.FakeTvSeasonRepository
import com.alexvicente.moviedb.testutil.rules.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TvSeasonViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeTvSeasonRepository

    @Before
    fun setUp() {
        fakeRepository = FakeTvSeasonRepository()
    }

    private fun handleWith(seriesId: Int, seasonNumber: Int) = SavedStateHandle(
        mapOf(
            TvSeasonViewModel.ARG_SERIES_ID to seriesId,
            TvSeasonViewModel.ARG_SEASON_NUMBER to seasonNumber
        )
    )

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = handleWith(seriesId = 1416, seasonNumber = 1)
    ) = TvSeasonViewModel(
        savedStateHandle = savedStateHandle,
        getTvSeasonDetailsUseCase = GetTvSeasonDetailsUseCase(fakeRepository)
    )

    @Test
    fun initialState_isLoading() = runTest {
        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value).isInstanceOf(TvSeasonUiState.Loading::class.java)
    }

    @Test
    fun whenRepositorySucceeds_emitsSuccessWithSeason() = runTest {
        fakeRepository.result = Resource.Success(
            TvSeasonFactory.createTvSeasonDetails(
                seasonNumber = 23, name = "Temporada 23", episodeCount = 6
            )
        )
        val viewModel = createViewModel()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TvSeasonUiState.Success::class.java)
        val season = (state as TvSeasonUiState.Success).season
        assertThat(season.name).isEqualTo("Temporada 23")
        assertThat(season.episodes).hasSize(6)
    }

    @Test
    fun requestsTheSeriesIdAndSeasonNumberFromSavedStateHandle() = runTest {
        createViewModel(handleWith(seriesId = 87108, seasonNumber = 1))

        advanceUntilIdle()

        assertThat(fakeRepository.requested).containsExactly(87108 to 1)
    }

    @Test
    fun whenSeasonNumberIsZero_requestsSpecialsInsteadOfTreatingItAsMissing() = runTest {
        createViewModel(handleWith(seriesId = 1416, seasonNumber = 0))

        advanceUntilIdle()

        assertThat(fakeRepository.requested).containsExactly(1416 to 0)
    }

    @Test
    fun whenArgumentsAreMissing_usesInvalidIdSentinelForBoth() = runTest {
        // -1 coincide con el android:defaultValue de los <argument> en nav_graph.xml
        createViewModel(SavedStateHandle())

        advanceUntilIdle()

        assertThat(fakeRepository.requested).containsExactly(-1 to -1)
    }

    @Test
    fun whenRepositoryFails_emitsErrorWithMessage() = runTest {
        fakeRepository.result = Resource.Error("boom")
        val viewModel = createViewModel()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TvSeasonUiState.Error::class.java)
        assertThat((state as TvSeasonUiState.Error).message).isEqualTo("boom")
    }

    @Test
    fun whenRetryAfterError_loadsAgainAndEmitsSuccess() = runTest {
        fakeRepository.result = Resource.Error("boom")
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(TvSeasonUiState.Error::class.java)

        fakeRepository.result = Resource.Success(TvSeasonFactory.createTvSeasonDetails())
        viewModel.retry()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(TvSeasonUiState.Success::class.java)
        assertThat(fakeRepository.requested).hasSize(2)
    }
}
