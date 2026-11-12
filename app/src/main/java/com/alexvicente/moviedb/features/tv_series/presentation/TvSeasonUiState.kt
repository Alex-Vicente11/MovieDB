package com.alexvicente.moviedb.features.tv_series.presentation

import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeasonDetails

sealed class TvSeasonUiState {
    object Loading: TvSeasonUiState()
    data class Success(val season: TvSeasonDetails) : TvSeasonUiState()
    data class Error(val message: String) : TvSeasonUiState()
}