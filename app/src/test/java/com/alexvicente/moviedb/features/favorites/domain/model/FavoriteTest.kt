package com.alexvicente.moviedb.features.favorites.domain.model

import com.alexvicente.moviedb.testutil.factories.FavoriteFactory
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

class FavoriteTest {

    private lateinit var originalLocale: Locale

    @Before
    fun saveLocale() {
        originalLocale = Locale.getDefault()
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun whenPosterPathExists_posterUrlUsesW500() {
        assertThat(FavoriteFactory.createFavorite(posterPath = "/poster.jpg").getPosterUrl())
            .isEqualTo("https://image.tmdb.org/t/p/w500/poster.jpg")
    }

    // Caracterización (decisión pendiente): Favorite devuelve "N/A" y Movie/MovieDetails devuelven "".
    // loadUrl() solo usa el placeholder directo si la URL está vacía, así que "N/A" dispara una carga fallida.
    @Test
    fun whenPosterPathIsNull_posterUrlIsNotAvailableMarker() {
        assertThat(FavoriteFactory.createFavorite(posterPath = null).getPosterUrl()).isEqualTo("N/A")
    }

    @Test
    fun whenReleaseDateIsFull_releaseYearIsTheYear() {
        assertThat(FavoriteFactory.createFavorite(releaseDate = "2010-07-16").getReleaseYear())
            .isEqualTo("2010")
    }

    // ROJO con el código actual: "".split("-") devuelve [""] y nunca llega a "N/A".
    @Test
    fun whenReleaseDateIsEmpty_releaseYearIsNotAvailable() {
        assertThat(FavoriteFactory.createFavorite(releaseDate = "").getReleaseYear()).isEqualTo("N/A")
    }

    @Test
    fun whenVoteAverageHasManyDecimals_ratingIsRoundedToOneDecimal() {
        assertThat(FavoriteFactory.createFavorite(voteAverage = 8.76).getFormattedRating())
            .isEqualTo("⭐ 8.8/10")
    }

    @Test
    fun whenVoteAverageIsZero_ratingShowsZero() {
        assertThat(FavoriteFactory.createFavorite(voteAverage = 0.0).getFormattedRating())
            .isEqualTo("⭐ 0.0/10")
    }

    // ROJO con el código actual: con Locale alemán String.format produce "8,8".
    @Test
    fun whenDeviceUsesCommaDecimal_ratingStillUsesDot() {
        Locale.setDefault(Locale.GERMANY)

        assertThat(FavoriteFactory.createFavorite(voteAverage = 8.8).getFormattedRating())
            .isEqualTo("⭐ 8.8/10")
    }
}