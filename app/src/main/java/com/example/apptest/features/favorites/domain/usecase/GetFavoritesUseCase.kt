package com.example.apptest.features.favorites.domain.usecase

import com.example.apptest.features.favorites.domain.model.Favorite
import com.example.apptest.features.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 *  UseCases de Favoritos - 4 casos de uso, uno por operación
 *  PRINCIPIO: cada UseCase hace una sola cosa (SRP)
 */

class GetFavoritesUseCase @Inject constructor(
    private val repository: FavoritesRepository
){
    operator fun invoke(): Flow<List<Favorite>> = repository.getAllFavoritesRepo()
}

