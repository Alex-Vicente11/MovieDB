package com.alexvicente.moviedb.features.movie_details.domain.model

import com.alexvicente.moviedb.testutil.factories.MovieDetailsFactory
import com.alexvicente.moviedb.testutil.factories.MovieDetailsFactory.createMovieDetails
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

class MovieDetailsTest {

    // String.format sin Locale usa el idioma del dispositivo: se guarda y se restaura
    private lateinit var originalLocale: Locale

    @Before
    fun saveLocale() {
        originalLocale = Locale.getDefault()
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    // -- URLs de imagen --------------------------------------------------------------

    @Test
    fun whenPosterPathExists_posterUrlUsesW500() {
        assertThat(createMovieDetails(posterPath = "/poster.jpg").getPosterUrl())
            .isEqualTo("https://image.tmdb.org/t/p/w500/poster.jpg")
    }

    @Test
    fun whenPosterPathIsNull_posterUrlIsEmpty() {
        assertThat(createMovieDetails(posterPath = null).getPosterUrl()).isEmpty()
    }

    @Test
    fun whenBackdropPathExists_backdropUrlUsesW780() {
        assertThat(createMovieDetails(backdropPath = "/backdrop.jpg").getBackdropUrl())
            .isEqualTo("https://image.tmdb.org/t/p/w780/backdrop.jpg")
    }

    @Test
    fun whenBackdropPathIsNull_backdropUrlIsEmpty() {
        assertThat(createMovieDetails(backdropPath = null).getBackdropUrl()).isEmpty()
    }

    // -- getReleaseYear() --------------------------------------------------------------

    @Test
    fun whenReleaseDateIsFull_releaseYearIsTheYear() {
        assertThat(createMovieDetails(releaseDate = "2010-07-16").getReleaseYear()).isEqualTo("2010")
    }

    @Test
    fun whenReleaseDateHasNoDash_releaseYearIsTheWholeValue() {
        assertThat(createMovieDetails(releaseDate = "2010").getReleaseYear()).isEqualTo("2010")
    }

    // ROJO con el código actual: "".split("-") devuelve [""] y nunca llega a "N/A".
    @Test
    fun whenReleaseDateIsEmpty_releaseYearIsNotAvailable() {
        assertThat(createMovieDetails(releaseDate = "").getReleaseYear()).isEqualTo("N/A")
    }

    // -- getFormattedRating() ----------------------------------------------------------

    @Test
    fun whenVoteAverageHasManyDecimals_ratingIsRoundedToOneDecimal() {
        assertThat(createMovieDetails(voteAverage = 8.76).getFormattedRating()).isEqualTo("⭐ 8.8/10")
    }

    @Test
    fun whenVoteAverageIsZero_ratingShowsZero() {
        assertThat(createMovieDetails(voteAverage = 0.0).getFormattedRating()).isEqualTo("⭐ 0.0/10")
    }

    // ROJO con el código actual: con Locale alemán String.format produce "8,8".
    @Test
    fun whenDeviceUsesCommaDecimal_ratingStillUsesDot() {
        Locale.setDefault(Locale.GERMANY)

        assertThat(createMovieDetails(voteAverage = 8.8).getFormattedRating()).isEqualTo("⭐ 8.8/10")
    }

    // -- getFormattedPopularity() ------------------------------------------------------

    @Test
    fun whenPopularityHasManyDecimals_popularityIsRoundedToOneDecimal() {
        assertThat(createMovieDetails(popularity = 123.456).getFormattedPopularity())
            .isEqualTo("🔥 123.5")
    }

    @Test
    fun whenPopularityIsZero_popularityShowsZero() {
        assertThat(createMovieDetails(popularity = 0.0).getFormattedPopularity())
            .isEqualTo("🔥 0.0")
    }

    // Con Locale alemán String.format sin Locale produce "123,5".
    @Test
    fun whenDeviceUsesCommaDecimal_popularityStillUsesDot() {
        Locale.setDefault(Locale.GERMANY)

        assertThat(createMovieDetails(popularity = 123.456).getFormattedPopularity())
            .isEqualTo("🔥 123.5")
    }

    // -- getFormattedRuntime() ---------------------------------------------------------

    @Test
    fun whenRuntimeIsNull_runtimeIsNotAvailable() {
        assertThat(createMovieDetails(runtime = null).getFormattedRuntime()).isEqualTo("N/A")
    }

    @Test
    fun whenRuntimeIsZero_runtimeIsNotAvailable() {
        assertThat(createMovieDetails(runtime = 0).getFormattedRuntime()).isEqualTo("N/A")
    }

    @Test
    fun whenRuntimeIsNegative_runtimeIsNotAvailable() {
        assertThat(createMovieDetails(runtime = -10).getFormattedRuntime()).isEqualTo("N/A")
    }

    @Test
    fun whenRuntimeIsUnderOneHour_runtimeShowsOnlyMinutes() {
        assertThat(createMovieDetails(runtime = 45).getFormattedRuntime()).isEqualTo("45min")
    }

    @Test
    fun whenRuntimeIsExactlyOneHour_runtimeShowsZeroMinutes() {
        assertThat(createMovieDetails(runtime = 60).getFormattedRuntime()).isEqualTo("1h 0min")
    }

    @Test
    fun whenRuntimeIsOverOneHour_runtimeShowsHoursAndMinutes() {
        assertThat(createMovieDetails(runtime = 148).getFormattedRuntime()).isEqualTo("2h 28min")
    }

    // -- getGenresString() -------------------------------------------------------------

    @Test
    fun whenThereAreNoGenres_genresStringIsEmpty() {
        assertThat(createMovieDetails(genres = emptyList()).getGenresString()).isEmpty()
    }

    @Test
    fun whenThereIsOneGenre_genresStringIsItsName() {
        val details = createMovieDetails(genres = listOf(MovieDetailsFactory.createGenre(28, "Action")))

        assertThat(details.getGenresString()).isEqualTo("Action")
    }

    @Test
    fun whenThereAreSeveralGenres_genresStringJoinsWithCommaAndSpace() {
        val details = createMovieDetails(
            genres = listOf(
                MovieDetailsFactory.createGenre(28, "Action"),
                MovieDetailsFactory.createGenre(878, "Science Fiction")
            )
        )

        assertThat(details.getGenresString()).isEqualTo("Action, Science Fiction")
    }

    // -- getFormattedBudget() ----------------------------------------------------------

    @Test
    fun whenBudgetIsNull_budgetIsNotAvailable() {
        assertThat(createMovieDetails(budget = null).getFormattedBudget()).isEqualTo("N/A")
    }

    @Test
    fun whenBudgetIsZero_budgetIsNotAvailable() {
        assertThat(createMovieDetails(budget = 0L).getFormattedBudget()).isEqualTo("N/A")
    }

    @Test
    fun whenBudgetIsInMillions_budgetShowsOneDecimalAndM() {
        assertThat(createMovieDetails(budget = 160_000_000L).getFormattedBudget()).isEqualTo("\$160.0M")
    }

    @Test
    fun whenBudgetIsBelowOneMillion_budgetShowsFractionOfMillion() {
        assertThat(createMovieDetails(budget = 500_000L).getFormattedBudget()).isEqualTo("\$0.5M")
    }

    // ROJO con el código actual: con Locale alemán sale "$160,0M".
    @Test
    fun whenDeviceUsesCommaDecimal_budgetStillUsesDot() {
        Locale.setDefault(Locale.GERMANY)

        assertThat(createMovieDetails(budget = 160_000_000L).getFormattedBudget()).isEqualTo("\$160.0M")
    }

    // -- getFormattedRevenue() ---------------------------------------------------------

    @Test
    fun whenRevenueIsNull_revenueIsNotAvailable() {
        assertThat(createMovieDetails(revenue = null).getFormattedRevenue()).isEqualTo("N/A")
    }

    @Test
    fun whenRevenueIsZero_revenueIsNotAvailable() {
        assertThat(createMovieDetails(revenue = 0L).getFormattedRevenue()).isEqualTo("N/A")
    }

    @Test
    fun whenRevenueIsInMillions_revenueShowsOneDecimalAndM() {
        assertThat(createMovieDetails(revenue = 829_895_144L).getFormattedRevenue()).isEqualTo("\$829.9M")
    }

    // ROJO con el código actual: con Locale alemán sale "$829,9M".
    @Test
    fun whenDeviceUsesCommaDecimal_revenueStillUsesDot() {
        Locale.setDefault(Locale.GERMANY)

        assertThat(createMovieDetails(revenue = 829_895_144L).getFormattedRevenue()).isEqualTo("\$829.9M")
    }
}