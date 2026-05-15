package com.example.apptest.features.favorites.domain.usecase

import com.example.apptest.features.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IsFavoriteUseCase @Inject constructor(
    private val repository: FavoritesRepository
) {
    operator fun invoke(movieId: Int): Flow<Boolean> = repository.isFavoriteRepo(movieId)
}