package com.alexvicente.moviedb.features.tv_series.presentation.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.core.presentation.theme.MovieDbTheme
import com.alexvicente.moviedb.features.tv_series.presentation.TvSeasonUiState
import com.alexvicente.moviedb.testutil.factories.TvSeasonFactory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE) // necesita medir texto para que aparezca "Ver más"
@Config(
    sdk = [34],
    qualifiers = "w360dp-h640dp",
    application = Application::class
)
class TvSeriesStateRestorationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun str(@StringRes id: Int): String = composeRule.activity.getString(id)

    private val longOverview = "palabra ".repeat(80).trim()

    @Test
    fun episodeExpandedState_survivesRecreation() {
        // Given
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            MovieDbTheme { TvEpisodeItem(TvSeasonFactory.createTvEpisode(overview = longOverview)) }
        }
        composeRule.onNodeWithText(str(R.string.tv_series_episode_see_more)).performClick()

        // When: se simula la recreacion de la Activity (rotación, proceso muerto)
        restorationTester.emulateSavedInstanceStateRestore()

        // Then
        composeRule.onNodeWithText(str(R.string.tv_series_episode_see_less)).assertIsDisplayed()
    }

    @Test
    fun seasonOverviewExpandedState_survivesRecreation() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            MovieDbTheme {
                TvSeasonScreen(
                    uiState = TvSeasonUiState.Success(
                        TvSeasonFactory.createTvSeasonDetails(overview = longOverview)
                    ),
                    onRetry = {},
                    onBackClick = {}
                )
            }
        }
        composeRule.onNodeWithText(str(R.string.tv_series_episode_see_more)).performClick()

        // Precondición: el toque realmente expandió. Si falla aquí, el problema no es la restauración
        composeRule.onNodeWithText(str(R.string.tv_series_episode_see_less)).assertIsDisplayed()

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithText(str(R.string.tv_series_episode_see_less)).assertIsDisplayed()
    }

    @Test
    fun expandedEpisode_survivesScrollingOutAndBackIn() {
        // Solo el episodio 1 tiene sinopsis larga: hay un único "Ver más" en pantalla
        val episodes = List(40) {
            TvSeasonFactory.createTvEpisode(
                id = it + 1,
                episodeNumber = it + 1,
                overview = if (it == 0) longOverview else ""
            )
        }
        composeRule.setContent {
            MovieDbTheme {
                TvSeasonScreen(
                    uiState = TvSeasonUiState.Success(
                        TvSeasonFactory.createTvSeasonDetails(episodes = episodes)
                    ),
                    onRetry = {},
                    onBackClick = {}
                )
            }
        }
        composeRule.onNodeWithText(str(R.string.tv_series_episode_see_more)).performClick()

        // El episodio 1 sale de la composicion del LazyColumn y vuelve
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Episodio 40"))
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Episodio 1"))
        composeRule.onNodeWithText(str(R.string.tv_series_episode_see_less)).assertIsDisplayed()
    }
}