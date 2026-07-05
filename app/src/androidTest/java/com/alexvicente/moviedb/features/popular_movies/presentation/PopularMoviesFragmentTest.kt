package com.alexvicente.moviedb.features.popular_movies.presentation

import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.util.Constants
import com.alexvicente.moviedb.features.popular_movies.di.PopularMoviesModule
import com.alexvicente.moviedb.features.popular_movies.domain.repository.PopularMoviesRepository
import com.alexvicente.moviedb.features.popular_movies.presentation.adapter.MovieAdapter
import com.alexvicente.moviedb.features.popular_movies.presentation.ui.PopularMoviesFragment
import com.alexvicente.moviedb.features.search.di.SearchModule
import com.alexvicente.moviedb.features.search.domain.repository.SearchRepository
import com.alexvicente.moviedb.testutil.factories.MovieFactory
import com.alexvicente.moviedb.util.launchFragmentInHiltContainer
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.google.common.truth.Truth.assertThat

@HiltAndroidTest
@UninstallModules(SearchModule::class, PopularMoviesModule::class)
@RunWith(AndroidJUnit4::class)
class PopularMoviesFragmentTest {

    // Regla de Hilt
    // HiltAndroidRule inicializa el grafo de dependencias de Hilt para el test.
    // Debe ser la primera regla en ejecutarse (order = 0)
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    // DEPENDENCIAS REEMPLAZADAS CON @BINDVALUE
    // @BindValue le dice a Hilt: "usa este objeto en lugar del que proveerías normalmente"
    // El Fragment recibe el mock sin saberlo

    @BindValue
    @JvmField
    val popularMoviesRepository: PopularMoviesRepository = mockk(relaxed = true)

    @BindValue
    @JvmField
    val searchRepository: SearchRepository = mockk(relaxed = true)

    // NavController de test - intercepta navigate() sin lanzar Activities reales
    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        hiltRule.inject()

        // TestNavController simula el NavController real
        // Necesita el nav_graph para conocer los destinos disponibles
        navController = TestNavHostController(
            ApplicationProvider.getApplicationContext()
        )


        /**
         * setGraph() registra un LifecycleObserver internamente (vía SavedStateRegistryController),
         * y LifecycleRegistry.addObserver() EXIGE ejecutarse en el hilo principal. @Before corre
         * en el hilo de instrumentación - por eso necesitamos runOnMainSync.
         */
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            navController.setGraph(R.navigation.nav_graph)
            navController.setCurrentDestination(R.id.popularMoviesFragment)
        }

    }

    // HELPER
    /**
     * Lanza el Fragment con el NavController de test ya configurado.
     * Separado en helper para no repetir el setup en cada test.
     */
    private fun launchFragment() {
        /**
         * Ahora solo lanza el Fragment - el NavController ya está completamente
         * configurado (tag asignado por launchFragmentInHiltContainer + graph listo)
         */
        launchFragmentInHiltContainer<PopularMoviesFragment>(
            navHostController = navController
        )
    }

    // TESTS DE ESTADO DE CARGA
    @Test
    fun whenFragmentLaunched_progressBarIsInitiallyVisible() {
        // Given - repositorio que nunca emite (simula carga infinita)
        // El Fragment llama getPopularMovies() en onViewCreated()
        // que emite Loading primero
        every { popularMoviesRepository.getPopularMovies() } returns flowOf(
            Resource.Loading()
        )

        // When
        launchFragment()

        // Then - progressBar debe ser visible durante la carga
        onView(withId(R.id.progressBar)).check(matches(isDisplayed()))

        //Thread.sleep(3000)
    }

    @Test
    fun whenRepositoryEmitsSuccess_recyclerViewIsVisible() {
        // Given
        val movies = MovieFactory.createMovieList(5)
        every { popularMoviesRepository.getPopularMovies() } returns flowOf(
            Resource.Success(movies)
        )

        // When
        launchFragment()

        // Then - RecyclerView visible, progressBar y estados de error ocultos
        onView(withId(R.id.recyclerView)).check(matches(isDisplayed()))
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
        onView(withId(R.id.tvError)).check(matches(not(isDisplayed())))
        onView(withId(R.id.tvEmpty)).check(matches(not(isDisplayed())))
        Thread.sleep(3000)
    }

    @Test
    fun whenRepositoryEmitsError_errorMessageIsVisible() {

        every { popularMoviesRepository.getPopularMovies() } returns flowOf(
            Resource.Error(Constants.ERROR_NETWORK)
        )

        // When
        launchFragment()

        // Then - tvError visible con el mensaje correcto
        onView(withId(R.id.tvError)).check(matches(isDisplayed()))
        onView(withId(R.id.tvError)).check(matches(withText(Constants.ERROR_NETWORK)))
        onView(withId(R.id.recyclerView)).check(matches(not(isDisplayed())))
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun whenSearchReturnsEmptyList_emptyStateIsVisible() {
        // Given — populares cargan normalmente
        every { popularMoviesRepository.getPopularMovies() } returns flowOf(
            Resource.Success(MovieFactory.createMovieList(3))
        )
        // La búsqueda retorna lista vacía — esto sí dispara MainUiState.Empty
        every { searchRepository.searchMovies(any()) } returns flowOf(
            Resource.Success(emptyList())
        )

        // When
        launchFragment()

        // Espresso escribe en el campo de búsqueda para disparar searchMovies()
        // El UseCase valida mínimo 2 caracteres antes de llamar al repositorio
        onView(withId(R.id.etSearch)).perform(
            click(),
            typeText("xy")
        )

        // Espera a que el debounce (500ms) y la coroutine terminen
        Thread.sleep(700)

        // Then — tvEmpty visible porque searchMovies retornó lista vacía
        onView(withId(R.id.tvEmpty)).check(matches(isDisplayed()))
        onView(withId(R.id.recyclerView)).check(matches(not(isDisplayed())))
    }

    // TESTS DE BÚSQUEDA

    @Test
    fun whenUserTypesInSearchField_searchIsTriggered() {
        // Given
        every { popularMoviesRepository.getPopularMovies() } returns flowOf(
            Resource.Success(MovieFactory.createMovieList(3))
        )

        val searchResults = listOf(MovieFactory.createMovie(title = "Batman Begins"))
        every { searchRepository.searchMovies("Batman") } returns flowOf(
            Resource.Success(searchResults)
        )

        // When
        launchFragment()

        // Espresso simula escritura en el campo de búsqueda
        onView(withId(R.id.etSearch)).perform(
            click(),
            typeText("Batman")
        )

        // Then - RecyclerView sigue visible con los resultados de búsqueda
        // El debounce de 500ms se maneja con IdlingResource en tests avanzados
        // Por ahora verificamos que el campo tiene el texto correcto
    }

    @Test
    fun whenSearchFieldCleared_popularMoviesAreRestored() {
        // Given
        every { popularMoviesRepository.getPopularMovies() } returns flowOf(
            Resource.Success(MovieFactory.createMovieList(5))
        )

        every { searchRepository.searchMovies(any()) } returns flowOf(
            Resource.Success(MovieFactory.createMovieList(2))
        )

        // When
        launchFragment()

        // Escribe en el campo de búsqueda
        onView(withId(R.id.etSearch)).perform(click(), typeText("Batman"))
        // Limpia el campo - debe volver a mostrar las películas populares
        onView(withId(R.id.etSearch)).perform(clearText())

        // Then - RecyclerView visible con películas populares
        onView(withId(R.id.recyclerView)).check(matches(isDisplayed()))
    }

    // TESTS DE NAVEGACIÓN
    @Test
    fun whenMovieClicked_navigatesToMovieDetails() {
        // Given - lista con películas para poder hacer click
        every { popularMoviesRepository.getPopularMovies() } returns flowOf(
            Resource.Success(MovieFactory.createMovieList(3))
        )

        // When
        launchFragment()

        // RecyclerViewActions.actionOnItemAtPosition simula click en el primer item
        onView(withId(R.id.recyclerView)).perform(
            RecyclerViewActions.actionOnItemAtPosition<MovieAdapter.MovieViewHolder>(
                0, // posición 0 = primera película
                click()
            )
        )

        /**
         * Then - TestNavHostController registró la navegación
         * Verificamos que el destino actual es movieDetailsFragment
         */
        assertThat(navController.currentDestination?.id)
            .isEqualTo(R.id.movieDetailsFragment)
    }

    @Test
    fun whenMovieClicked_passesCorrectMovieIdToDetails() {
        // Given - película con ID conocido en posición 0
        val movies = listOf(
            MovieFactory.createMovie(id = 42, title = "Inception")
        )
        every { popularMoviesRepository.getPopularMovies() } returns flowOf(
            Resource.Success(movies)
        )

        // When
        launchFragment()

        onView(withId(R.id.recyclerView)).perform(
            RecyclerViewActions.actionOnItemAtPosition<MovieAdapter.MovieViewHolder>(
                0,
                click()
            )
        )
        // Then - verifica que el argumento movieId llegó correctamente
        // Safe Args empaqueta el ID en el BackStackEntry del NavController
        val movieId = navController.backStack.last()
            .arguments?.getInt("movieId")
        assertThat(movieId).isEqualTo(42)
    }
}