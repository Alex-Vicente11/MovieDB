package com.example.apptest.data.repository

import android.util.Log
import com.example.apptest.data.api.RetrofitClient
import com.example.apptest.data.model.Movie

class MovieRepository {

    private val apiService = RetrofitClient.apiService

    companion object {
        private const val TAG = "MovieRepository"
    }

    suspend fun searchMovies(query: String): List<Movie>? {
        return try {
            Log.d(TAG, "Searching movies with query : $query")

            val response = apiService.searchMovies(query)

            if (response.isSuccessful) {
                val body = response.body()

                Log.d(TAG, "Success Found: ${body?.results?.size} movies")
                body?.results?.forEach { movie ->
                    Log.d(TAG, "- ${movie.title} (${movie.voteAverage}/10)")
                }
                body?.results
            } else {
                Log.e(TAG, "Error ${response.code()} - ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception ${e.message}", e)
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