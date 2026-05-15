package com.example.apptest.features.favorites.domain.usecase

import com.example.apptest.core.domain.model.Movie
import com.example.apptest.features.favorites.domain.repository.FavoritesRepository
import javax.inject.Inject

class AddFavoriteUseCase @Inject constructor(
    private val repository: FavoritesRepository
) {
    suspend operator fun invoke(movie: Movie) = repository.addFavoriteRepo(movie)
}