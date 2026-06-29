package com.alexvicente.moviedb.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alexvicente.moviedb.core.data.local.entity.MovieDetailsEntity

/**
 * Nótese que getMovieDetails NO retorna Flow sino suspend.
 * ¿Por qué? La pantalla de detalles no necesita ser reactiva - el usuario ve los detalles una sola vez por visita.
 * No hay cambios en tiempo real.
 * suspend es más eficiente que Flow para lecturas puntuales
 *
 * Comparación:
 *      Flow -> para listas que pueden cambiar (populares, favoritos)
 *      suspend -> para objetos únicos que se leen una sola vez (detalles)
 */

@Dao
interface MovieDetailsDao {

    // suspend (no flow) - lectura puntual de un solo objeto
    @Query("SELECT * FROM movie_details WHERE id = :movieId")
    suspend fun getMovieDetails(movieId: Int): MovieDetailsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovieDetails(movieDetails: MovieDetailsEntity)

    @Query("DELETE FROM movie_details WHERE id = :movieId")
    suspend fun deleteMovieDetails(movieId: Int)
}