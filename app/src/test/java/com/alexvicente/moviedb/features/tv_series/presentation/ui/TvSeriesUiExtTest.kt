package com.alexvicente.moviedb.features.tv_series.presentation.ui

import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesStatus
import com.alexvicente.moviedb.testutil.factories.TvSeriesFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TvSeriesUiExtTest {

    private fun season(number: Int) =
        TvSeriesFactory.createTvSeasonSummary(id = number + 100, seasonNumber = number)

    @Test
    fun specialLast_movesSeasonZeroToTheEnd_keepingTheRestInOrder() {
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
}