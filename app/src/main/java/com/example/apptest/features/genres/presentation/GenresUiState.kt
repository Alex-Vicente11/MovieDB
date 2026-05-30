package com.example.apptest.features.genres.presentation

import com.example.apptest.core.domain.model.Genre

sealed class GenresUiState {
    object Loading : GenresUiState()
    data class Success(val genres: List<Genre>) : GenresUiState()
    data class Error(val message: String) : GenresUiState()
}