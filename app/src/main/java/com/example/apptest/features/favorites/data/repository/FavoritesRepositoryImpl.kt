package com.example.apptest.features.favorites.data.repository

import com.example.apptest.core.data.local.dao.FavoritesDao
import com.example.apptest.core.data.local.mapper.toDomain
import com.example.apptest.core.data.local.mapper.toFavoriteEntity
import com.example.apptest.core.domain.model.Movie
import com.example.apptest.features.favorites.domain.model.Favorite
import com.example.apptest.features.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// ════════════════════════════════════════════════════════════════════════════
// FavoritesRepositoryImpl.kt
// UBICACIÓN: features/favorites/data/repository/FavoritesRepositoryImpl.kt
// ════════════════════════════════════════════════════════════════════════════
//
// Favoritos es 100% local — no hay Retrofit aquí.
// Solo FavoritesDao. Room es la única fuente de datos.
//
// .map { entities -> entities.toDomain() }
//   Transforma el Flow<List<FavoriteEntity>> de Room
//   en Flow<List<Favorite>> del dominio.
//   Cada vez que Room emita nuevos datos, el map los transforma automáticamente.
// ════════════════════════════════════════════════════════════════════════════

class FavoritesRepositoryImpl @Inject constructor(
    private val favoritesDao: FavoritesDao
) : FavoritesRepository {

    override fun getAllFavoritesRepo(): Flow<List<Favorite>> =
        favoritesDao.getAllFavorites().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun isFavoriteRepo(movieId: Int): Flow<Boolean> =
        favoritesDao.isFavorite(movieId)

    override suspend fun addFavoriteRepo(movie: Movie) =
        favoritesDao.addFavorite(movie.toFavoriteEntity())

    override suspend fun removeFavoriteRepo(movieId: Int) =
        favoritesDao.removeFavorite(movieId)
}