package com.alexvicente.moviedb.features.popular_movies.di

import com.alexvicente.moviedb.features.popular_movies.data.repository.PopularMoviesRepositoryImpl
import com.alexvicente.moviedb.features.popular_movies.domain.repository.PopularMoviesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PopularMoviesModule {
    @Binds
    @Singleton
    abstract fun bindPopularMoviesRepository(
        impl: PopularMoviesRepositoryImpl
    ): PopularMoviesRepository
}