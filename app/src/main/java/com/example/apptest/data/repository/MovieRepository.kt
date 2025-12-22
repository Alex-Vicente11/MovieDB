package com.example.apptest.data.repository

import android.util.Log
import com.example.apptest.data.api.RetrofitClient
import com.example.apptest.data.model.Movie
import com.example.apptest.data.network.NetworkResult
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher

class MovieRepository {

    private val apiService = RetrofitClient.apiService

    companion object {
        private const val TAG = "MovieRepository"
    }

    private val responseJson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    suspend fun searchMovies(query: String): NetworkResult<List<Movie>> {
        // withContext: Cambia el dispatcher (thread)
        // Dispatchers.IO: Thread pool optimizado para operaciones I/O
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, " Searching: '$query'")

                // Validación
                if (query.isBlank()) {
                    return@withContext NetworkResult.Error("La búsqueda no puede estar vacía")
                }

                val response = apiService.searchMovies(query)

                when {
                    response.isSuccessful -> {
                        val movies = response.body()?.results

                        movies?.let {
                            Log.d(TAG, "Found ${it.size} movies")
                            NetworkResult.Success(it)
                        } ?: NetworkResult.Error("No se encontraron películas")
                    }
                    response.code() == 401 -> {
                        NetworkResult.Error("Error de autenticación. Verifica tu API token")
                    }
                    response.code() == 404 -> {
                        NetworkResult.Error("Endpoint no encontrado")
                    }
                    else -> {
                        NetworkResult.Error("Error ${response.code()}: ${response.message()}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception", e)
                NetworkResult.Error(e.message ?: "Error desconocido")
            }
        }
    }

    suspend fun getPopularMovies(): NetworkResult<List<Movie>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting popular movies...")
                val response = apiService.getPopularMovies()

                if (response.isSuccessful) {
                    val movies = response.body()?.results
                    movies?.let {
                        NetworkResult.Success(it)
                    } ?: NetworkResult.Error("No se encontraron películas")
                } else {
                    NetworkResult.Error("Error ${response.code()}")
                }
            } catch (e: Exception) {
                NetworkResult.Error(e.message ?: "Error desconocido")
            }
        }
    }
}