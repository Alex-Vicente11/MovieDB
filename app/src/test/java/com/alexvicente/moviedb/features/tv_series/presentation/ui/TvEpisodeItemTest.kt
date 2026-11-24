package com.alexvicente.moviedb.features.tv_series.presentation.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.core.presentation.theme.MovieDbTheme
import com.alexvicente.moviedb.features.tv_series.domain.model.TvEpisode
import com.alexvicente.moviedb.testutil.factories.TvSeasonFactory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h1600dp", application = Application::class)
class TvEpisodeItemTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private fun str(@StringRes id: Int): String = composeRule.activity.getString(id)

    // Sinopsis larga: en el ancho disponible ocupa bastante más de 3 líneas
    private val longOverview = "palabra ".repeat(80).trim()

    private fun setItem(episode: TvEpisode) = composeRule.setContent {
        MovieDbTheme { TvEpisodeItem(episode) }
    }

    // CALIFICACIÓN: la decide vote_count, no vote_average
    @Test
    fun whenEpisodeHasVotes_ratingIsShown() {
        setItem(TvSeasonFactory.createTvEpisode(voteAverage = 7.8, voteCount = 120))

        composeRule.onNodeWithText("★ ${7.8.toRatingText()}", substring = true).assertIsDisplayed()
    }

    @Test
    fun whenEpisodeHasNoVotes_ratingIsHidden() {
        // vote_average llega como 0.0 sin votos: mostrarlo sería un "★ 0.0" engañoso
        setItem(TvSeasonFactory.createTvEpisode(voteAverage = 0.0, voteCount = 0))

        composeRule.onNodeWithText("★", substring = true).assertDoesNotExist()
    }

    // SINOPSIS EXPANDIBLE
    @Test
    fun whenOverviewIsEmpty_noToggleIsShown() {
        setItem(TvSeasonFactory.createTvEpisode(overview = ""))

        composeRule.onNodeWithText(str(R.string.tv_series_episode_see_more)).assertDoesNotExist()
    }

    @Test
    fun whenOverviewFitsInThreeLines_noToggleIsShown() {
        setItem(TvSeasonFactory.createTvEpisode(overview = "Una sinopsis corta."))

        composeRule.onNodeWithText(str(R.string.tv_series_episode_see_more)).assertDoesNotExist()
    }

    @Test
    fun whenOverviewIsLong_seeMoreIsShown() {
        setItem(TvSeasonFactory.createTvEpisode(overview = longOverview))

        composeRule.onNodeWithText(str(R.string.tv_series_episode_see_more)).assertIsDisplayed()
    }

    @Test
    fun whenToggleClicked_expandsAndCollapses() {
        setItem(TvSeasonFactory.createTvEpisode(overview = longOverview))

        composeRule.onNodeWithText(str(R.string.tv_series_episode_see_more)).performClick()
        composeRule.onNodeWithText(str(R.string.tv_series_episode_see_less)).assertIsDisplayed()

        composeRule.onNodeWithText(str(R.string.tv_series_episode_see_less)).performClick()
        composeRule.onNodeWithText(str(R.string.tv_series_episode_see_more)).assertIsDisplayed()
    }
}