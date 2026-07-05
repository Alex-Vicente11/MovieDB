package com.alexvicente.moviedb.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "videos",
    indices = [Index(value = ["movie_id"])]
)
data class VideoEntity(

    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,         // ID del video en TMDB (String, no Int)

    @ColumnInfo(name = "movie_id")
    val movieId: Int,       // FK - a qué película pertenece este video

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "key")
    val key: String,         // ID del video en YouTube

    @ColumnInfo(name = "site")
    val site: String,

    @ColumnInfo(name = "type")
    val type: String,       // "trailer", "Teaser", "Clip", etc.

    @ColumnInfo(name = "official")
    val official: Boolean,

    @ColumnInfo(name = "published_at")
    val publishedAt: String,

    @ColumnInfo(name = "size")
    val size: Int,          // resolución: 360, 480, 720, 1080

    @ColumnInfo(name = "iso_639_1")
    val iso6391: String,    // idioma: "es, "en"

    @ColumnInfo(name = "iso_3166_1")
    val iso31661: String,   // país: "US", "MX"

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)