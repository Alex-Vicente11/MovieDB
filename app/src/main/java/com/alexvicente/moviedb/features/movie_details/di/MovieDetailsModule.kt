package com.alexvicente.moviedb.features.movie_details.di

import com.alexvicente.moviedb.features.movie_details.data.remote.api.MovieDetailsApi
import com.alexvicente.moviedb.features.movie_details.data.repository.MovieDetailsRepositoryImpl
import com.alexvicente.moviedb.features.movie_details.domain.repository.MovieDetailsRepository
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

// Reemplaza MovieDetailsContainer.kt — ELIMINAR el container.
//
// ¿Por qué abstract class y no object?
//   @Binds solo funciona en funciones abstractas → necesita abstract class.
//   Los @Provides van en el companion object (funciones concretas).
//
// @Binds → vincula interfaz con implementación.
//   REQUISITO: MovieDetailsRepositoryImpl debe tener @Inject constructor.
//
// @Provides → Hilt no puede crear MovieDetailsApi solo (necesita retrofit.create()).
//   Hilt resuelve Retrofit automáticamente desde NetworkModule.

@Module
@InstallIn(SingletonComponent::class)
abstract class MovieDetailsModule {

    @Binds
    @Singleton
    abstract fun bindMovieDetailsRepository(
        impl: MovieDetailsRepositoryImpl
    ): MovieDetailsRepository

    companion object {
        @Provides
        @Singleton
        fun provideMovieDetailsApi(retrofit: Retrofit): MovieDetailsApi =
            retrofit.create(MovieDetailsApi::class.java)
    }
}