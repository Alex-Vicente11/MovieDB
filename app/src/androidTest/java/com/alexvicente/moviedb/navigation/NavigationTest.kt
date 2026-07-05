package com.alexvicente.moviedb.navigation

import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import com.alexvicente.moviedb.features.popular_movies.presentation.adapter.MovieAdapter
import com.alexvicente.moviedb.features.search.di.SearchModule
import com.alexvicente.moviedb.features.search.domain.repository.SearchRepository
import com.alexvicente.moviedb.testutil.factories.MovieFactory
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(
    PopularMoviesModule::class,
    SearchModule::class,
    GenresModule::class,
    FavoritesModule::class
)
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    // REPOSITORIOS MOCKEADOS
    // MainActivity carga PopularMoviesFragment al inicio - necesitamos que
    // el repositorio tenga datos para que el RecyclerView sea visible
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


    @Before
    fun setUp() {
        hiltRule.inject()

        // Configuramos el mock ANTES de lanzar la Activity
        // PopularMoviesFragment llama getPopularMovies() en onViewCreated()
        every { popularMoviesRepository.getPopularMovies() } returns flowOf(
            Resource.Success(MovieFactory.createMovieList(5))
        )

        every { searchRepository.searchMovies(any()) } returns flowOf(
            Resource.Success(MovieFactory.createMovieList(3))
        )

        every { favoritesRepository.getAllFavoritesRepo() } returns flowOf(
            emptyList()
        )

        every { favoritesRepository.isFavoriteRepo(any()) } returns flowOf(false)
    }

    // HELPER
    /**
     * Lanza MainActivity y retorna el scenario para acceder al NavController.
     * A diferencia de los test de Fragmnet, aquí usamos la Activity real porque
     * queremos verificar la navegación completa con BottomNav
     */
    private fun launchMainActivity() = ActivityScenario.launch(MainActivity::class.java)

    // TESTS DE DESTINO INICIAL

    @Test
    fun whenAppLaunches_popularMoviesIsStartDestination() {
        // Given / When - lanzamos la app
        launchMainActivity()

        // Then - el RecyclerView de películas populares es visible
        // (confirma que PopularMoviesFragment es el destino inicial)
        onView(withId(R.id.recyclerView)).check(matches(isDisplayed()))
    }

    @Test
    fun whenAppLaunches_bottomNavigationIsVisible() {
        // Given / When
        launchMainActivity()

        // Then - BottomNavigationView visible en destinos top-level
        onView(withId(R.id.bottomNavigationView)).check(matches(isDisplayed()))
    }

    // TESTS DE NAVEGACIÓN POR BOTTOMNAVIGATIONVIEW

    @Test
    fun whenFavoritesTabClicked_navigatesToFavoritesFragment() {
        // Given
        launchMainActivity()

        // When - click en la pestaña de favoritos del BottomNav
        onView(withId(R.id.favoritesFragment)).perform(click())

        // Then - el Fragment de favoritos es visible
        // Verificamos con un View que solo existe en fragment_favorites.xml
        onView(withId(R.id.recyclerViewFavorites)).check(matches(isDisplayed()))
    }

    // REVISAR ESTE TEST!!
    @Test
    fun whenFavoritesTabClicked_navigatesToFavoritesFragmentEmpty() {
        // Given
        launchMainActivity()

        onView(withId(R.id.favoritesFragment)).perform(click())

        // emptyState es predecible - favoritesRepository retorna lista vacía
        onView(withId(R.id.emptyState)).check(matches(isDisplayed()))
    }

    @Test
    fun whenGenresTabClicked_navigatesToGenresFragment() {
        val scenario = launchMainActivity()

        onView(withId(R.id.genresFragment))
            .perform(click())

        scenario.onActivity { activity ->
            val navHostFragment =
                activity.supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

            assertEquals(
                R.id.genresFragment,
                navHostFragment.navController.currentDestination?.id
            )
        }
    }

    @Test
    fun whenNavigatingToSecondaryDestination_bottomNavIsHidden() {
        // Given - populares cargadas con items clickeables
        launchMainActivity()

        // When - click en una película navega a MovieDetailsFragment (destino secundario)
        onView(withId(R.id.recyclerView)).perform(
            RecyclerViewActions.actionOnItemAtPosition<MovieAdapter.MovieViewHolder>(
                0,
                click()
            )
        )

        // Then - BottomNav se oculta en destinos secundarios
        // comportamiento definido en addOnDestinationChangedListener de MainActivity
        onView(withId(R.id.bottomNavigationView)).check(matches(not(isDisplayed())))
        // AQUÍ SALTA UN MENSAJE ERROR DE RESULTADOS
    }

    // TESTS DE BACKSTACK

    @Test
    fun whenNavigatingToDetailsAndPressBack_returnsToPopularMovies() {
        val scenario = launchMainActivity()

        onView(withId(R.id.recyclerView))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<MovieAdapter.MovieViewHolder>(
                    0,
                    click()
                )
            )

        scenario.onActivity { activity ->
            val navHostFragment =
                activity.supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

            assertEquals(
                R.id.movieDetailsFragment,
                navHostFragment.navController.currentDestination?.id
            )
        }

        Espresso.pressBack()

        scenario.onActivity { activity ->
            val navHostFragment =
                activity.supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

            assertEquals(
                R.id.popularMoviesFragment,
                navHostFragment.navController.currentDestination?.id
            )
        }
    }

    @Test
    fun whenSwitchingTabs_bottomNavRestoresCorrectFragment() {
        // Given
        launchMainActivity()

        // Navegar a favoritos
        onView(withId(R.id.favoritesFragment)).perform(click())

        // When - volver a populares
        onView(withId(R.id.popularMoviesFragment)).perform(click())

        // Then - PopularMoviesFragment está activo con su contenido
        onView(withId(R.id.recyclerView)).check(matches(isDisplayed()))
        onView(withId(R.id.bottomNavigationView)).check(matches(isDisplayed()))
    }

    // TESTS DEL NAVCONTROLLER REAL

    @Test
    fun whenMovieClicked_navControllerCurrentDestinationIsMovieDetails() {
        // Given
        val scenario = launchMainActivity()

        // When
        onView(withId(R.id.recyclerView)).perform(
            RecyclerViewActions.actionOnItemAtPosition<MovieAdapter.MovieViewHolder>(
                0,
                click()
            )
        )

        // Then
        scenario.onActivity { activity ->
            val navHostFragment = activity.supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment)
                    as NavHostFragment

            val currentDestinationId = checkNotNull(
                navHostFragment.navController.currentDestination?.id
            ) { "currentDestination no debería ser null" }

            assertThat(currentDestinationId).isEqualTo(R.id.movieDetailsFragment)
        }
    }
}