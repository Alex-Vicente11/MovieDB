package com.alexvicente.moviedb.features.tv_series.presentation

import app.cash.turbine.test
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesCategory
import com.alexvicente.moviedb.features.tv_series.domain.usecase.GetTvSeriesUseCase
import com.alexvicente.moviedb.testutil.factories.TvSeriesFactory
import com.alexvicente.moviedb.testutil.fakes.FakeTvSeriesRepository
import com.alexvicente.moviedb.testutil.rules.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TvSeriesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeTvSeriesRepository
    private lateinit var viewModel: TvSeriesViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeTvSeriesRepository().apply {
            seriesByCategory = mapOf(
                TvSeriesCategory.POPULAR to TvSeriesFactory.createTvSeriesList(3),
                TvSeriesCategory.TOP_RATED to listOf(
                    TvSeriesFactory.createTvSeries(id = 100, name = "Top")
                )
            )
        }
        viewModel = TvSeriesViewModel(GetTvSeriesUseCase(fakeRepository))
    }

    @Test
    fun initialCategoryIsPopular() = runTest {
        assertThat(viewModel.selectedCategory.value).isEqualTo(TvSeriesCategory.POPULAR)
    }

    @Test
    fun whenCategorySelected_emitsNewCategory() = runTest {
        viewModel.selectedCategory.test {
            assertThat(awaitItem()).isEqualTo(TvSeriesCategory.POPULAR)

            viewModel.onCategorySelected(TvSeriesCategory.TOP_RATED)

            assertThat(awaitItem()).isEqualTo(TvSeriesCategory.TOP_RATED)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun whenSameCategorySelected_noNewEmission() = runTest {
        // Documenta la decisión de no repetir el `if` de Géneros: StateFlow ya ignora iguales
        viewModel.selectedCategory.test {
            assertThat(awaitItem()).isEqualTo(TvSeriesCategory.POPULAR)

            viewModel.onCategorySelected(TvSeriesCategory.POPULAR)

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun whenCategoryChanges_repositoryIsQueriedForTheNewCategory() = runTest {
        // backgroundScope se cancela solo al terminar el test: no deja corrutinas pendientes
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.tvSeries.collect { }
        }
        advanceUntilIdle()
        assertThat(fakeRepository.requestedCategories).containsExactly(TvSeriesCategory.POPULAR)

        viewModel.onCategorySelected(TvSeriesCategory.TOP_RATED)
        advanceUntilIdle()

        assertThat(fakeRepository.requestedCategories)
            .containsExactly(TvSeriesCategory.POPULAR, TvSeriesCategory.TOP_RATED).inOrder()
    }
}