package com.alexvicente.moviedb.features.tv_series.presentation.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.core.presentation.theme.MovieDbTheme
import com.alexvicente.moviedb.features.tv_series.presentation.CastUiState
import com.alexvicente.moviedb.features.tv_series.presentation.TvSeriesDetailsUiState
import com.alexvicente.moviedb.testutil.factories.TvFactory
import com.alexvicente.moviedb.testutil.factories.TvSeasonFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.collections.emptyList

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)  // el font scale solo tiene efecto si el texto se mide de verdad
@Config(
    sdk = [34],
    application = Application::class
)
class TvSeriesConfigurationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun str(@StringRes id: Int): String = composeRule.activity.getString(id)

    // Anchos (compacto, medio, expandido).
    private val compact = DpSize(400.dp, 500.dp)
    private val medium = DpSize(610.dp, 500.dp)
    private val expanded = DpSize(900.dp, 1000.dp)

    // HELPERS
    private fun setDetails(
        override: DeviceConfigurationOverride,
        uiState: TvSeriesDetailsUiState =
            TvSeriesDetailsUiState.Success(TvFactory.createSeriesDetails()),
        onRetry: () -> Unit = {},
        onSeasonClick: (Int) -> Unit = {}
    ) = composeRule.setContent {
        DeviceConfigurationOverride(override) {
            MovieDbTheme {
                TvSeriesDetailsScreen(
                    uiState = uiState,
                    castState = CastUiState.Success(emptyList()),
                    onRetry = onRetry,
                    onCastRetry = {},
                    onBackClick = {},
                    onSeasonClick = onSeasonClick
                )
            }
        }
    }

    private fun assertDetailsUsableAt(size: DpSize) {
        var clicked: Int? = null
        setDetails(DeviceConfigurationOverride.ForcedSize(size), onSeasonClick = { clicked = it })

        composeRule.onNodeWithText("Anatomía según Grey").assertIsDisplayed()
        // la temporada puede estar afuera de la ventana forzada: se hace scroll hasta ella
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Temporada 1"))
        composeRule.onNodeWithText("Temporada 1").performClick()

        assertThat(clicked).isEqualTo(1)
    }

    // TAMAÑOS DE VENTANA
    @Test
    fun details_isUsableAtCompactWidth() = assertDetailsUsableAt(compact)

    @Test
    fun details_isUsableAtMediumWidth() = assertDetailsUsableAt(medium)

    @Test
    fun details_isUsableAtExpandedWidth() = assertDetailsUsableAt(expanded)

    // FONT SCALE
    @Test
    fun errorScreen_retryRemainsClickable_withLargeFont() {
        var retries = 0
        setDetails(
            override = DeviceConfigurationOverride.FontScale(2f),
            uiState = TvSeriesDetailsUiState.Error("fallo"),
            onRetry = { retries++ }
        )

        composeRule.onNodeWithText(str(R.string.tv_series_retry)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.tv_series_retry)).performClick()

        assertThat(retries).isEqualTo(1)
    }

    @Test
    fun details_seasonRemainsClickable_withLargeFontInCompactWindow() {
        var clicked: Int? = null
        setDetails(
            override = DeviceConfigurationOverride.ForcedSize(compact) then
                DeviceConfigurationOverride.FontScale(2f),
            onSeasonClick = { clicked = it }
        )

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Temporada 1"))
        composeRule.onNodeWithText("Temporada 1").performClick()

        assertThat(clicked).isEqualTo(1)
    }

    @Test
    fun episodeToggle_works_withLargeFont() {
        // Con fuente grande la sinopsis ocupa más lineas: el toggle debe seguir apareciendo y funcionando
        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                MovieDbTheme {
                    TvEpisodeItem(TvSeasonFactory.createTvEpisode(overview = "palabra ".repeat(80).trim()))
                }
            }
        }

        composeRule.onNodeWithText(str(R.string.tv_series_episode_see_more)).performClick()
        composeRule.onNodeWithText(str(R.string.tv_series_episode_see_less)).assertIsDisplayed()
    }
}