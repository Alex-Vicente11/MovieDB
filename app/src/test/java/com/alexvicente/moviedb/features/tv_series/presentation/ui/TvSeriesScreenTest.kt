package com.alexvicente.moviedb.features.tv_series.presentation.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.core.presentation.theme.MovieDbTheme
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeries
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesCategory
import com.alexvicente.moviedb.testutil.factories.TvSeriesFactory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h1600dp", application = Application::class)
class TvSeriesScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private fun str(@StringRes id: Int): String = composeRule.activity.getString(id)

    // HELPERS
    /** Estados de carga de Paging. prepend siempre completo: no hay página anterior. */
    private fun loadStates(
        refresh: LoadState = LoadState.NotLoading(endOfPaginationReached = false),
        append: LoadState = LoadState.NotLoading(endOfPaginationReached = false)
    ) = LoadStates(
        refresh = refresh,
        prepend = LoadState.NotLoading(endOfPaginationReached = true),
        append = append
    )

    private fun setScreen(
        series: List<TvSeries> = TvSeriesFactory.createTvSeriesList(
            size = 3, namePrefix = "Serie", posterPath = null, backdropPath = null
        ),
        states: LoadStates = loadStates(),
        selectedCategory: TvSeriesCategory = TvSeriesCategory.POPULAR,
        onCategorySelected: (TvSeriesCategory) -> Unit = {},
        onSeriesClick: (Int) -> Unit = {}
    ) {
        composeRule.setContent {
            // remember: sin él, cada recomposición crearía un flujo nuevo y reiniciaría Paging
            val flow = remember { flowOf(PagingData.from(series, sourceLoadStates = states)) }
            val items = flow.collectAsLazyPagingItems()
            MovieDbTheme {
                TvSeriesScreen(
                    tvSeries = items,
                    selectedCategory = selectedCategory,
                    onCategorySelected = onCategorySelected,
                    onSeriesClick = onSeriesClick,
                    contentPadding = PaddingValues()
                )
            }
        }
        composeRule.waitForIdle()
    }

    // ESTADOS DE LA LISTA (refresh)
    @Test
    fun whenRefreshSucceeds_seriesNamesAreShown() {
        setScreen(series = TvSeriesFactory.createTvSeriesList(3, namePrefix = "Serie", posterPath = null, backdropPath = null))

        composeRule.onNodeWithText("Serie 1").assertIsDisplayed()
        composeRule.onNodeWithText("Serie 3").assertIsDisplayed()
    }

    @Test
    fun whenRefreshIsLoading_progressIsShown_andNoItems() {
        setScreen(series = emptyList(), states = loadStates(refresh = LoadState.Loading))

        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.tv_series_load_error)).assertDoesNotExist()
    }

    @Test
    fun whenRefreshFails_errorMessageAndRetryAreShown() {
        setScreen(
            series = emptyList(),
            states = loadStates(refresh = LoadState.Error(RuntimeException("sin red")))
        )

        composeRule.onNodeWithText(str(R.string.tv_series_load_error)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.tv_series_retry)).assertIsDisplayed()
    }

    // ESTADOS DE PAGINACIÓN (append): los ítems siguen visibles
    @Test
    fun whenAppendFails_itemsRemain_andRetryButtonAppears() {
        setScreen(states = loadStates(append = LoadState.Error(RuntimeException("sin red"))))

        composeRule.onNodeWithText("Serie 1").assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.tv_series_retry)).assertIsDisplayed()
        // un fallo al paginar no es un fallo de pantalla completa
        composeRule.onNodeWithText(str(R.string.tv_series_load_error)).assertDoesNotExist()
    }

    // NAVEGACIÓN POR LAMBDAS
    @Test
    fun whenSeriesClicked_passesSeriesId() {
        var clickedId: Int? = null
        setScreen(onSeriesClick = { clickedId = it })

        composeRule.onNodeWithText("Serie 2").performClick()

        assertThat(clickedId).isEqualTo(2)
    }

    // TABS DE CATEGORÍA
    @Test
    fun whenTopRatedTabClicked_emitsTopRatedCategory() {
        var selected: TvSeriesCategory? = null
        setScreen(onCategorySelected = { selected = it })

        composeRule.onNodeWithText(str(R.string.tv_series_category_top_rated)).performClick()

        assertThat(selected).isEqualTo(TvSeriesCategory.TOP_RATED)
    }

    @Test
    fun selectedCategory_isTheOnlyTabMarkedAsSelected() {
        setScreen(selectedCategory = TvSeriesCategory.TOP_RATED)

        composeRule.onNodeWithText(str(R.string.tv_series_category_top_rated)).assertIsSelected()
        composeRule.onNodeWithText(str(R.string.tv_series_category_popular)).assertIsNotSelected()
    }
}