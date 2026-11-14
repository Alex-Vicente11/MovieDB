package com.alexvicente.moviedb.features.tv_series.presentation.ui

import android.app.Application
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.core.presentation.theme.MovieDbTheme
import com.alexvicente.moviedb.features.tv_series.presentation.CastUiState
import com.alexvicente.moviedb.features.tv_series.presentation.TvSeriesDetailsUiState
import com.alexvicente.moviedb.testutil.factories.TvFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
// sdk 34: Robolectric 4.14.1 llega hasta API 35 y el proyecto compila con 36
// pantalla alta: el LazyColumn compone todoo y assertDoesNotExist() es fiable
@Config(sdk = [34],
    qualifiers = "w360dp-h1600dp",
    application = Application::class   // evita MovieDbApplication (Firebase, Timber, Hilt)
)
class TvSeriesDetailsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun str(@StringRes id: Int): String = context.getString(id)

    // HELPER: el Screen es stateless, no necesita Hilt ni ViewModel
    private fun setScreen(
        uiState: TvSeriesDetailsUiState = TvSeriesDetailsUiState.Success(TvFactory.createSeriesDetails()),
        castState: CastUiState = CastUiState.Success(emptyList()),
        onRetry: () -> Unit = {},
        onCastRetry: () -> Unit = {},
        onBackClick: () -> Unit = {},
        onSeasonClick: (Int) -> Unit = {}
    ) = composeRule.setContent {
        MovieDbTheme {
            TvSeriesDetailsScreen(
                uiState = uiState,
                castState = castState,
                onRetry = onRetry,
                onCastRetry = onCastRetry,
                onBackClick = onBackClick,
                onSeasonClick = onSeasonClick
            )
        }
    }

    // ESTADOS DEL DETALLE
    @Test
    fun whenStateIsError_messageIsShown_andRetryInvokesCallback() {
        // Given
        var retries = 0
        setScreen(TvSeriesDetailsUiState.Error("fallo"), onRetry = { retries++ })

        // Then / When
        composeRule.onNodeWithText(str(R.string.tv_series_load_error)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.tv_series_retry)).performClick()

        assertThat(retries).isEqualTo(1)
    }

    @Test
    fun whenStateIsSuccess_seriesNameAndSeasonsTitleAreShown() {
        setScreen()

        composeRule.onNodeWithText("Anatomía según Grey").assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.tv_series_details_seasons)).assertIsDisplayed()
    }

    @Test
    fun whenOverviewIsEmpty_placeholderIsShown() {
        setScreen(TvSeriesDetailsUiState.Success(TvFactory.createSeriesDetails(overview = "")))

        composeRule.onNodeWithText(str(R.string.tv_series_details_overview_empty)).assertIsDisplayed()
    }

    // NAVEGACIÓN POR LAMBDAS
    @Test
    fun whenSpecialsSeasonClicked_passesSeasonNumberZero() {
        // La temporada 0 es válida: no debe confundirse con «argumento ausente» (-1)
        var clicked: Int? = null
        setScreen(onSeasonClick = { clicked = it })

        composeRule.onNodeWithText("Especiales").performClick()

        assertThat(clicked).isEqualTo(0)
    }

    @Test
    fun whenRegularSeasonClicked_passesItsSeasonNumber() {
        var clicked: Int? = null
        setScreen(onSeasonClick = { clicked = it })

        composeRule.onNodeWithText("Temporada 1").performClick()

        assertThat(clicked).isEqualTo(1)
    }

    @Test
    fun whenBackButtonClicked_invokesCallback() {
        var backs = 0
        setScreen(onBackClick = { backs++ })

        composeRule.onNodeWithContentDescription(str(R.string.tv_series_details_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }

    // REPARTO: estado independiente del detalle
    @Test
    fun whenCastFails_detailsStillShown_andCastRetryInvokesCallback() {
        var castRetries = 0
        setScreen(castState = CastUiState.Error("fallo"), onCastRetry = { castRetries++ })

        // El detalle sigue funcionando aunque el reparto falle
        composeRule.onNodeWithText("Anatomía según Grey").assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.tv_series_cast_error)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.tv_series_retry)).performClick()

        assertThat(castRetries).isEqualTo(1)
    }

    @Test
    fun whenCastHasMembers_titleAndNamesAreShown() {
        // Control positivo del test siguiente
        setScreen(
            castState = CastUiState.Success(
                listOf(
                    TvFactory.createCastMember(id = 1, name = "Ellen Pompeo"),
                    TvFactory.createCastMember(id = 2, name = "Chandra Wilson")
                )
            )
        )

        composeRule.onNodeWithText(str(R.string.tv_series_details_cast)).assertIsDisplayed()
        composeRule.onNodeWithText("Ellen Pompeo").assertIsDisplayed()
    }

    @Test
    fun whenCastIsEmpty_sectionIsHidden() {
        setScreen(castState = CastUiState.Success(emptyList()))

        composeRule.onNodeWithText(str(R.string.tv_series_details_cast)).assertDoesNotExist()
    }
}