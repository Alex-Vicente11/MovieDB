package com.example.apptest.data.api
import com.example.apptest.data.model.MovieResponse
import org.intellij.lang.annotations.Language
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query


interface TMDDApiService {

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("language") language: String = "es-MX",
        @Query("page") page: Int = 1
    ): Response <MovieResponse>

    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("language") language: String = "es-MX",
        @Query("page") page: Int = 1
    ): Response<MovieResponse>
}