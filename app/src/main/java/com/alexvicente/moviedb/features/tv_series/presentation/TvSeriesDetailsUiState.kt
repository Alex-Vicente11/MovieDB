package com.alexvicente.moviedb.features.tv_series.presentation

import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesDetails

sealed class TvSeriesDetailsUiState {
    object Loading: TvSeriesDetailsUiState()
    data class Success(val details: TvSeriesDetails) : TvSeriesDetailsUiState()
    data class Error(val message: String) : TvSeriesDetailsUiState()
}