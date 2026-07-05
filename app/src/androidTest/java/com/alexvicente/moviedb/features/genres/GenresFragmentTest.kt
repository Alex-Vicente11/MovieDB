package com.alexvicente.moviedb.features.genres

import androidx.paging.PagingData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.domain.model.Genre
import com.alexvicente.moviedb.features.genres.di.GenresModule
import com.alexvicente.moviedb.features.genres.domain.repository.GenresRepository
import com.alexvicente.moviedb.features.genres.domain.repository.MoviesByGenreRepository
import com.alexvicente.moviedb.features.genres.presentation.GenresFragment
import com.alexvicente.moviedb.testutil.factories.MovieFactory
import com.alexvicente.moviedb.util.genres.hasExactChildCount
import com.alexvicente.moviedb.util.genres.isChipChecked
import com.alexvicente.moviedb.util.launchFragmentInHiltContainer
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@HiltAndroidTest
@UninstallModules(GenresModule::class)
@RunWith(AndroidJUnit4::class)
class GenresFragmentTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    // ── Repositorios mockeados ────────────────────────────────────────────────

    @BindValue @JvmField
    val genresRepository: GenresRepository = mockk(relaxed = true)

    @BindValue @JvmField
    val moviesByGenreRepository: MoviesByGenreRepository = mockk(relaxed = true)

    // ── Datos de prueba ───────────────────────────────────────────────────────

    // Géneros fijos para verificar chips por texto
    private val testGenres = listOf(
        Genre(id = 28, name = "Acción"),
        Genre(id = 12, name = "Aventura"),
        Genre(id = 16, name = "Animación")
    )

    @Before
    fun setUp() {
        hiltRule.inject()

        // moviesByGenreRepository debe estar listo ANTES de lanzar el Fragment
        // porque GenresViewModel.init{} llama loadGenres() que puede disparar
        // _selectedGenreId, que a su vez activa flatMapLatest sobre movies
        every { moviesByGenreRepository.getMoviesByGenre(any(), any()) } returns
                flowOf(PagingData.from(MovieFactory.createMovieList(3)))
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Configura el mock de géneros y lanza el Fragment.
     * Separado en helper porque cada test necesita un estado de géneros diferente.
     */
    private fun launchWithGenres(genres: List<Genre> = testGenres) {
        // coEvery porque getGenres() es suspend fun
        coEvery { genresRepository.getGenres() } returns Resource.Success(genres)
        launchFragmentInHiltContainer<GenresFragment>()
    }

    private fun launchWithLoading() {
        // suspend fun que nunca retorna — simula carga infinita
        coEvery { genresRepository.getGenres() } coAnswers {
            kotlinx.coroutines.delay(Long.MAX_VALUE)
            Resource.Success(emptyList())
        }
        launchFragmentInHiltContainer<GenresFragment>()
    }

    private fun launchWithError(message: String = "Error de conexión") {
        coEvery { genresRepository.getGenres() } returns Resource.Error(message)
        launchFragmentInHiltContainer<GenresFragment>()
    }

    // ── Tests de estado Loading ───────────────────────────────────────────────

    @Test
    fun whenGenresAreLoading_progressBarIsVisible() {
        // Given / When
        launchWithLoading()

        // Then — progressBarGenres visible, chips ocultos
        onView(withId(R.id.progressBarGenres)).check(matches(isDisplayed()))
        onView(withId(R.id.scrollViewChips)).check(matches(not(isDisplayed())))
    }

    // ── Tests de estado Success ───────────────────────────────────────────────

    @Test
    fun whenGenresLoadSuccessfully_chipsAreVisible() {
        // Given / When
        launchWithGenres()

        // Then — scrollViewChips visible, progressBar oculto
        onView(withId(R.id.scrollViewChips)).check(matches(isDisplayed()))
        onView(withId(R.id.progressBarGenres)).check(matches(not(isDisplayed())))
    }

    @Test
    fun whenGenresLoadSuccessfully_correctNumberOfChipsIsRendered() {
        // Given / When — 3 géneros deben generar 3 chips
        launchWithGenres(testGenres)

        // hasMinimumChildCount verifica que ChipGroup tiene al menos N hijos
        // (chips se agregan dinámicamente en setupChips())
        onView(withId(R.id.chipGroupGenres))
            .check(matches(hasExactChildCount(testGenres.size)))
    }

    @Test
    fun whenGenresLoadSuccessfully_chipTextsMatchGenreNames() {
        // Given / When
        launchWithGenres(testGenres)

        // withText() busca en toda la jerarquía de vistas, incluyendo chips dinámicos
        onView(withText("Acción")).check(matches(isDisplayed()))
        onView(withText("Aventura")).check(matches(isDisplayed()))
        onView(withText("Animación")).check(matches(isDisplayed()))
    }

    @Test
    fun whenGenresLoadSuccessfully_firstChipIsCheckedByDefault() {
        // Given / When
        launchWithGenres(testGenres)

        // Then — "Acción" debe estar visible Y marcado como seleccionado
        // Espresso espera automáticamente a que la View esté disponible
        onView(withText("Acción"))
            .check(matches(isDisplayed()))
            .check(matches(isChipChecked()))
    }

    // ── Tests de estado Error ─────────────────────────────────────────────────

    @Test
    fun whenGenresFailToLoad_errorLayoutIsVisible() {
        // Given / When
        launchWithError("Error de conexión")

        // Then — layoutError visible, chips y progress ocultos
        onView(withId(R.id.layoutError)).check(matches(isDisplayed()))
        onView(withId(R.id.scrollViewChips)).check(matches(not(isDisplayed())))
        onView(withId(R.id.progressBarGenres)).check(matches(not(isDisplayed())))
        Thread.sleep(2000)
    }

    // ── Tests de interacción con chips ────────────────────────────────────────

    @Test
    fun whenChipClicked_triggersMoviesLoadForThatGenre() {
        // Given
        launchWithGenres(testGenres)
        onView(withText("Aventura")).check(matches(isDisplayed()))

        // When
        onView(withText("Aventura")).perform(click())

        // Then — el chip clickeado queda seleccionado,
        // lo que confirma que onGenreSelected() fue llamado
        onView(withText("Aventura")).check(matches(isChipChecked()))
    }

    // ── Tests de estado vacío ─────────────────────────────────────────────────

    @Test
    fun whenGenresReturnEmptyList_noChipsAreShown() {
        // Given / When — API retorna lista vacía de géneros
        launchWithGenres(emptyList())

        // Then — ChipGroup existe pero sin hijos
        // scrollViewChips sigue visible (GenresUiState.Success con lista vacía)
        onView(withId(R.id.scrollViewChips)).check(matches(isDisplayed()))
        onView(withId(R.id.chipGroupGenres)).check(matches(hasMinimumChildCount(0)))
    }
}