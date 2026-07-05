package com.alexvicente.moviedb.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alexvicente.moviedb.core.data.local.entity.MovieDetailsEntity

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