package com.example.apptest.features.popular_movies.presentation

import com.example.apptest.core.domain.model.Movie


sealed class MainUiState {

    object Idle : MainUiState()

    object Loading : MainUiState()

    data class Success(val movies: List<Movie>) : MainUiState()

    data class Error(val message: String) : MainUiState()

    object Empty : MainUiState()
}