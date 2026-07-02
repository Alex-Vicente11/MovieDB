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