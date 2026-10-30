package com.alexvicente.moviedb.features.tv_series.di

import com.alexvicente.moviedb.features.tv_series.data.remote.api.TvSeriesApi
import com.alexvicente.moviedb.features.tv_series.data.repository.TvSeriesDetailsRepositoryImpl
import com.alexvicente.moviedb.features.tv_series.data.repository.TvSeriesRepositoryImpl
import com.alexvicente.moviedb.features.tv_series.domain.repository.TvSeriesDetailsRepository
import com.alexvicente.moviedb.features.tv_series.domain.repository.TvSeriesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TvSeriesModule {

    @Binds
    @Singleton
    abstract fun bindTvSeriesRepository(
        impl: TvSeriesRepositoryImpl
    ): TvSeriesRepository

    @Binds
    @Singleton
    abstract fun bindTvSeriesDetailsRepository(
        impl: TvSeriesDetailsRepositoryImpl
    ): TvSeriesDetailsRepository

    companion object {

        @Provides
        @Singleton
        fun provideTvSeriesApi(retrofit: Retrofit): TvSeriesApi =
            retrofit.create(TvSeriesApi::class.java)
    }
}