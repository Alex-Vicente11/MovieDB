package com.alexvicente.moviedb.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ¿Qué es una @Entity
 *      Es una clase de datos que representa UNA TABLA en SQLite.
 *      Cada propiedad de la data class = una columna en la tabla.
 *      Cada instancia de la clase = una fila en la tabla.
 *
 * ¿Por qué está en core/ y no en popular_movies/?
 *      MovieEntity se usa tanto para películas populares como para resultados
 *      de búsqueda - ambos features usan el mismo modelo Movie del dominio
 *      y la misma tabla de base de datos. Centralizar evita duplicación
 *
 * @PrimaryKey -> columna que identifica únicamente cada fila
 *      autoGenerate = false porque el ID viene de la API de TMDB.
 *
 * @ColumnInfo(name = "...") -> define el nombre de la columna en SQLite.
 *      Si no se especifica, Room usa el nombre de la propiedad de Kotlin.
 *      Se usan snake_case por convención de bases de datos.
 *
 * SEPARACIÓN DE CAPAS:
 *      MovieEntity (Data Layer) != Movie(Domain Layer)
 *      Son clases diferentes a propósito:
 *      -MovieEntity tiene campos específicos de base de datos (timestamps, etc.)
 *      -Movie es puro dominio sin anotaciones de Room
 *      -MovieEntityMapper convierte entre ellas
 */

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