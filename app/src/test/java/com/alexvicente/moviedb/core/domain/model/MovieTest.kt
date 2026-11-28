package com.alexvicente.moviedb.core.domain.model

import com.alexvicente.moviedb.testutil.factories.MovieFactory
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

class MovieTest {

    // String.format sin Locale usa el idioma del dispositivo: se guarda y se restaura
    // para que ningún test deje el Locale cambiado a los demás.
    private lateinit var originalLocale: Locale

    @Before
    fun saveLocale() {
        originalLocale = Locale.getDefault()
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    // -- getReleaseYear() --------------------------------------------------------------

    @Test
    fun whenReleaseDateIsFull_releaseYearIsTheYear() {
        assertThat(MovieFactory.createMovie(releaseDate = "2010-07-16").getReleaseYear())
            .isEqualTo("2010")
    }

    @Test
    fun whenReleaseDateHasNoDash_releaseYearIsTheWholeValue() {
        assertThat(MovieFactory.createMovie(releaseDate = "2010").getReleaseYear())
            .isEqualTo("2010")
    }

    // ROJO con el código actual: "".split("-") devuelve [""], firstOrNull() es "" y nunca llega a "N/A".
    @Test
    fun whenReleaseDateIsEmpty_releaseYearIsNotAvailable() {
        assertThat(MovieFactory.createMovie(releaseDate = "").getReleaseYear())
            .isEqualTo("N/A")
    }

    // -- getFormattedRating() ----------------------------------------------------------

    @Test
    fun whenVoteAverageHasManyDecimals_ratingIsRoundedToOneDecimal() {
        assertThat(MovieFactory.createMovie(voteAverage = 8.76).getFormattedRating())
            .isEqualTo("⭐ 8.8/10")
    }

    @Test
    fun whenVoteAverageIsZero_ratingShowsZero() {
        assertThat(MovieFactory.createMovie(voteAverage = 0.0).getFormattedRating())
            .isEqualTo("⭐ 0.0/10")
    }

    // ROJO con el código actual: con Locale alemán String.format produce "8,8".
    @Test
    fun whenDeviceUsesCommaDecimal_ratingStillUsesDot() {
        Locale.setDefault(Locale.GERMANY)

        assertThat(MovieFactory.createMovie(voteAverage = 8.8).getFormattedRating())
            .isEqualTo("⭐ 8.8/10")
    }

    // -- getPosterUrl() / getBackdropUrl() ---------------------------------------------

    @Test
    fun whenPosterPathExists_posterUrlUsesW500() {
        assertThat(MovieFactory.createMovie(posterPath = "/poster.jpg").getPosterUrl())
            .isEqualTo("https://image.tmdb.org/t/p/w500/poster.jpg")
    }

    @Test
    fun whenPosterPathIsNull_posterUrlIsEmpty() {
        assertThat(MovieFactory.createMovie(posterPath = null).getPosterUrl()).isEmpty()
    }

    @Test
    fun whenBackdropPathExists_backdropUrlUsesW780() {
        assertThat(MovieFactory.createMovie(backdropPath = "/backdrop.jpg").getBackdropUrl())
            .isEqualTo("https://image.tmdb.org/t/p/w780/backdrop.jpg")
    }

    @Test
    fun whenBackdropPathIsNull_backdropUrlIsEmpty() {
        assertThat(MovieFactory.createMovie(backdropPath = null).getBackdropUrl()).isEmpty()
    }

    // Caracterización (decisión pendiente): una ruta vacía NO es null, así que la URL queda
    // sin archivo y loadUrl() (que solo usa el placeholder directo si la URL está vacía)
    // intentaría descargarla. Pasa con posterPath = "" desde el mapper de Apollo.
    @Test
    fun whenPosterPathIsEmpty_posterUrlIsBaseUrlWithoutFile() {
        assertThat(MovieFactory.createMovie(posterPath = "").getPosterUrl())
            .isEqualTo("https://image.tmdb.org/t/p/w500")
    }
}