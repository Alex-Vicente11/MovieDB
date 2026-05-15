package com.example.apptest.features.favorites.domain.usecase

import com.example.apptest.features.favorites.domain.repository.FavoritesRepository
import javax.inject.Inject

class RemoveFavoriteUseCase @Inject constructor(
    private val repository: FavoritesRepository
) {
    suspend operator fun invoke(movieId: Int) = repository.removeFavoriteRepo(movieId)
}