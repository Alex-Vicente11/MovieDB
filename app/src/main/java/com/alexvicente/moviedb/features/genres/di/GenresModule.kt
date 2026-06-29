package com.alexvicente.moviedb.features.genres.di

import com.alexvicente.moviedb.features.genres.data.remote.api.GenresApi
import com.alexvicente.moviedb.features.genres.data.remote.api.MoviesByGenreApi
import com.alexvicente.moviedb.features.genres.data.repository.GenresRepositoryImpl
import com.alexvicente.moviedb.features.genres.data.repository.MoviesByGenreRepositoryImpl
import com.alexvicente.moviedb.features.genres.domain.repository.GenresRepository
import com.alexvicente.moviedb.features.genres.domain.repository.MoviesByGenreRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GenresModule {

    @Binds
    @Singleton
    abstract fun bindGenresRepository(
        impl: GenresRepositoryImpl
    ): GenresRepository

    @Binds
    @Singleton
    abstract fun bindMoviesByGenreRepository(
        impl: MoviesByGenreRepositoryImpl
    ): MoviesByGenreRepository

    companion object {

        @Provides
        @Singleton
        fun provideGenresApi(retrofit: Retrofit): GenresApi =
            retrofit.create(GenresApi::class.java)

        @Provides
        @Singleton
        fun provideMoviesByGenreApi(retrofit: Retrofit): MoviesByGenreApi =
            retrofit.create(MoviesByGenreApi::class.java)
    }
}