package com.alexvicente.moviedb.features.favorites.domain.usecase

import com.alexvicente.moviedb.features.favorites.domain.model.Favorite
import com.alexvicente.moviedb.features.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFavoritesUseCase @Inject constructor(
    private val repository: FavoritesRepository
){
    operator fun invoke(): Flow<List<Favorite>> = repository.getAllFavoritesRepo()
}

