package com.alexvicente.moviedb.features.search.di

import com.alexvicente.moviedb.features.search.data.remote.api.SearchApi
import com.alexvicente.moviedb.features.search.data.repository.SearchRepositoryImpl
import com.alexvicente.moviedb.features.search.domain.repository.SearchRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SearchModule {

    @Binds
    @Singleton
    abstract fun bindSearchRepository(
        impl: SearchRepositoryImpl
    ): SearchRepository

    companion object {
        @Provides @Singleton
        fun provideSearchApi(retrofit: Retrofit): SearchApi =
            retrofit.create(SearchApi::class.java)
    }
}