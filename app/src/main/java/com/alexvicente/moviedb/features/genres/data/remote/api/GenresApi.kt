package com.alexvicente.moviedb.features.genres.data.remote.api

import retrofit2.http.Query
import com.alexvicente.moviedb.features.genres.data.remote.dto.GenresResponseDto
import retrofit2.http.GET

interface GenresApi {

    @GET("genre/movie/list")
    suspend fun getGenres(
        @Query("language") language: String = "es-MX"
    ): GenresResponseDto
}