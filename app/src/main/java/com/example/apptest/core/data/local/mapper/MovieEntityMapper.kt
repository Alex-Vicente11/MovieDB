package com.example.apptest.core.data.local.mapper

import com.example.apptest.core.data.local.entity.FavoriteEntity
import com.example.apptest.core.data.local.entity.MovieDetailsEntity
import com.example.apptest.core.data.local.entity.MovieEntity
import com.example.apptest.core.data.local.entity.VideoEntity
import com.example.apptest.core.domain.model.Movie
import com.example.apptest.features.favorites.domain.model.Favorite
import com.example.apptest.features.movie_details.domain.model.Genre
import com.example.apptest.features.movie_details.domain.model.MovieDetails
import com.example.apptest.features.videos.domain.model.Video
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * PATRÓN: Mapper / Converter
 *
 * Convierte entre las capas:
 *      Entity (Data Layer) <-> Domain Model (Domain Layer)
 *
 * ¿Por qué tener clases separadas (Entity vs Domain)?
 *      - Las Entity tienen anotaciones de Room (@Entity, @ColumnInfo) que
 *      contaminarían el Domain Layer con detalles de implementación.
 *      - Los Domain Models son puros Kotlin sin dependencias externas.
 *      - Si se cambia Room por SQLDelight, solo se cambian los mappers.
 *      y las entidades - los ViewModels y UseCases NO se tocan.
 *      - Principio de inversión de dependencias: el dominio no depende de datos.
 *
 * Extensiones de Kotlin para mappers:
 *      fun MovieEntity.toDomain()  -> convierte UNA entidad a dominio
 *      fun List<MovieEntity>.toDomain() -> convierte UNA lista completa
 *      fun Movie.toEntity() -> convierte dominio a entidad para guardar
 *
 *
 */


private val gson = Gson()

// --- Movie <-> MovieEntity ---
fun MovieEntity.toDomain(): Movie = Movie(
    id = id,
    title = title,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    voteAverage = voteAverage,
    voteCount = voteCount,
    releaseDate = releaseDate,
    popularity = popularity
)

fun Movie.toEntity(isPopular: Boolean = false): MovieEntity = MovieEntity(
    id = id,
    title = title,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    voteAverage = voteAverage,
    voteCount = voteCount,
    releaseDate = releaseDate,
    popularity = popularity,
    isPopular = isPopular,
    cachedAt = System.currentTimeMillis()
)

fun List<Movie>.toEntity(isPopular: Boolean = false): List<MovieEntity> =
    map { it.toEntity(isPopular) }


// --- MovieDetails <-> MovieDetailsEntity ---
fun MovieDetailsEntity.toDomain(): MovieDetails {
    // Deserealizar géneros desde JSON
    val genreType = object : TypeToken<List<Genre>>() {}.type
    val genres: List<Genre> = gson.fromJson(genresJson, genreType) ?: emptyList()

    return MovieDetails(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        voteCount = voteCount,
        releaseDate = releaseDate,
        popularity = popularity,
        runtime = runtime,
        budget = budget,
        revenue = revenue,
        genres = genres,
        tagline = tagline
    )
}

fun MovieDetails.toEntity(): MovieDetailsEntity = MovieDetailsEntity(
    id = id,
    title = title,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    voteAverage = voteAverage,
    voteCount = voteCount,
    releaseDate = releaseDate,
    popularity = popularity,
    runtime = runtime,
    budget = budget,
    revenue = revenue,
    genresJson = gson.toJson(genres),  // serializar List<Genre> a JSON
    tagline = tagline,
    cachedAt = System.currentTimeMillis()
)

// ---Video <-> VideoEntity ---

fun VideoEntity.toDomain(): Video = Video(
    id = id,
    name = name,
    key = key,
    site = site,
    type = type,
    official = official,
    publishedAt = publishedAt,
    size = size,
    language = iso6391,
    country = iso31661
)

fun Video.toEntity(movieId: Int): VideoEntity = VideoEntity(
    id = id,
    movieId = movieId,
    name = name,
    key = key,
    site = site,
    type = type,
    official = official,
    publishedAt = publishedAt,
    size = size,
    iso6391 = language,
    iso31661 = country,
    cachedAt = System.currentTimeMillis()
)

fun List<Video>.toEntity(movieId: Int): List<VideoEntity> =
    map { it.toEntity(movieId) }

// --- Favorite <-> FavoriteEntity ---
fun FavoriteEntity.toDomain(): Favorite = Favorite(
    id = movieId,
    title = title,
    posterPath = posterPath,
    voteAverage = voteAverage,
    releaseDate = releaseDate,
    overview = overview,
    addedAt = addedAt
)

fun Movie.toFavoriteEntity(): FavoriteEntity = FavoriteEntity(
    movieId = id,
    title = title,
    posterPath = posterPath,
    voteAverage = voteAverage,
    releaseDate = releaseDate,
    overview = overview,
    addedAt = System.currentTimeMillis()
)
