package com.example.apptest.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Relación con MovieDetailsEntity:
 *      Una película tiene múltiples videos (1:N).
 *      movie_id es la Foreign Key que vincula cada video con su película.
 *
 * @Entity con indices:
 *      indices = [Index("movie_id")] -> crea un índice en la columna movie_id.
 *      Esto acelera enormemente la consulta "dame todos los videos de movideId=X"
 *      porque SQLite no tiene que escanear toda la tabla.
 *
 * NOTA: No usamos ForeignKey con onDelete CASCADE porque queremos mantener los videos
 *  aunque se elimine la entrada de movie_details (caché independiente).
 *  Si en el futuro quieres borrado en cascada, agregar:
 *  foreignKeys = [ForeignKey(entity = MovieDetailsEntity::class, parentColumns = ["id"], childColumns = ["movie_id], onDelete = ForeignKey.CASCADE)]
 *
 */

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