package com.alexvicente.moviedb.features.favorites.domain.usecase

import com.alexvicente.moviedb.features.favorites.domain.repository.FavoritesRepository
import javax.inject.Inject

class RemoveFavoriteUseCase @Inject constructor(
    private val repository: FavoritesRepository
) {
    suspend operator fun invoke(movieId: Int) = repository.removeFavoriteRepo(movieId)
}