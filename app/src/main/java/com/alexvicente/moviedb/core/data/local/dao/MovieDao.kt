package com.alexvicente.moviedb.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alexvicente.moviedb.core.data.local.entity.MovieEntity
import kotlinx.coroutines.flow.Flow

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