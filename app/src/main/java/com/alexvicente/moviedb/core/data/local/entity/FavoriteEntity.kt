package com.alexvicente.moviedb.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

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