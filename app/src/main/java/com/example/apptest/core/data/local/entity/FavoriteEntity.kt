package com.example.apptest.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Favoritos es un feature 100% local - no hay API de TMDB para esto.
 * Room es la única fuente de datos: no hay Retrofit, no hay DTO.
 *
 * Guardamos los datos de la película directamente en la tabla de favoritos
 * para que la pantalla de favoritos funcione completamente offline sin
 * necesitar cruzar datos con otras tablas (evitamos JOINs complejos).
 *
 * added_at -> timestamp cuando el usuario marcó la película como favorita.
 *  Permite ordenar favoritos por fecha de adición.
 */

@Entity(tableName = "favorites")
data class FavoriteEntity(

    @PrimaryKey
    @ColumnInfo(name = "movie_id")
    val movieId: Int,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "poster_path")
    val posterPath: String?,

    @ColumnInfo(name = "vote_average")
    val voteAverage: Double,

    @ColumnInfo(name = "release_date")
    val releaseDate: String,

    @ColumnInfo(name = "overview")
    val overview: String,

    // Timestamp de cuando el usuario marcó la película como favorita
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)