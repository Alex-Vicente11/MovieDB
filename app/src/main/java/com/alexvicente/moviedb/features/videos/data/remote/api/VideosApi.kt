package com.alexvicente.moviedb.features.videos.data.remote.api

import com.alexvicente.moviedb.features.videos.data.remote.dto.VideoResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface VideosApi {

    @GET("movie/{movie_id}/videos")
    suspend fun getMovieVideos(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "es-MX"
    ): VideoResponseDto
}