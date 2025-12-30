package com.example.apptest.data.repository

import android.util.Log
import com.example.apptest.data.api.RetrofitClient
import com.example.apptest.data.model.Movie
import com.example.apptest.data.network.NetworkResult
import com.example.apptest.util.ViewState
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher


/**
 * MovieRepository: Capa de datos
 *
 * Responsabilidad ÚNICA:
 * - Obtener datos de la API
 * - Transformar respuestas en ViewState
 * - Manejar errores
 *
 * NO debe:
 * - Manejar UI
 * - Tener lógica de negocio compleja
 */

class MovieRepository {

    private val apiService = RetrofitClient.apiService

    companion object {
        private const val TAG = "MovieRepository"
    }

    /**
     * Buscar películas por query
     *
     * Flow<ViewState<List<Movie>>>:
     * - Flow: Flujo de datos asíncrono
     * - ViewState: Wrapper con estados (Loading, Success, Error)
     * - List<Movie>: Los datos en caso de éxito
     *
     * NO es suspend porque Flow ya es asíncrono
     */

    private val responseJson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    suspend fun searchMovies(query: String): Flow<ViewState<List<Movie>>> = flow {
        // Flow: lo que está dentro se ejecuta cuando alguien "colecta" el Flow
        try {
            emit(ViewState.Loading)

            Log.d(TAG, "Searching: '$query'")

            // Validacion de entrada
            if (query.isBlank()) {
                emit(ViewState.Error("La busqueda no puede estar vacia"))
                return@flow // Esto sale del flow { }
            }

            // llamada a la API (es la única funcion suspend)
            // suspend fun searchMovies(): Response<MovieResponse>
            val response = apiService.searchMovies(query)

            when {
                response.isSuccessful -> {
                    // response.body()?.results
                    val movies = response.body()?.results

                    if (movies.isNullOrEmpty()) {
                        emit(ViewState.Error("No se encontraron peliculas"))
                    } else {
                        Log.d(TAG, "Found ${movies.size} movies")
                        emit(ViewState.Success(movies))
                    }
                }

                // Error 401: No autorizado
                response.code() == 401 -> {
                    emit(ViewState.Error("Error de autenticacion. Verifica el token"))
                }

                response.code() == 404 -> {
                    emit(ViewState.Error("Endpoint no encontrado"))
                }

                // Otros errores
                else -> {
                    emit(ViewState.Error("Error ${response.code()}: ${response.message()}"))
                }
            }
        } catch (e: Exception) {
            // Capturar cualquier excepción (red, timeout, etc.)
            Log.e(TAG, "Exception", e)
            emit(ViewState.Error(e.message ?: "Error desconocido"))
        }
    }.flowOn(Dispatchers.IO)
    // flowOn(Dispatchers.IO):
    // - Ejecuta el flow en un thread de I/O
    // - No bloquea el thread principal (UI)
    // - Maneja el threading automáticamente

    fun getPopularMovies(): Flow<ViewState<List<Movie>>> = flow {
        try {
            emit(ViewState.Loading)

            Log.d(TAG, " Getting popular movies")

            val response = apiService.getPopularMovies()

            if (response.isSuccessful) {
                val movies = response.body()?.results

                if (movies.isNullOrEmpty()) {
                    emit(ViewState.Error("No hay peliculas disponibles"))
                } else {
                    Log.d(TAG, "Found ${movies.size} popular movies")
                    emit(ViewState.Success(movies))
                }
            } else {
                emit(ViewState.Error("Error ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception", e)
            emit(ViewState.Error(e.message ?: "Error desconocido"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Obtener detalles de una película por ID
     *
     * endpoint específico:
     * GET /movie/{id}
     *
     */

    fun getPopularMovies(movieId: Int): Flow<ViewState<Movie>> = flow {
        try {
            emit(ViewState.Loading)

            Log.d(TAG, " Getting movie details for ID: $movieId")

            val response = apiService.getMovieDetails(movieId)

            when {
                response.isSuccessful -> {
                    val movie = response.body()

                    if (movie != null) {
                        Log.d(TAG, "Movie found: ${movie.title}")
                        Log.d(TAG, "Runtime: ${movie.runtime} min")
                        Log.d(TAG, "Budget: $${movie.budget}")
                        Log.d(TAG, "Genres: ${movie.getGenresString()}")

                        emit(ViewState.Success(movie))
                    } else {
                        Log.w(TAG, "Response body is null")
                        emit(ViewState.Error("Pelicula no encontrada"))
                    }
                }

                response.code() == 404 -> {
                    Log.e(TAG, "Movie not found (404)")
                    emit(ViewState.Error("Película no encontrada (ID: $movieId)"))
                }

                response.code() == 401 -> {
                    Log.e(TAG, "Unauthorized (401)")
                    emit(ViewState.Error("Error de autenticación"))
                }

                else -> {
                    Log.e(TAG, "Error ${response.code()}: ${response.message()}")
                    emit(ViewState.Error("Error ${response.code()}"))
                }
            }
        }catch (e: Exception) {
            Log.e(TAG, "Exception getting movie details", e)
            emit(ViewState.Error(e.message ?: "Error desconocido"))
        }
    }.flowOn(Dispatchers.IO)


}