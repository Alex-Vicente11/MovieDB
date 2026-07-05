package com.alexvicente.moviedb.features.favorites.domain.repository

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.features.favorites.domain.model.Favorite
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {

    fun getAllFavoritesRepo(): Flow<List<Favorite>>
    fun isFavoriteRepo(movieId: Int): Flow<Boolean>
    suspend fun addFavoriteRepo(movie: Movie): Resource<Unit>
    suspend fun removeFavoriteRepo(movieId: Int): Resource<Unit>
}