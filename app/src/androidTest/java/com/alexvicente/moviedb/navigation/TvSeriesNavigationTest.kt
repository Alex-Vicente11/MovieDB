package com.alexvicente.moviedb.navigation

import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.alexvicente.moviedb.MainActivity
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.favorites.di.FavoritesModule
import com.alexvicente.moviedb.features.favorites.domain.repository.FavoritesRepository
import com.alexvicente.moviedb.features.genres.di.GenresModule
import com.alexvicente.moviedb.features.genres.domain.repository.GenresRepository
import com.alexvicente.moviedb.features.genres.domain.repository.MoviesByGenreRepository
import com.alexvicente.moviedb.features.popular_movies.di.PopularMoviesModule
import com.alexvicente.moviedb.features.popular_movies.domain.repository.PopularMoviesRepository
import com.alexvicente.moviedb.features.search.di.SearchModule
import com.alexvicente.moviedb.features.search.domain.repository.SearchRepository
import com.alexvicente.moviedb.features.tv_series.di.TvSeriesModule
import com.alexvicente.moviedb.features.tv_series.domain.repository.TvSeasonRepository
import com.alexvicente.moviedb.features.tv_series.domain.repository.TvSeriesCastRepository
import com.alexvicente.moviedb.features.tv_series.domain.repository.TvSeriesDetailsRepository
import com.alexvicente.moviedb.features.tv_series.domain.repository.TvSeriesRepository
import com.alexvicente.moviedb.features.tv_series.presentation.TvSeasonViewModel
import com.alexvicente.moviedb.features.tv_series.presentation.TvSeriesDetailsViewModel
import com.alexvicente.moviedb.testutil.factories.MovieFactory
import com.alexvicente.moviedb.testutil.fakes.FakeTvSeasonRepository
import com.alexvicente.moviedb.testutil.fakes.FakeTvSeriesCastRepository
import com.alexvicente.moviedb.testutil.fakes.FakeTvSeriesDetailsRepository
import com.alexvicente.moviedb.testutil.fakes.FakeTvSeriesRepository
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(
    TvSeriesModule::class,
    PopularMoviesModule::class,
    SearchModule::class,
    GenresModule::class,
    FavoritesModule::class
)
@RunWith(AndroidJUnit4::class)
class TvSeriesNavigationTest {

    // HiltAndroidRule primero: debe existir el componente antes que cualquier otra cosa
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    // Regla "vacía": la Activity se lanza a mano en setUp, DESPUÉS de configurar los mocks
    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    // FAKES DE TV SERIES
    private val fakeSeries = FakeTvSeriesRepository()
    private val fakeDetails = FakeTvSeriesDetailsRepository()
    private val fakeCast = FakeTvSeriesCastRepository()
    private val fakeSeason = FakeTvSeasonRepository()

    @BindValue @JvmField
    val tvSeriesRepository: TvSeriesRepository = fakeSeries

    @BindValue @JvmField
    val tvSeriesDetailsRepository: TvSeriesDetailsRepository = fakeDetails

    @BindValue @JvmField
    val tvSeriesCastRepository: TvSeriesCastRepository = fakeCast

    @BindValue @JvmField
    val tvSeasonRepository: TvSeasonRepository = fakeSeason

    // MOCKS DE LAS OTRAS FEATURES (MainActivity arranca en Películas)
    @BindValue @JvmField
    val popularMoviesRepository: PopularMoviesRepository = mockk(relaxed = true)

    @BindValue @JvmField
    val searchRepository: SearchRepository = mockk(relaxed = true)

    @BindValue @JvmField
    val genresRepository: GenresRepository = mockk(relaxed = true)

    @BindValue @JvmField
    val favoritesRepository: FavoritesRepository = mockk(relaxed = true)

    @BindValue @JvmField
    val moviesByGenreRepository: MoviesByGenreRepository = mockk(relaxed = true)

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        hiltRule.inject()

        every { popularMoviesRepository.getPopularMovies() } returns flowOf(
            Resource.Success(MovieFactory.createMovieList(5))
        )
        every { searchRepository.searchMovies(any()) } returns flowOf(
            Resource.Success(MovieFactory.createMovieList(3))
        )
        every { favoritesRepository.getAllFavoritesRepo() } returns flowOf(emptyList())
        every { favoritesRepository.isFavoriteRepo(any()) } returns flowOf(false)

        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    // HELPERS
    private fun str(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    /** Espera a que haya al menos un nodo con ese texto (las transiciones de Fragment son asíncronas). */
    private fun waitForText(text: String, timeoutMs: Long = 5_000) {
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun currentDestinationId(): Int? {
        var id: Int? = null
        scenario.onActivity { activity ->
            val navHost = activity.supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            id = navHost.navController.currentDestination?.id
        }
        return id
    }

    private fun currentArgs(): Bundle? {
        var args: Bundle? = null
        scenario.onActivity { activity ->
            val navHost = activity.supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            args = navHost.navController.currentBackStackEntry?.arguments
        }
        return args
    }

    private fun goToSeriesList() {
        onView(withId(R.id.tvSeriesFragment)).perform(click())
        waitForText("Serie 1")
    }

    private fun openSeriesDetails(seriesName: String, detailsTitle: String) {
        goToSeriesList()
        composeRule.onNodeWithText(seriesName).performClick()
        waitForText(detailsTitle)
    }

    /** Las temporadas están al final del LazyColumn: pueden no estar compuestas todavía. */
    private fun clickWithScroll(text: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
        composeRule.onNodeWithText(text).performClick()
    }

    private fun assertBottomNavVisible() =
        onView(withId(R.id.bottomNavigationView)).check(matches(isDisplayed()))

    private fun assertBottomNavHidden() =
        onView(withId(R.id.bottomNavigationView))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))

    // LISTA
    @Test
    fun whenSeriesTabClicked_listIsShown_andBottomNavStaysVisible() {
        // When
        goToSeriesList()

        // Then
        assertThat(currentDestinationId()).isEqualTo(R.id.tvSeriesFragment)
        assertBottomNavVisible()
    }

    // LISTA -> DETALLE
    @Test
    fun whenSeriesClicked_navigatesToDetails_withSeriesId_andHidesBottomNav() {
        // Given
        goToSeriesList()

        // When
        composeRule.onNodeWithText("Serie 2").performClick()
        waitForText("Detalle 2")

        // Then: destino, argumento y petición a la capa de datos
        assertThat(currentDestinationId()).isEqualTo(R.id.tvSeriesDetailsFragment)
        assertThat(currentArgs()?.getInt(TvSeriesDetailsViewModel.ARG_SERIES_ID, -1)).isEqualTo(2)
        assertThat(fakeDetails.requestedIds).contains(2)
        assertBottomNavHidden()
    }

    // DETALLE -> TEMPORADA
    @Test
    fun whenSeasonClicked_navigatesToSeason_withSeriesIdAndSeasonNumber() {
        openSeriesDetails(seriesName = "Serie 2", detailsTitle = "Detalle 2")

        clickWithScroll("Temporada 1")
        waitForText("Episodio 1")

        assertThat(currentDestinationId()).isEqualTo(R.id.tvSeasonFragment)
        val args = currentArgs()
        assertThat(args?.getInt(TvSeasonViewModel.ARG_SERIES_ID, -1)).isEqualTo(2)
        assertThat(args?.getInt(TvSeasonViewModel.ARG_SEASON_NUMBER, -1)).isEqualTo(1)
        assertThat(fakeSeason.requests.last()).isEqualTo(2 to 1)
        composeRule.onNodeWithText("Temporada 1").assertIsDisplayed()
    }

    @Test
    fun whenSpecialsSeasonClicked_seasonZeroTravelsEndToEnd() {
        // El 0 es válido («Especiales»): no debe convertirse en el centinela -1 en ningún punto
        openSeriesDetails(seriesName = "Serie 2", detailsTitle = "Detalle 2")

        clickWithScroll("Especiales")
        waitForText("Episodio 1")

        assertThat(currentArgs()?.getInt(TvSeasonViewModel.ARG_SEASON_NUMBER, -1)).isEqualTo(0)
        // la petición que llegó al repositorio también lleva el 0
        assertThat(fakeSeason.requests.last()).isEqualTo(2 to 0)
        composeRule.onNodeWithText("Especiales").assertIsDisplayed()
    }

    // BACKSTACK
    @Test
    fun backNavigation_returnsThroughDetailsToList_andBottomNavReappears() {
        openSeriesDetails(seriesName = "Serie 2", detailsTitle = "Detalle 2")
        clickWithScroll("Temporada 1")
        waitForText("Episodio 1")

        // Temporada -> Detalle: botón de volver de Compose (onBackClick -> navigateUp)
        composeRule.onNodeWithContentDescription(str(R.string.tv_series_details_back)).performClick()
        waitForText("Detalle 2")
        assertThat(currentDestinationId()).isEqualTo(R.id.tvSeriesDetailsFragment)

        // Detalle -> Lista: botón físico de atrás
        Espresso.pressBack()
        waitForText("Serie 1")
        assertThat(currentDestinationId()).isEqualTo(R.id.tvSeriesFragment)
        assertBottomNavVisible()
    }

    // CAMBIO DE PESTAÑAS (caracterización del comportamiento actual)
    @Test
    fun selectedCategory_survivesSwitchingTabs() {
        goToSeriesList()

        composeRule.onNodeWithText(str(R.string.tv_series_category_top_rated)).performClick()
        waitForText("Top 1")

        // Series -> Películas -> Series
        onView(withId(R.id.popularMoviesFragment)).perform(click())
        onView(withId(R.id.tvSeriesFragment)).perform(click())
        // Si la categoría se conserva, se ve la lista de mejor valoradas y no la de populares
        waitForText("Top 1")
    }

    // E2E: un recorrido largo
    @Test
    fun fullJourney_listToSeasonToExpandedEpisodeAndBack() {
        // Lista -> Detalle -> Temporada
        openSeriesDetails(seriesName = "Serie 1", detailsTitle = "Detalle 1")
        clickWithScroll("Temporada 1")
        waitForText("Episodio 1")

        // Expandir y contraer la sinopsis del primer episodio
        composeRule.onNodeWithText(str(R.string.tv_series_episode_see_more)).performClick()
        composeRule.onNodeWithText(str(R.string.tv_series_episode_see_less)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.tv_series_episode_see_less)).performClick()
        composeRule.onNodeWithText(str(R.string.tv_series_episode_see_more)).assertIsDisplayed()

        // Volver hasta la lista
        composeRule.onNodeWithContentDescription(str(R.string.tv_series_details_back)).performClick()
        waitForText("Detalle 1")
        Espresso.pressBack()
        waitForText("Serie 1")

        assertThat(currentDestinationId()).isEqualTo(R.id.tvSeriesFragment)
        assertBottomNavVisible()
    }
}