package com.alexvicente.moviedb.features.favorites.domain.usecase

import com.alexvicente.moviedb.features.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IsFavoriteUseCase @Inject constructor(
    private val repository: FavoritesRepository
) {
    operator fun invoke(movieId: Int): Flow<Boolean> = repository.isFavoriteRepo(movieId)
}