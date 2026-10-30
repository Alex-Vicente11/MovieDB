package com.alexvicente.moviedb.features.tv_series.data.remote.api

import com.alexvicente.moviedb.features.tv_series.data.remote.dto.TvSeriesDetailsDto
import com.alexvicente.moviedb.features.tv_series.data.remote.dto.TvSeriesResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TvSeriesApi {

    @GET("tv/popular")
    suspend fun getPopularTvSeries(
        @Query("page") page: Int,
        @Query("languge") language: String
    ): TvSeriesResponseDto

    @GET("tv/top_rated")
    suspend fun getTopRatedTvSeries(
        @Query("page") page: Int,
        @Query("language") language: String
    ): TvSeriesResponseDto

    @GET("tv/{series_id}")
    suspend fun getTvSeriesDetails(
        @Path("series_id") seriesId: Int,
        @Query("language") language: String
    ): TvSeriesDetailsDto
}