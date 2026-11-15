package com.alexvicente.moviedb.features.tv_series.presentation.ui

import android.app.Application
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.core.presentation.theme.MovieDbTheme
import com.alexvicente.moviedb.features.tv_series.presentation.TvSeasonUiState
import com.alexvicente.moviedb.testutil.factories.TvFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    qualifiers = "w360dp-h1600dp",
    application = Application::class
)
class TvSeasonScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun str(@StringRes id: Int): String = context.getString(id)

    private fun setScreen(
        uiState: TvSeasonUiState = TvSeasonUiState.Success(TvFactory.createSeasonDetails()),
        onRetry: () -> Unit = {},
        onBackClick: () -> Unit = {}
    ) = composeRule.setContent {
        MovieDbTheme {
            TvSeasonScreen(
                uiState = uiState,
                onRetry = onRetry,
                onBackClick = onBackClick
            )
        }
    }

    // ESTADOS
    @Test
    fun whenStateIsLoading_progressIsShown() {
        setScreen(TvSeasonUiState.Loading)

        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }

    @Test
    fun whenStateIsError_messageIsShown_andRetryInvokesCallback() {
        var retries = 0
        setScreen(TvSeasonUiState.Error("fallo"), onRetry = { retries++ })

        composeRule.onNodeWithText(str(R.string.tv_series_load_error)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.tv_series_retry)).performClick()

        assertThat(retries).isEqualTo(1)
    }

    @Test
    fun whenStateIsSuccess_seasonNameAndEpisodesAreShown() {
        setScreen()

        // el nombre de la temporada vive en la barra superior
        composeRule.onNodeWithText("Temporada 1").assertIsDisplayed()
        composeRule.onNodeWithText("Episodio 1").assertIsDisplayed()
        composeRule.onNodeWithText("Episodio 2").assertIsDisplayed()
    }

    @Test
    fun whenStateIsSuccess_headerShowsEpisodeCountWithPlural() {
        setScreen()

        val twoEpisodes = context.resources.getQuantityString(R.plurals.tv_series_episode_count, 2, 2)
        composeRule.onNodeWithText(twoEpisodes, substring = true).assertIsDisplayed()
    }

    // LISTA DE EPISODIOS VACIA
    @Test
    fun whenSeasonHasNoEpisodes_emptyMessageIsShown() {
        setScreen(TvSeasonUiState.Success(TvFactory.createSeasonDetails(episodes = emptyList())))

        composeRule.onNodeWithText(str(R.string.tv_series_season_no_episodes)).assertIsDisplayed()
    }

    @Test
    fun whenSeasonHasEpisodes_emptyMessageIsHidden() {
        // Control positivo del test anterior
        setScreen()

        composeRule.onNodeWithText(str(R.string.tv_series_season_no_episodes)).assertDoesNotExist()
    }

    // NAVEGACION
    @Test
    fun whenBackButtonClicked_invokesCallback() {
        var backs = 0
        setScreen(onBackClick = { backs++ })

        composeRule.onNodeWithContentDescription(str(R.string.tv_series_details_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}