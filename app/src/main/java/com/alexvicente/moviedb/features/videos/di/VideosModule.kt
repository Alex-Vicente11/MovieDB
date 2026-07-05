package com.alexvicente.moviedb.features.videos.di

import com.alexvicente.moviedb.features.videos.data.remote.api.VideosApi
import com.alexvicente.moviedb.features.videos.data.repository.VideosRepositoryImpl
import com.alexvicente.moviedb.features.videos.domain.repository.VideosRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

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
