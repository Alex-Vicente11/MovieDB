package com.alexvicente.moviedb.features.favorites.presentation

import com.alexvicente.moviedb.features.favorites.domain.model.Favorite

// Estado de UI
sealed class FavoritesUiState {
    object Loading: FavoritesUiState()
    object Empty: FavoritesUiState()
    data class Success(val favorites: List<Favorite>): FavoritesUiState()
}