package com.example.apptest.features.genres

import androidx.navigation.testing.TestNavHostController
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.apptest.R
import com.example.apptest.core.data.util.Resource
import com.example.apptest.core.domain.model.Genre
import com.example.apptest.core.domain.model.Movie
import com.example.apptest.features.genres.di.GenresModule
import com.example.apptest.features.genres.domain.repository.GenresRepository
import com.example.apptest.features.genres.domain.repository.MoviesByGenreRepository
import com.example.apptest.features.genres.presentation.GenresFragment
import com.example.apptest.testutil.factories.MovieFactory
import com.example.apptest.util.genres.waitForView
import com.example.apptest.util.launchFragmentInHiltContainer
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.not
import org.hamcrest.core.AllOf.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados de Paging 3 con UI real.
 *
 * Diferencia vs MoviesByGenrePagingSourceTest:
 *   MoviesByGenrePagingSourceTest → unit test, PagingSource.load() puro,
 *                                   sin Android, verifica LoadResult
 *   MoviesByGenrePagingUiTest     → instrumentado, verifica que el
 *                                   RecyclerView renderiza items reales
 *                                   y que el footer de retry funciona
 *
 * Estrategia: PagingData.from() crea un Flow<PagingData<Movie>> estático
 * sin pasar por PagingSource real — perfecto para testear solo el Adapter
 * y el comportamiento visual, sin la complejidad de paginación real.
 */
@HiltAndroidTest
@UninstallModules(GenresModule::class)
@RunWith(AndroidJUnit4::class)
class MoviesByGenrePagingUiTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @BindValue @JvmField
    val genresRepository: GenresRepository = mockk(relaxed = true)

    @BindValue @JvmField
    val moviesByGenreRepository: MoviesByGenreRepository = mockk(relaxed = true)

    private val testGenres = listOf(Genre(id = 28, name = "Acción"))

    @Before
    fun setUp() {
        hiltRule.inject()
        // Géneros siempre disponibles — necesarios para que el ViewModel
        // dispare _selectedGenreId y active flatMapLatest sobre movies
        coEvery { genresRepository.getGenres() } returns Resource.Success(testGenres)
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun launchFragment() {
        launchFragmentInHiltContainer<GenresFragment>()
        // Barrera de sincronización semántica — chips visibles significa
        // que el ViewModel está activo y submitData() fue llamado
        waitForView(withId(R.id.scrollViewChips))
    }

    private fun launchFragmentWithNavController(): TestNavHostController {
        val navController = TestNavHostController(
            ApplicationProvider.getApplicationContext()
        )

        launchFragmentInHiltContainer<GenresFragment>(
            navHostController = navController
        ) {
            // Configurar el grafo DENTRO del contexto del Fragment
            // porque TestNavHostController necesita el Main thread
            navController.setGraph(R.navigation.nav_graph)
            navController.setCurrentDestination(R.id.genresFragment)
        }

        return navController
    }

    // ── Tests de renderizado de items ─────────────────────────────────────────

    @Test
    fun whenMoviesLoad_recyclerViewShowsItems() {
        // Given — PagingData.from() alimenta el adapter directamente
        // sin pasar por PagingSource, ideal para testear solo el Adapter
        every { moviesByGenreRepository.getMoviesByGenre(any(), any()) } returns
                flowOf(PagingData.from(MovieFactory.createMovieList(5)))

        // When
        launchFragment()

        // Then
        onView(withId(R.id.recyclerViewMovies)).check(matches(isDisplayed()))
        onView(withId(R.id.progressBarMovies)).check(matches(not(isDisplayed())))
    }

    @Test
    fun whenMoviesLoad_itemCountMatchesData() {
        // Given — 3 películas, sin footer activo (PagingData.from usa
        // append=NotLoading por defecto → MoviesLoadStateAdapter no agrega item)
        every { moviesByGenreRepository.getMoviesByGenre(any(), any()) } returns
                flowOf(PagingData.from(MovieFactory.createMovieList(3)))

        // When
        launchFragment()

        // Esperar determinísticamente a que el RecyclerView sea visible
        // antes de verificar el conteo — onPagesUpdatedFlow garantiza
        // que itemCount ya está actualizado cuando la View se hace VISIBLE
        waitForView(withId(R.id.recyclerViewMovies))

        // Then — exactamente 3 items, no "al menos 3"
        // El footer no aparece porque append=NotLoading(endOfPaginationReached=true)
        onView(withId(R.id.recyclerViewMovies)).check { view, _ ->
            val recyclerView = view as RecyclerView
            assertThat(recyclerView.adapter?.itemCount).isEqualTo(3)
        }
    }

    @Test
    fun whenMovieItemClicked_navigatesToDetails() {
        // Given — una película para hacer click en el primer item
        val movies = listOf(MovieFactory.createMovie(id = 99, title = "Test Movie"))
        every { moviesByGenreRepository.getMoviesByGenre(any(), any()) } returns
                flowOf(PagingData.from(movies))

        // When
        val navController = launchFragmentWithNavController()

        // Esperar determinísticamente a que el RecyclerView tenga items
        waitForView(withId(R.id.recyclerViewMovies))

        // Click en el primer item
        onView(withId(R.id.recyclerViewMovies)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click())
        )

        // Then — verificar que el NavController navegó al destino correcto
        assertThat(navController.currentDestination?.id)
            .isEqualTo(R.id.movieDetailsFragment)

    }

    // ── Tests de estado vacío ─────────────────────────────────────────────────

    @Test
    fun whenNoMoviesAvailable_recyclerViewIsNotDisplayed() {
        // Given
        every { moviesByGenreRepository.getMoviesByGenre(any(), any()) } returns
                flowOf(PagingData.from(emptyList()))

        // When
        launchFragment()

        // Primero verificamos que el Fragment terminó de procesar
        // (los chips son visibles → géneros cargaron → ViewModel activo)
        onView(withId(R.id.scrollViewChips)).check(matches(isDisplayed()))

        // Then — con itemCount=0, recyclerViewMovies debe estar GONE
        onView(withId(R.id.recyclerViewMovies))
            .check(matches(not(isDisplayed())))

        // Y ningún estado de error está visible — la lista vacía no es un error
        onView(withId(R.id.layoutError))
            .check(matches(not(isDisplayed())))
    }

    // ── Tests del LoadStateAdapter (footer) ────────────────────────────────────

    @Test
    fun whenLoadingMorePages_footerShowsProgressBar() {
        every { moviesByGenreRepository.getMoviesByGenre(any(), any()) } returns flow {
            emit(
                PagingData.from(
                    data = MovieFactory.createMovieList(3),
                    sourceLoadStates = LoadStates(
                        refresh = LoadState.NotLoading(endOfPaginationReached = false),
                        prepend = LoadState.NotLoading(endOfPaginationReached = true),
                        append = LoadState.Loading
                    )
                )
            )
        }

        launchFragment()
        waitForView(withId(R.id.recyclerViewMovies))

        // itemCount exacto: 3 películas + 1 footer de loading
        onView(withId(R.id.recyclerViewMovies)).check { view, _ ->
            assertThat((view as RecyclerView).adapter?.itemCount).isEqualTo(4)
        }

        // progressBar del footer visible — restringido al RecyclerView
        // para evitar AmbiguousViewMatcherException con progressBarMovies/progressBarGenres
        onView(
            allOf(
                withId(R.id.progressBar),
                isDescendantOfA(withId(R.id.recyclerViewMovies))
            )
        ).check(matches(isDisplayed()))

        // btnRetry del footer NO visible — restringido al RecyclerView
        // para evitar conflicto con btnRetry del layoutError principal
        onView(
            allOf(
                withId(R.id.btnRetry),
                isDescendantOfA(withId(R.id.recyclerViewMovies))
            )
        ).check(matches(not(isDisplayed())))
    }

    @Test
    fun whenAppendFails_footerShowsRetryButton() {
        // Given — 3 películas con append=Error simulando fallo al cargar página siguiente
        every { moviesByGenreRepository.getMoviesByGenre(any(), any()) } returns flow {
            emit(
                PagingData.from(
                    data = MovieFactory.createMovieList(3),
                    sourceLoadStates = LoadStates(
                        refresh = LoadState.NotLoading(endOfPaginationReached = false),
                        prepend = LoadState.NotLoading(endOfPaginationReached = true),
                        append = LoadState.Error(RuntimeException("Network error"))
                    )
                )
            )
        }

        // When
        launchFragment()
        waitForView(withId(R.id.recyclerViewMovies))

        // Then 1 — itemCount exacto: 3 películas + 1 footer de error
        onView(withId(R.id.recyclerViewMovies)).check { view, _ ->
            assertThat((view as RecyclerView).adapter?.itemCount).isEqualTo(4)
        }

        // Then 2 — btnRetry del footer visible — append=Error activa el retry
        // isDescendantOfA evita conflicto con btnRetry del layoutError principal
        onView(
            allOf(
                withId(R.id.btnRetry),
                isDescendantOfA(withId(R.id.recyclerViewMovies))
            )
        ).check(matches(isDisplayed()))

        // Then 3 — progressBar del footer NO visible — es Error, no Loading
        // Este Then distingue este test del anterior (Loading vs Error)
        onView(
            allOf(
                withId(R.id.progressBar),
                isDescendantOfA(withId(R.id.recyclerViewMovies))
            )
        ).check(matches(not(isDisplayed())))

        // Then 4 — layoutError del Fragment principal NO visible
        // El error de append afecta solo al footer, no al layout de error global
        onView(withId(R.id.layoutError))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun whenInitialLoadFails_errorLayoutIsVisible() {
        // Given — refresh=Error simula fallo en la carga inicial
        // data=emptyList porque con Error no hay películas que mostrar
        every { moviesByGenreRepository.getMoviesByGenre(any(), any()) } returns flow {
            emit(
                PagingData.from(
                    data = emptyList(),
                    sourceLoadStates = LoadStates(
                        refresh = LoadState.Error(RuntimeException("Connection failed")),
                        prepend = LoadState.NotLoading(endOfPaginationReached = true),
                        append = LoadState.NotLoading(endOfPaginationReached = true)
                    )
                )
            )
        }

        // When
        launchFragment()

        // Then 1 — layoutError visible por refresh=Error
        onView(withId(R.id.layoutError)).check(matches(isDisplayed()))

        // Then 2 — recyclerViewMovies GONE — no hay películas que mostrar
        onView(withId(R.id.recyclerViewMovies))
            .check(matches(not(isDisplayed())))

        // Then 3 — progressBarMovies GONE — la carga terminó (con error)
        onView(withId(R.id.progressBarMovies))
            .check(matches(not(isDisplayed())))

        // Then 4 — btnRetry del layoutError principal visible
        // (distinto del btnRetry del footer — este es el del Fragment)
        onView(
            allOf(
                withId(R.id.btnRetry),
                not(isDescendantOfA(withId(R.id.recyclerViewMovies)))
            )
        ).check(matches(isDisplayed()))
    }

    @Test
    fun whenRetryButtonClicked_layoutErrorDisappearsAndMoviesLoad() {
        val pagingFlow = MutableSharedFlow<PagingData<Movie>>(replay = 1)

        every { moviesByGenreRepository.getMoviesByGenre(any(), any()) } returns pagingFlow

        val errorPagingData = PagingData.from(
            data = emptyList<Movie>(),
            sourceLoadStates = LoadStates(
                refresh = LoadState.Error(RuntimeException("Connection failed")),
                prepend = LoadState.NotLoading(endOfPaginationReached = true),
                append = LoadState.NotLoading(endOfPaginationReached = true)
            )
        )

        val successPagingData = PagingData.from(
            data = MovieFactory.createMovieList(3),
            sourceLoadStates = LoadStates(
                refresh = LoadState.NotLoading(endOfPaginationReached = false),
                prepend = LoadState.NotLoading(endOfPaginationReached = true),
                append = LoadState.NotLoading(endOfPaginationReached = true)
            )
        )

        // 1. Emitir error ANTES de lanzar el Fragment
        pagingFlow.tryEmit(errorPagingData)

        // 2. Lanzar Fragment — verá el error inmediatamente
        launchFragment()
        onView(withId(R.id.scrollViewChips)).check(matches(isDisplayed()))
        onView(withId(R.id.layoutError)).check(matches(isDisplayed()))

        // 3. Click en btnRetry — ANTES de emitir éxito
        onView(
            allOf(
                withId(R.id.btnRetry),
                not(isDescendantOfA(withId(R.id.recyclerViewMovies)))
            )
        ).perform(click())

        // 4. Emitir éxito DESPUÉS del click — simula que el retry trajo datos
        pagingFlow.tryEmit(successPagingData)

        // 5. Then — películas visibles, layoutError oculto
        waitForView(withId(R.id.recyclerViewMovies))
        onView(withId(R.id.layoutError)).check(matches(not(isDisplayed())))
    }
}
