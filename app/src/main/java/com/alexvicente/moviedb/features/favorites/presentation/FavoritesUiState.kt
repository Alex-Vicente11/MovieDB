package com.alexvicente.moviedb.features.favorites.presentation

import com.alexvicente.moviedb.features.favorites.domain.model.Favorite

sealed class FavoritesUiState {
    object Loading: FavoritesUiState()
    object Empty: FavoritesUiState()
    data class Success(val favorites: List<Favorite>): FavoritesUiState()
}