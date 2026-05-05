package com.example.apptest.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.apptest.core.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Favoritos usa Flow en TODAS sus lecturas porque:
 *  - La lista de favoritos debe actualizarse en tiempo real cuando el
 *    usuario agrega o elimina una pelicula desde cualquier pantalla.
 *  - isFavorite(movieId) como Flow permite que el botón (ícono) en la pantalla
 *    de detalles refleje el estado actual sin necesidad de recargar.
 *
 * Esto es la diferencia clave con videos/detalles que usan suspend:
 *  Favoritos = estado que puede cambiar en cualquier momento -> Flow
 *  Detalles = snapshot que se lee una vez -> suspend
 */

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