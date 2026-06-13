package com.example.apptest.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.apptest.core.data.local.entity.MovieEntity
import kotlinx.coroutines.flow.Flow

/**
 * ¿Qué es un @Dao?
 *      Data Access Object - interfaz que define todas las operaciones
 *      sobre un tabla. Room genera la implementación automáticamante en tiempo de compilación.
 *      Tú defines QUÉ, Room genera el CÓMO
 *
 * @Query -> ejecuta SQL personalizado.
 *      Room verifica la sintaxis SQL en compilación. Si escribes mal una columna o una tabla,
 *      el error aparece al compilar, no en runtime.
 *
 * @Insert -> inserta filas.
 *      onConflict = REPLACE -> si ya existe una película con el mismo id,
 *      la reemplaza. Esto actualiza el caché autmáticamente.
 *
 * @Delete y @Query DELETE -> eliminan filas.
 *
 * Flow<List<MovieEntity>> vs suspend:
 *      - Flow -> para lecturas reactivas. La UI se actualiza automáticamente
 *      cuando cambian los datos. Úsalo en getters.
 *      - suspend -> para escrituras puntuales (insert, delete).
 *      No necesitan ser reactivas - solo se ejecutan una vez.
 */

@Dao
interface MovieDao {

    // Lectura reactiva
    // Flow -> Room emite automáticamente cuandos los datos cambian.
    // El RepositoryImpl observa este Flow y lo transforma al dominio.

    @Query("SELECT * FROM movies WHERE is_popular = 1 ORDER BY popularity DESC")
    fun getPopularMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE title LIKE '%' || :query || '%'")
    fun searchMovies(query: String): Flow<List<MovieEntity>>

    // Lectura puntual
    // suspend (sin Flow) para lecturas de un solo valor que no necesitan
    // ser reactivas - por ejemplo verificar si el caché expiró

    @Query("SELECT MAX(cached_at) FROM movies WHERE is_popular = 1")
    suspend fun getLastCacheTime(): Long?

    // Escritura

    // OnConflictStrategy.REPLACE -> si el id ya existe, actualiza la fila.
    // Equivale a un UPSERT (INSERT OR REPLACE en SQLite).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<MovieEntity>)

    @Query("DELETE FROM movies WHERE is_popular = 1")
    suspend fun deletePopularMovies()

    @Query("DELETE FROM movies WHERE is_popular = 0")
    suspend fun deleteSearchResults()
}