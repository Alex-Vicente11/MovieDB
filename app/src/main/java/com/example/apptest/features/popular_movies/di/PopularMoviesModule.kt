package com.example.apptest.features.popular_movies.di

import com.example.apptest.features.popular_movies.data.remote.api.PopularMoviesApi
import com.example.apptest.features.popular_movies.data.repository.PopularMoviesRepositoryImpl
import com.example.apptest.features.popular_movies.domain.repository.PopularMoviesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Patrón de construcción:
 * 1. API (capa más baja - acceso a datos externos)
 * 2. Repository (implementa lógica de acceso a datos)
 * 3. UseCase (implementa lógica de negocio)
 */


@Module
@InstallIn(SingletonComponent::class)
abstract class PopularMoviesModule {
    @Binds
    @Singleton
    abstract fun bindPopularMoviesRepository(
        impl: PopularMoviesRepositoryImpl
    ): PopularMoviesRepository

    companion object {
        @Provides @Singleton
        fun providePopularMoviesApi(retrofit: Retrofit): PopularMoviesApi =
            retrofit.create(PopularMoviesApi::class.java)
    }
}