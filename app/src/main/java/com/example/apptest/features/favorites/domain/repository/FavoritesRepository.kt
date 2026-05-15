package com.example.apptest.features.favorites.domain.repository

import com.example.apptest.core.domain.model.Movie
import com.example.apptest.features.favorites.domain.model.Favorite
import kotlinx.coroutines.flow.Flow

/**
 * Flow<List<Favorite>> -> reactivo: la UI se actualiza cuando cambia la lista
 * FLow<Boolean> -> reactivo: el icono reacciona al instante
 */

interface FavoritesRepository {

    fun getAllFavoritesRepo(): Flow<List<Favorite>>

    fun isFavoriteRepo(movieId: Int): Flow<Boolean>

    suspend fun addFavoriteRepo(movie: Movie)

    suspend fun removeFavoriteRepo(movieId: Int)
}