package com.alexvicente.moviedb.features.genres.presentation

import com.alexvicente.moviedb.core.domain.model.Genre

sealed class GenresUiState {
    object Loading : GenresUiState()
    data class Success(val genres: List<Genre>) : GenresUiState()
    data class Error(val message: String) : GenresUiState()
}