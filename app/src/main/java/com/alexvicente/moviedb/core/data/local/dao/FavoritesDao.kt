package com.alexvicente.moviedb.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alexvicente.moviedb.core.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {

    // Flow -> lista reactiva, se actualiza cuando se agrega/elimina un favorito
    @Query("SELECT * FROM favorites ORDER BY added_at DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    // Flow<Boolean> -> el botón (ícono) reacciona inmediatamente sin recargar
    @Query("SELECT COUNT(*) > 0 FROM favorites WHERE movie_id = :movieId")
    fun isFavorite(movieId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE movie_id = :movieId")
    suspend fun removeFavorite(movieId: Int)
}