package com.alexvicente.moviedb.features.tv_series.data.remote.api

import com.alexvicente.moviedb.features.tv_series.data.remote.dto.TvCreditsDto
import com.alexvicente.moviedb.features.tv_series.data.remote.dto.TvSeasonDetailsDto
import com.alexvicente.moviedb.features.tv_series.data.remote.dto.TvSeriesDetailsDto
import com.alexvicente.moviedb.features.tv_series.data.remote.dto.TvSeriesResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TvSeriesApi {

    @GET("tv/popular")
    suspend fun getPopularTvSeries(
        @Query("page") page: Int,
        @Query("language") language: String = "es-MX"
    ): TvSeriesResponseDto

    @GET("tv/top_rated")
    suspend fun getTopRatedTvSeries(
        @Query("page") page: Int,
        @Query("language") language: String = "es-MX"
    ): TvSeriesResponseDto

    @GET("tv/{series_id}")
    suspend fun getTvSeriesDetails(
        @Path("series_id") seriesId: Int,
        @Query("language") language: String = "es-MX"
    ): TvSeriesDetailsDto

    @GET("tv/{series_id}/credits")
    suspend fun getTvSeriesCredits(
        @Path("series_id") seriesId: Int,
        @Query("language") language: String = "es-MX"
    ): TvCreditsDto

    @GET("tv/{series_id}/season/{season_number}")
    suspend fun getTvSeasonDetails(
        @Path("series_id") series: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("language") language: String = "es-MX"
    ): TvSeasonDetailsDto
}