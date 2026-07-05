package com.alexvicente.moviedb.core.di

import android.content.Context
import androidx.room.Room
import com.alexvicente.moviedb.core.data.local.AppDatabase
import com.alexvicente.moviedb.core.data.local.dao.FavoritesDao
import com.alexvicente.moviedb.core.data.local.dao.MovieDao
import com.alexvicente.moviedb.core.data.local.dao.MovieDetailsDao
import com.alexvicente.moviedb.core.data.local.dao.VideosDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // --- AppDatabase ---
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "moviedb_database"
        )

            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    @Singleton
    fun provideMovieDao(db: AppDatabase): MovieDao = db.movieDao()

    @Provides
    @Singleton
    fun provideMovieDetailsDao(db: AppDatabase): MovieDetailsDao = db.movieDetailsDao()

    @Provides
    @Singleton
    fun provideVideosDao(db: AppDatabase): VideosDao = db.videosDao()

    @Provides
    @Singleton
    fun provideFavoritesDao(db: AppDatabase): FavoritesDao = db.favoritesDao()
}