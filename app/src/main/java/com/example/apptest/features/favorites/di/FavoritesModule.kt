package com.example.apptest.features.favorites.di

import com.example.apptest.features.favorites.data.repository.FavoritesRepositoryImpl
import com.example.apptest.features.favorites.domain.repository.FavoritesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Favoritos no necesita @Provides para API (No hay Retrofit).
 * Solo @Binds para vincular interfaz con implementación.
 * FavoritesDao ya es proveído por DatabaseModule - Hilt lo resuelve solo.
 */

@Module
@InstallIn(SingletonComponent::class)
abstract class FavoritesModule {

    @Binds @Singleton
    abstract fun bindFavoritesRepository(
        impl: FavoritesRepositoryImpl
    ): FavoritesRepository
}