package com.alexvicente.moviedb.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ¿Por qué está en movie_details/ y no en core/?
 *      MovieDetailsEntity tiene campos que SOLO usa la pantalla de detalles
 *      (runtime, budget, revenue, genres, tagline). Nunca se usa en listas.
 *      Principio YAGNI: no lo movemos a core hasta que otro feature lo necesite.
 *
 * genres: String -> los géneros se serializan como JSON con Converters.kt
 *      En dominio: List<Genre> = [Genre(28, "Action"), Genre(878, "Sci-Fi")]
 *      En SQLite: genres TEXT = '[{"id":28, "name":"Action"},{"id":878,"name":"Sci-Fi"}]'
 *
 * cached_at -> permite caché perpetuo para detalles.
 *      La estrategia para detalles es cache-first sin expiración:
 *      si existe en Room, se devuelve sin llamar a la API.
 *      Solo se refresca si el usuario lo fuerza explícitamente.
 */

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