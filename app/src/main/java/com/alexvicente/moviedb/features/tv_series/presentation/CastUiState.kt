package com.alexvicente.moviedb.features.tv_series.presentation

import com.alexvicente.moviedb.features.tv_series.domain.model.CastMember

sealed class CastUiState {
    object Loading: CastUiState()
    data class Success(val members: List<CastMember>) : CastUiState()
    data class Error(val message: String) : CastUiState()
}