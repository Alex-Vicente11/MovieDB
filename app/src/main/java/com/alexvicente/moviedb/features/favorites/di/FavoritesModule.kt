package com.alexvicente.moviedb.features.favorites.di

import com.alexvicente.moviedb.features.favorites.data.repository.FavoritesRepositoryImpl
import com.alexvicente.moviedb.features.favorites.domain.repository.FavoritesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FavoritesModule {

    @Binds @Singleton
    abstract fun bindFavoritesRepository(
        impl: FavoritesRepositoryImpl
    ): FavoritesRepository
}