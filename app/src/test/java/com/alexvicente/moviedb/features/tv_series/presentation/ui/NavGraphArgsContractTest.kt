package com.alexvicente.moviedb.features.tv_series.presentation.ui

import android.app.Application
import com.alexvicente.moviedb.features.tv_series.presentation.TvSeasonViewModel
import com.alexvicente.moviedb.features.tv_series.presentation.TvSeriesDetailsViewModel
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NavGraphArgsContractTest {

    // Las claves de los ViewModels deben coincidir con los android:name de nav_graph.xml.
    // Si no, savedStateHandle[...] devuelve null y el ViewModel pide el id -1 sin avisar.
    // Safe Args genera los Args a partir del grafo: si el grafo cambia, estos tests lo detectan.
    // El valor por defecto de getInt es -1: una clave equivocada devuelve -1 y el test falla.

    @Test
    fun tvSeriesDetails_seriesIdKey_matchesViewModel() {
        val bundle = TvSeriesDetailsFragmentArgs(seriesId = 7).toBundle()

        assertThat(bundle.getInt(TvSeriesDetailsViewModel.ARG_SERIES_ID, -1)).isEqualTo(7)
    }

    @Test
    fun tvSeason_argumentKeys_matchViewModel() {
        val bundle = TvSeasonFragmentArgs(seriesId = 7, seasonNumber = 3).toBundle()

        assertThat(bundle.getInt(TvSeasonViewModel.ARG_SERIES_ID, -1)).isEqualTo(7)
        assertThat(bundle.getInt(TvSeasonViewModel.ARG_SEASON_NUMBER, -1)).isEqualTo(3)
    }
}