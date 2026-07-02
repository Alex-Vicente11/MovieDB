package com.alexvicente.moviedb.features.popular_movies.presentation

import com.alexvicente.moviedb.core.domain.model.Movie

sealed class MainUiState {

    object Idle : MainUiState()

    object Loading : MainUiState()

    data class Success(val movies: List<Movie>) : MainUiState()

    data class Error(val message: String) : MainUiState()

    object Empty : MainUiState()
}