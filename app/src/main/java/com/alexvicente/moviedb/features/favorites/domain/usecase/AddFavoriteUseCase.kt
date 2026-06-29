package com.alexvicente.moviedb.features.favorites.domain.usecase

import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.features.favorites.domain.repository.FavoritesRepository
import javax.inject.Inject

class AddFavoriteUseCase @Inject constructor(
    private val repository: FavoritesRepository
) {
    suspend operator fun invoke(movie: Movie) = repository.addFavoriteRepo(movie)
}