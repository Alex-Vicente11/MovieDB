package com.example.apptest.features.videos.di

import com.example.apptest.features.videos.data.remote.api.VideosApi
import com.example.apptest.features.videos.data.repository.VideosRepositoryImpl
import com.example.apptest.features.videos.domain.repository.VideosRepository
import com.example.apptest.features.videos.domain.usecase.GetMovieVideosUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Patron Constructor
 * 1. Api (capa mas baja - acceso a datos externos)
 * 2. Repository (implementa lógica de acceso a datos)
 * 3. UseCase (implementa lógica de negocio)
 */


// Reemplaza VideosContainer.kt — ELIMINAR el container.
@Module
@InstallIn(SingletonComponent::class)
abstract class VideosModule {

    @Binds @Singleton
    abstract fun bindVideosRepository(
        impl: VideosRepositoryImpl
    ): VideosRepository

    companion object {
        @Provides @Singleton
        fun provideVideosApi(retrofit: Retrofit): VideosApi =
            retrofit.create(VideosApi::class.java)
    }
}
