package com.alexvicente.moviedb.features.favorites

import androidx.navigation.testing.TestNavHostController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.favorites.di.FavoritesModule
import com.alexvicente.moviedb.features.favorites.domain.model.Favorite
import com.alexvicente.moviedb.features.favorites.domain.repository.FavoritesRepository
import com.alexvicente.moviedb.features.favorites.presentation.FavoritesFragment
import com.alexvicente.moviedb.util.favorites.clickChildViewWithId
import com.alexvicente.moviedb.util.waitForView
import com.alexvicente.moviedb.util.launchFragmentInHiltContainer
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(FavoritesModule::class)
@RunWith(AndroidJUnit4::class)
class FavoritesFragmentTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @BindValue @JvmField
    val favoritesRepository: FavoritesRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun createFavorite(
        id: Int = 1,
        title: String = "Inception"
    ) = Favorite(
        id = id,
        title = title,
        posterPath = "/poster.jpg",
        voteAverage = 8.8,
        releaseDate = "2010-07-16",
        overview = "A thief who steals corporate secrets",
        addedAt = System.currentTimeMillis()
    )

    private fun launchWithFavorites(favorites: List<Favorite>) {
        every { favoritesRepository.getAllFavoritesRepo() } returns flowOf(favorites)
        every { favoritesRepository.isFavoriteRepo(any()) } returns flowOf(false)
        launchFragmentInHiltContainer<FavoritesFragment>()
        // Thread.sleep(300) → reemplazado por barrera semántica
        if (favorites.isNotEmpty()) {
            waitForView(withId(R.id.recyclerViewFavorites))
        } else {
            waitForView(withId(R.id.emptyState))
        }
    }

    // ── Tests de estado Loading ───────────────────────────────────────────────

    @Test
    fun whenFragmentLaunched_initialStateShowsLoading() {
        // Given — repositorio que nunca emite
        every { favoritesRepository.getAllFavoritesRepo() } returns flow { awaitCancellation() }
        every { favoritesRepository.isFavoriteRepo(any()) } returns flowOf(false)

        // When
        launchFragmentInHiltContainer<FavoritesFragment>()

        // Then
        onView(withId(R.id.progressBar)).check(matches(isDisplayed()))
        onView(withId(R.id.recyclerViewFavorites)).check(matches(not(isDisplayed())))
        onView(withId(R.id.emptyState)).check(matches(not(isDisplayed())))
    }

    // ── Tests de estado Empty ─────────────────────────────────────────────────

    @Test
    fun whenNoFavorites_emptyStateIsVisible() {
        // Given
        launchWithFavorites(emptyList())

        // Then
        onView(withId(R.id.emptyState)).check(matches(isDisplayed()))
        onView(withId(R.id.recyclerViewFavorites)).check(matches(not(isDisplayed())))
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    // ── Tests de estado Success ───────────────────────────────────────────────

    @Test
    fun whenFavoritesExist_recyclerViewIsVisible() {
        // Given
        launchWithFavorites(listOf(createFavorite(1), createFavorite(2)))

        // Then
        onView(withId(R.id.recyclerViewFavorites)).check(matches(isDisplayed()))
        onView(withId(R.id.emptyState)).check(matches(not(isDisplayed())))
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun whenFavoritesExist_correctItemCountIsRendered() {
        // Given
        launchWithFavorites(listOf(
            createFavorite(1, "Inception"),
            createFavorite(2, "Interstellar"),
            createFavorite(3, "The Dark Knight")
        ))

        // Then — verificamos el itemCount real del adapter
        onView(withId(R.id.recyclerViewFavorites)).check { view, _ ->
            val rv = view as RecyclerView
            assertThat(rv.adapter?.itemCount).isEqualTo(3)
        }
    }

    @Test
    fun whenFavoritesExist_titlesAreDisplayedCorrectly() {
        // Given
        launchWithFavorites(listOf(
            createFavorite(1, "Inception"),
            createFavorite(2, "Interstellar")
        ))

        // Then — títulos visibles dentro del RecyclerView específicamente
        // isDescendantOfA evita AmbiguousViewMatcherException si el mismo
        // texto aparece en otro lugar de la jerarquía (Toolbar, otro Fragment)
        onView(
            allOf(
                withText("Inception"),
                isDescendantOfA(withId(R.id.recyclerViewFavorites))
            )
        ).check(matches(isDisplayed()))

        onView(
            allOf(
                withText("Interstellar"),
                isDescendantOfA(withId(R.id.recyclerViewFavorites))
            )
        ).check(matches(isDisplayed()))
    }

    // ── Tests de interacción ──────────────────────────────────────────────────

    @Test
    fun whenFavoriteItemClicked_navigatesToMovieDetails() {
        // Given
        val navController = TestNavHostController(
            ApplicationProvider.getApplicationContext()
        )

        val favoritesFlow = MutableSharedFlow<List<Favorite>>(replay = 1)
        every { favoritesRepository.getAllFavoritesRepo() } returns favoritesFlow
        every { favoritesRepository.isFavoriteRepo(any()) } returns flowOf(false)
        favoritesFlow.tryEmit(listOf(createFavorite(id = 99, title = "Inception")))

        launchFragmentInHiltContainer<FavoritesFragment>(
            navHostController = navController
        ) {
            navController.setGraph(R.navigation.nav_graph)
            navController.setCurrentDestination(R.id.favoritesFragment)
        }

        waitForView(withId(R.id.recyclerViewFavorites))

        // When — click en el primer item
        onView(withId(R.id.recyclerViewFavorites)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, click()
            )
        )

        // Then — NavController navegó al destino correcto con el argumento correcto
        assertThat(navController.currentDestination?.id)
            .isEqualTo(R.id.movieDetailsFragment)

        // Verificar también el argumento movieId
        assertThat(navController.backStack.last().arguments?.getInt("movieId"))
            .isEqualTo(99)
    }

    @Test
    fun whenRemoveButtonClicked_favoritesListBecomesEmpty() {
        val favoritesFlow = MutableSharedFlow<List<Favorite>>(replay = 1)

        every { favoritesRepository.getAllFavoritesRepo() } returns favoritesFlow
        every { favoritesRepository.isFavoriteRepo(any()) } returns flowOf(false)

        coEvery { favoritesRepository.removeFavoriteRepo(42) } coAnswers {
            favoritesFlow.tryEmit(emptyList())
            Resource.Success(Unit)
        }

        // tryEmit para la emisión inicial también — no necesita corrutina
        favoritesFlow.tryEmit(listOf(createFavorite(id = 42, title = "Inception")))

        launchFragmentInHiltContainer<FavoritesFragment>()
        waitForView(withId(R.id.recyclerViewFavorites))

        onView(withId(R.id.recyclerViewFavorites)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, clickChildViewWithId(R.id.btnRemoveFavorite)
            )
        )

        waitForView(withId(R.id.emptyState))
        onView(withId(R.id.emptyState)).check(matches(isDisplayed()))
        onView(withId(R.id.recyclerViewFavorites)).check(matches(not(isDisplayed())))
    }
}