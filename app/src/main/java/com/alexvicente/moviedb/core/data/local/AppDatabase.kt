package com.alexvicente.moviedb.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.alexvicente.moviedb.core.data.local.dao.FavoritesDao
import com.alexvicente.moviedb.core.data.local.dao.MovieDao
import com.alexvicente.moviedb.core.data.local.dao.MovieDetailsDao
import com.alexvicente.moviedb.core.data.local.dao.VideosDao
import com.alexvicente.moviedb.core.data.local.entity.FavoriteEntity
import com.alexvicente.moviedb.core.data.local.entity.MovieDetailsEntity
import com.alexvicente.moviedb.core.data.local.entity.MovieEntity
import com.alexvicente.moviedb.core.data.local.entity.VideoEntity

@Database(
    entities = [
        MovieEntity::class,     // tabla: movies (populares + búsqueda)
        MovieDetailsEntity::class, // tabla: movie_details
        VideoEntity::class,         // tabla: videos
        FavoriteEntity::class,      // tablas: favorites
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun movieDao(): MovieDao
    abstract fun movieDetailsDao(): MovieDetailsDao
    abstract fun videosDao(): VideosDao
    abstract fun favoritesDao(): FavoritesDao
}