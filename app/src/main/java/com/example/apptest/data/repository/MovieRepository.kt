package com.example.apptest.data.repository

import android.util.Log
import com.example.apptest.data.api.RetrofitClient
import com.example.apptest.data.model.Movie
import com.google.gson.GsonBuilder

class MovieRepository {

    private val apiService = RetrofitClient.apiService

    companion object {
        private const val TAG = "MovieRepository"
    }

    private val responseJson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    suspend fun searchMovies(query: String): List<Movie>? {
        return try {
            Log.d(TAG, "Searching movies with query : $query")

            val response = apiService.searchMovies(query)

            if (response.isSuccessful) {
                val body = response.body()

                val jsonFormatted =  responseJson.toJson(body)
                Log.d(TAG, "RESPONSE JSON:\n$jsonFormatted")
                Log.d(TAG, "  °Page: ${body?.page}")
                Log.d(TAG, "  °Results: ${body?.results?.size} movies")
                Log.d(TAG, "Total pages: ${body?.totalPages}")
                Log.d(TAG, "Total results: ${body?.totalResults}")
                Log.d(TAG, "---------------------------------------")


                Log.d(TAG, "Success Found: ${body?.results?.size} movies")
                body?.results?.forEachIndexed { index, movie ->
                    Log.d(TAG, "")
                    Log.d(TAG, "    Movie ${index + 1}:")
                    Log.d(TAG, "    Title: ${movie.title}")
                    Log.d(TAG, "    Original: ${movie.originalTitle}")
                    Log.d(TAG, "    Rating: ${movie.voteAverage}/10 (${movie.voteCount} votes")
                    Log.d(TAG, "    Release: ${movie.releaseDate}")
                    Log.d(TAG, "    Language: ${movie.originalLanguage}")
                    Log.d(TAG, "    Popularity: ${movie.popularity}")
                    Log.d(TAG, "    Overview: ${movie.overview.take(100)}...")
                    Log.d(TAG, "    Poster: ${movie.getPosterURL()}")
                }
                body?.results
            } else {
                Log.e(TAG, "Error ${response.code()} - ${response.message()}")
                Log.e(TAG, "Body: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception ${e.message}", e)
            e.printStackTrace()
            null
        }
    }

    suspend fun getPopularMovies(): List<Movie>? {
        return try {
            Log.d(TAG, "Getting popular movies...")
            val response = apiService.getPopularMovies()

            when {
                response.isSuccessful -> {
                    val body = response.body()


                    Log.d(TAG, "Success! found: ${body?.results?.size} movies")

                    body?.let {
                        Log.d(TAG, "Total pages: ${it.totalPages}")
                        Log.d(TAG, "Total results: ${it.totalResults}")
                    }
                    body?.results
                }
                else -> {
                    Log.e(TAG, "Error: ${response.code()}")
                    null
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception ${e.message}", e)
            null
        }
    }
}