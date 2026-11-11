package com.alexvicente.moviedb.features.tv_series.presentation.ui

import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesStatus
import com.alexvicente.moviedb.testutil.factories.TvSeasonFactory
import com.alexvicente.moviedb.testutil.factories.TvSeriesFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class TvSeriesUiExtTest {

    private fun season(number: Int) =
        TvSeriesFactory.createTvSeasonSummary(id = number + 100, seasonNumber = number)

    // ── Temporadas ────────────────────────────────────────────────────────────

    @Test
    fun specialsLast_movesSeasonZeroToTheEnd_keepingTheRestInOrder() {
        val seasons = listOf(season(0), season(1), season(2), season(3))

        assertThat(seasons.specialsLast().map { it.seasonNumber })
            .containsExactly(1, 2, 3, 0).inOrder()
    }

    @Test
    fun specialsLast_withoutSpecials_keepsOrder() {
        val seasons = listOf(season(1), season(2))

        assertThat(seasons.specialsLast().map { it.seasonNumber })
            .containsExactly(1, 2).inOrder()
    }

    // ── Serie ─────────────────────────────────────────────────────────────────

    @Test
    fun statusLabel_isNullOnlyForUnknown() {
        assertThat(TvSeriesStatus.UNKNOWN.labelRes()).isNull()
        TvSeriesStatus.entries
            .filter { it != TvSeriesStatus.UNKNOWN }
            .forEach { assertThat(it.labelRes()).isNotNull() }
    }

    @Test
    fun firstAirYear_takesTheYear_orNullWhenThereIsNoDate() {
        val details = TvSeriesFactory.createTvSeriesDetails()

        assertThat(details.firstAirYear()).isEqualTo("2020")
        assertThat(details.copy(firstAirDate = null).firstAirYear()).isNull()
    }

    // ── Temporada (detalle) y episodios ───────────────────────────────────────

    @Test
    fun seasonAirYear_takesTheYear_orNullWhenThereIsNoDate() {
        val season = TvSeasonFactory.createTvSeasonDetails()

        assertThat(season.airYear()).isEqualTo("2020")
        assertThat(season.copy(airDate = null).airYear()).isNull()
    }

    @Test
    fun hasRating_dependsOnVoteCount_notOnVoteAverage() {
        val episode = TvSeasonFactory.createTvEpisode()

        // 0.0 de promedio con 0 votos significa «sin calificación», no una nota baja
        assertThat(episode.copy(voteCount = 0, voteAverage = 0.0).hasRating()).isFalse()
        assertThat(episode.copy(voteCount = 0, voteAverage = 8.0).hasRating()).isFalse()
        assertThat(episode.copy(voteCount = 5, voteAverage = 7.8).hasRating()).isTrue()
    }

    // ── Fechas ────────────────────────────────────────────────────────────────

    @Test
    fun formatAirDate_withNullBlankOrInvalidInput_returnsNull() {
        assertThat(formatAirDate(null)).isNull()
        assertThat(formatAirDate("")).isNull()
        assertThat(formatAirDate("   ")).isNull()
        assertThat(formatAirDate("no-es-una-fecha")).isNull()
    }

    @Test
    fun formatAirDate_withImpossibleDate_returnsNullInsteadOfRollingOver() {
        // Con un parser permisivo, el mes 13 se convertiría en enero del año siguiente
        assertThat(formatAirDate("2026-13-45")).isNull()
    }

    @Test
    fun formatAirDate_withValidDate_isLocalizedAndKeepsTheDay() {
        val formatted = formatAirDate("2026-11-05", Locale.US)

        assertThat(formatted).isNotNull()
        assertThat(formatted).isNotEqualTo("2026-11-05")
        assertThat(formatted).contains("2026")
        assertThat(formatted).contains("5")
    }

    @Test
    fun formatAirDate_usesTheContentLanguageByDefault_notTheDeviceLanguage() {
        // Si el idioma por defecto fuera el del dispositivo, el resultado coincidiría con el de otro Locale
        assertThat(formatAirDate("2026-11-05"))
            .isNotEqualTo(formatAirDate("2026-11-05", Locale.US))
    }
}
