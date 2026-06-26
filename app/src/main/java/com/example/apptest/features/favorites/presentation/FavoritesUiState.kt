package com.example.apptest.features.favorites.presentation

import com.example.apptest.features.favorites.domain.model.Favorite

// Estado de UI
sealed class FavoritesUiState {
    object Loading: FavoritesUiState()
    object Empty: FavoritesUiState()
    data class Success(val favorites: List<Favorite>): FavoritesUiState()
}