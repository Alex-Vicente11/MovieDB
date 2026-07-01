package com.alexvicente.moviedb.features.favorites.data.repository

import com.alexvicente.moviedb.core.data.local.dao.FavoritesDao
import com.alexvicente.moviedb.core.data.local.mapper.toDomain
import com.alexvicente.moviedb.core.data.local.mapper.toFavoriteEntity
import com.alexvicente.moviedb.core.data.util.ErrorMapper
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.data.util.toUserMessage
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.features.favorites.domain.model.Favorite
import com.alexvicente.moviedb.features.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FavoritesRepositoryImpl @Inject constructor(
    private val favoritesDao: FavoritesDao
) : FavoritesRepository {

    override fun getAllFavoritesRepo(): Flow<List<Favorite>> =
        favoritesDao.getAllFavorites().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun isFavoriteRepo(movieId: Int): Flow<Boolean> =
        favoritesDao.isFavorite(movieId)

    override suspend fun addFavoriteRepo(movie: Movie): Resource<Unit> = try {
        favoritesDao.addFavorite(movie.toFavoriteEntity())
        Resource.Success(Unit)
    } catch (e: Exception) {
        val error = ErrorMapper.map(e)
        Resource.Error(message = error.toUserMessage(), error = error)
    }

    override suspend fun removeFavoriteRepo(movieId: Int): Resource<Unit> = try {
        favoritesDao.removeFavorite(movieId)
        Resource.Success(Unit)
    } catch (e: Exception) {
        val error = ErrorMapper.map(e)
        Resource.Error(message = error.toUserMessage(), error = error)
    }
}