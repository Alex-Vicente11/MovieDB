package com.alexvicente.moviedb.features.favorites.domain.repository

import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.features.favorites.domain.model.Favorite
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