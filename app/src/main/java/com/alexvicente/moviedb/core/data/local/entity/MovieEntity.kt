package com.alexvicente.moviedb.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class MovieEntity(

    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "overview")
    val overview: String,

    @ColumnInfo(name = "poster_path")
    val posterPath: String?,

    @ColumnInfo(name = "backdrop_path")
    val backdropPath: String?,

    @ColumnInfo(name = "vote_average")
    val voteAverage: Double,

    @ColumnInfo(name = "vote_count")
    val voteCount: Int,

    @ColumnInfo(name = "release_date")
    val releaseDate: String,

    @ColumnInfo(name = "popularity")
    val popularity: Double,

    /*
    Campos de caché
    Indican de dónde viene la película y cuándo fue guardada.
    Esto permite implementar TTL (Time To Live) para invalidar el caché.
     */

    // true = viene de la lista de populares, false = viene de búsqueda
    @ColumnInfo(name = "is_popular")
    val isPopular: Boolean = false,

    //Timestamp en milisegundos de cuando fue guardada en Room.
    // Se usa para verificar si el caché expiró (>30 minutos = recargar)
    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)