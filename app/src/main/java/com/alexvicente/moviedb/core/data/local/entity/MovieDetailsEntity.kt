package com.alexvicente.moviedb.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movie_details")
data class MovieDetailsEntity(

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

    // Campos exclusivos de detalles
    @ColumnInfo(name = "runtime")
    val runtime: Int?,

    @ColumnInfo(name = "budget")
    val budget: Long?,

    @ColumnInfo(name = "revenue")
    val revenue: Long?,

    // genres se serializa a JSON mediante Converters.kt
    // Room no soporta List<Genere> directamente
    @ColumnInfo(name = "genres_json")
    val genresJson: String,

    @ColumnInfo(name = "tagline")
    val tagline: String?,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()

)